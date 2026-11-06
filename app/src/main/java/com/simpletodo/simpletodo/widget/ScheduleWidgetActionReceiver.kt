package com.simpletodo.simpletodo.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simpletodo.simpletodo.MainActivity
import com.simpletodo.simpletodo.data.AppDatabase
import com.simpletodo.simpletodo.notification.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Handles taps on the widget's list items. ListView collection items can only
 * share one PendingIntent template, so a broadcast (rather than launching the
 * Activity directly) lets each row's fill-in intent pick a different action —
 * the checkbox marks the schedule complete in place, the rest of the row
 * opens that schedule's edit screen.
 */
class ScheduleWidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getStringExtra(EXTRA_ACTION)) {
            ACTION_COMPLETE -> {
                val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
                if (scheduleId == -1L) return
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val dao = AppDatabase.getInstance(context).scheduleDao()
                        dao.markCompleted(scheduleId, Instant.now())
                        AlarmScheduler.cancel(context, scheduleId)
                        ScheduleWidgetUpdater.updateAll(context)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_OPEN -> {
                val openIntent = Intent(context, MainActivity::class.java).apply {
                    val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
                    if (scheduleId != -1L) putExtra(MainActivity.EXTRA_EDIT_SCHEDULE_ID, scheduleId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                context.startActivity(openIntent)
            }
        }
    }

    companion object {
        const val EXTRA_ACTION = "com.simpletodo.simpletodo.widget.EXTRA_ACTION"
        const val EXTRA_SCHEDULE_ID = "com.simpletodo.simpletodo.widget.EXTRA_SCHEDULE_ID"
        const val ACTION_COMPLETE = "complete"
        const val ACTION_OPEN = "open"
    }
}
