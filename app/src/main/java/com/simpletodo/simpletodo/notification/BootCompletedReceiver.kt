package com.simpletodo.simpletodo.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simpletodo.simpletodo.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Re-registers alarms after reboot, since AlarmManager entries don't survive a restart. */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(context).scheduleDao()
                val schedules = dao.getCurrentSchedules().first()
                schedules.forEach { AlarmScheduler.schedule(context, it) }
                AlarmScheduler.scheduleDailyBriefing(context)
                RecurringReminderScheduler.rescheduleAll(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
