package com.simpletodo.simpletodo.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.simpletodo.simpletodo.data.AppDatabase
import com.simpletodo.simpletodo.widget.ScheduleWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

/** The 완료 button on a schedule's own notification: completes it without opening the app. */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_COMPLETE) return
        val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
        if (scheduleId == -1L) return

        NotificationManagerCompat.from(context).cancel(scheduleId.toInt())
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppDatabase.getInstance(context).scheduleDao().markCompleted(scheduleId, Instant.now())
                AlarmScheduler.cancel(context, scheduleId)
                ScheduleWidgetUpdater.updateAll(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE = "com.simpletodo.simpletodo.notification.ACTION_COMPLETE"
        const val EXTRA_SCHEDULE_ID = "com.simpletodo.simpletodo.notification.EXTRA_COMPLETE_SCHEDULE_ID"
    }
}
