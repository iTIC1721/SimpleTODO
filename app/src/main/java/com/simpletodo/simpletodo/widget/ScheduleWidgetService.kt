package com.simpletodo.simpletodo.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.simpletodo.simpletodo.R
import com.simpletodo.simpletodo.data.AppDatabase
import com.simpletodo.simpletodo.data.Schedule
import com.simpletodo.simpletodo.data.scheduleBucketLabel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class ScheduleWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val isDark = intent.getBooleanExtra(EXTRA_IS_DARK, false)
        return ScheduleRemoteViewsFactory(applicationContext, isDark)
    }

    companion object {
        const val EXTRA_IS_DARK = "com.simpletodo.simpletodo.widget.EXTRA_IS_DARK"
    }
}

private val widgetDateFormatter = DateTimeFormatter.ofPattern("M/d", Locale.KOREAN)
private val widgetTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.KOREAN)

private sealed interface WidgetRow {
    data class Header(val label: String) : WidgetRow
    data class Item(val schedule: Schedule) : WidgetRow
}

/**
 * Supplies rows for the widget's ListView on demand — this is what makes the
 * list scrollable and unbounded (no fixed item cap). Schedules are grouped
 * into date-bucket sections (지난 일정/오늘/내일/7일 이내/나중 할일), each
 * rendered as a small header row followed by its items.
 */
private class ScheduleRemoteViewsFactory(
    private val context: Context,
    private val isDark: Boolean,
) : RemoteViewsService.RemoteViewsFactory {

    private val colors = widgetColorsFor(isDark)
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
            is WidgetRow.Header -> RemoteViews(context.packageName, R.layout.schedule_widget_header_row).apply {
                setTextViewText(R.id.widget_header_label, row.label)
                setTextColor(R.id.widget_header_label, colors.headerText)
                setInt(R.id.widget_header_divider, "setBackgroundColor", colors.divider)
            }
            is WidgetRow.Item -> RemoteViews(context.packageName, R.layout.schedule_widget_item).apply {
                val title = if (row.schedule.isImportant) "★ ${row.schedule.title}" else row.schedule.title
                setTextViewText(R.id.widget_item_title, title)
                setTextColor(R.id.widget_item_title, if (row.schedule.isImportant) WIDGET_IMPORTANT_ACCENT else colors.text)
                setTextColor(R.id.widget_item_checkbox, colors.subText)
                val dateText = row.schedule.date.format(widgetDateFormatter)
                val timeText = row.schedule.time?.format(widgetTimeFormatter)
                setTextViewText(
                    R.id.widget_item_date,
                    if (timeText != null) "$dateText $timeText" else dateText,
                )
                setTextColor(R.id.widget_item_date, colors.subText)

                val openFillIntent = Intent().putExtra(
                    ScheduleWidgetActionReceiver.EXTRA_ACTION,
                    ScheduleWidgetActionReceiver.ACTION_OPEN,
                )
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
