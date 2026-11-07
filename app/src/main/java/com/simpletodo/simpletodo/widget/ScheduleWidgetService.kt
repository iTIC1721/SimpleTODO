package com.simpletodo.simpletodo.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.simpletodo.simpletodo.R
import com.simpletodo.simpletodo.data.AppDatabase
import com.simpletodo.simpletodo.data.Schedule
import com.simpletodo.simpletodo.data.ThemeMode
import com.simpletodo.simpletodo.data.scheduleBucketLabel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import android.view.View
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ScheduleWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val themeMode = runCatching { ThemeMode.valueOf(intent.getStringExtra(EXTRA_THEME_MODE)!!) }
            .getOrDefault(ThemeMode.SYSTEM)
        return ScheduleRemoteViewsFactory(applicationContext, forcedWidgetColors(themeMode))
    }

    companion object {
        const val EXTRA_THEME_MODE = "com.simpletodo.simpletodo.widget.EXTRA_THEME_MODE"
    }
}

private val widgetDateFormatter = DateTimeFormatter.ofPattern("M/d (E)", Locale.KOREAN)
private val widgetTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.KOREAN)

private sealed interface WidgetRow {
    data class Header(val label: String) : WidgetRow
    data class Item(val schedule: Schedule) : WidgetRow
}

/**
 * Supplies rows for the widget's ListView on demand — this is what makes the
 * list scrollable and unbounded (no fixed item cap). Schedules are grouped
 * into date-bucket sections (기한 지남/오늘/내일/7일 이내/나중에), each
 * rendered as a small header row followed by its items.
 */
private class ScheduleRemoteViewsFactory(
    private val context: Context,
    /** Null under "시스템 기본": the layouts' day/night resources apply. */
    private val colors: WidgetColors?,
) : RemoteViewsService.RemoteViewsFactory {

    // Forced themes use alias layout ids; see res/values/widget_layouts.xml.
    private val headerLayout =
        if (colors == null) R.layout.schedule_widget_header_row else R.layout.schedule_widget_header_row_forced
    private val itemLayout = if (colors == null) R.layout.schedule_widget_item else R.layout.schedule_widget_item_forced

    private var rows: List<WidgetRow> = emptyList()

    override fun onCreate() = Unit

    // Called on a background thread by the widget host, so a blocking DB read
    // here is the documented, expected pattern for RemoteViewsFactory.
    override fun onDataSetChanged() {
        val dao = AppDatabase.getInstance(context).scheduleDao()
        val schedules = runBlocking { dao.getCurrentSchedules().first() }
        val today = LocalDate.now()

        val flattened = mutableListOf<WidgetRow>()
        var currentBucket: String? = null
        for (schedule in schedules) {
            val bucket = scheduleBucketLabel(schedule.date, today)
            if (bucket != currentBucket) {
                flattened += WidgetRow.Header(bucket)
                currentBucket = bucket
            }
            flattened += WidgetRow.Item(schedule)
        }
        rows = flattened
    }

    override fun onDestroy() {
        rows = emptyList()
    }

    override fun getCount(): Int = rows.size

    override fun getViewAt(position: Int): RemoteViews {
        return when (val row = rows[position]) {
            is WidgetRow.Header -> RemoteViews(context.packageName, headerLayout).apply {
                val overdue = row.label == OVERDUE_BUCKET
                showOneOf(R.id.widget_header_label, R.id.widget_header_label_overdue, useSecond = overdue)
                    .let { label ->
                        setTextViewText(label, row.label)
                        colors?.let { setTextColor(label, if (overdue) it.error else it.muted) }
                    }
            }
            is WidgetRow.Item -> RemoteViews(context.packageName, itemLayout).apply {
                val schedule = row.schedule
                val title = showOneOf(R.id.widget_item_title, R.id.widget_item_title_important, schedule.isImportant)
                setTextViewText(title, schedule.title)
                setViewVisibility(R.id.widget_item_star, if (schedule.isImportant) View.VISIBLE else View.GONE)

                val overdue = schedule.isOverdue()
                val date = showOneOf(R.id.widget_item_date, R.id.widget_item_date_overdue, overdue)
                val dateText = schedule.date.format(widgetDateFormatter)
                val timeText = schedule.time?.format(widgetTimeFormatter)
                setTextViewText(date, if (timeText != null) "$dateText · $timeText" else dateText)

                colors?.let {
                    setTextColor(title, if (schedule.isImportant) it.accent else it.text)
                    setTextColor(date, if (overdue) it.error else it.muted)
                    setInt(R.id.widget_item_checkbox, "setColorFilter", it.muted)
                    setInt(R.id.widget_item_star, "setColorFilter", it.accent)
                }

                val openFillIntent = Intent()
                    .putExtra(ScheduleWidgetActionReceiver.EXTRA_ACTION, ScheduleWidgetActionReceiver.ACTION_OPEN)
                    .putExtra(ScheduleWidgetActionReceiver.EXTRA_SCHEDULE_ID, schedule.id)
                setOnClickFillInIntent(R.id.widget_item_root, openFillIntent)

                val completeFillIntent = Intent()
                    .putExtra(ScheduleWidgetActionReceiver.EXTRA_ACTION, ScheduleWidgetActionReceiver.ACTION_COMPLETE)
                    .putExtra(ScheduleWidgetActionReceiver.EXTRA_SCHEDULE_ID, row.schedule.id)
                setOnClickFillInIntent(R.id.widget_item_checkbox, completeFillIntent)
            }
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 2

    override fun getItemId(position: Int): Long = when (val row = rows[position]) {
        is WidgetRow.Header -> row.label.hashCode().toLong()
        is WidgetRow.Item -> row.schedule.id
    }

    override fun hasStableIds(): Boolean = true
}

private const val OVERDUE_BUCKET = "기한 지남"

/** Shows one of two alternately-colored twin views and returns its id. */
private fun RemoteViews.showOneOf(first: Int, second: Int, useSecond: Boolean): Int {
    setViewVisibility(first, if (useSecond) View.GONE else View.VISIBLE)
    setViewVisibility(second, if (useSecond) View.VISIBLE else View.GONE)
    return if (useSecond) second else first
}

private fun Schedule.isOverdue(): Boolean {
    val today = LocalDate.now()
    return date.isBefore(today) || (date.isEqual(today) && time != null && time.isBefore(LocalTime.now()))
}
