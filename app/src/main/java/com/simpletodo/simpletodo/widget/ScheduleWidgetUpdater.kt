package com.simpletodo.simpletodo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.simpletodo.simpletodo.MainActivity
import com.simpletodo.simpletodo.R

/**
 * Builds RemoteViews and pushes them straight through AppWidgetManager — no
 * Glance/WorkManager session in the middle, so this reflects on the home
 * screen essentially immediately after being called. The schedule list itself
 * is supplied by ScheduleWidgetService (a RemoteViewsFactory), which is what
 * lets the list scroll and show every schedule instead of a fixed row cap.
 */
object ScheduleWidgetUpdater {

    suspend fun updateAll(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, ScheduleWidgetProvider::class.java))
        for (id in ids) {
            refresh(context, appWidgetManager, id)
        }
    }

    suspend fun update(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        refresh(context, appWidgetManager, appWidgetId)
    }

    private fun refresh(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        appWidgetManager.updateAppWidget(appWidgetId, buildRemoteViews(context, appWidgetId))
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list_view)
    }

    private fun buildRemoteViews(context: Context, appWidgetId: Int): RemoteViews {
        val isDark = resolveWidgetIsDark(context)
        val colors = widgetColorsFor(isDark)
        val views = RemoteViews(context.packageName, R.layout.schedule_widget)

        views.setInt(
            R.id.widget_root,
            "setBackgroundResource",
            if (isDark) R.drawable.widget_background_dark else R.drawable.widget_background_light,
        )
        views.setTextColor(R.id.widget_title_label, colors.text)
        views.setTextColor(R.id.widget_add_button, colors.text)
        views.setTextColor(R.id.widget_empty_text, colors.subText)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_root, openPendingIntent)

        // List items need a shared template + per-view fill-in intents so the
        // checkbox and the rest of the row can trigger different actions; that
        // only works with a mutable broadcast template, not an Activity intent.
        val actionIntent = Intent(context, ScheduleWidgetActionReceiver::class.java)
        val actionPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        views.setPendingIntentTemplate(R.id.widget_list_view, actionPendingIntent)

        val addIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_ADD, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val addPendingIntent = PendingIntent.getActivity(
            context,
            0,
            addIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_add_button, addPendingIntent)

        val serviceIntent = Intent(context, ScheduleWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(ScheduleWidgetService.EXTRA_IS_DARK, isDark)
            // RemoteViewsService caches its factory by this Intent's identity, which
            // is compared by data Uri — not by extras. Folding isDark into the Uri
            // forces a fresh factory (and thus fresh colors) whenever theme changes,
            // instead of silently reusing a factory built under the old theme.
            data = Uri.parse("widget://com.simpletodo.simpletodo/$appWidgetId?dark=$isDark")
        }
        views.setRemoteAdapter(R.id.widget_list_view, serviceIntent)
        views.setEmptyView(R.id.widget_list_view, R.id.widget_empty_text)

        return views
    }
}
