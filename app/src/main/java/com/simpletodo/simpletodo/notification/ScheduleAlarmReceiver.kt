package com.simpletodo.simpletodo.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simpletodo.simpletodo.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScheduleAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
        if (scheduleId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(context).scheduleDao()
                val schedule = dao.getScheduleById(scheduleId)
                // The schedule may have been completed or deleted between
                // being scheduled and the alarm firing — skip in that case.
                if (schedule != null && !schedule.isCompleted) {
                    NotificationHelper.show(context, schedule)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_SCHEDULE_ID = "com.simpletodo.simpletodo.notification.EXTRA_SCHEDULE_ID"
    }
}
