package com.simpletodo.simpletodo.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simpletodo.simpletodo.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fires one recurring reminder's notification, then re-chains its alarm for the next matching day. */
class RecurringReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(context).recurringReminderDao()
                val reminder = dao.getById(reminderId) ?: return@launch
                if (reminder.isEnabled) {
                    NotificationHelper.showRecurringReminder(context, reminder)
                }
                RecurringReminderScheduler.schedule(context, reminder)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_REMINDER_ID = "com.simpletodo.simpletodo.notification.EXTRA_REMINDER_ID"
    }
}
