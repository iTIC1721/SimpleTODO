package com.simpletodo.simpletodo.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.simpletodo.simpletodo.data.AlarmPreferences
import com.simpletodo.simpletodo.data.AppDatabase
import com.simpletodo.simpletodo.data.RecurringReminder
import com.simpletodo.simpletodo.data.daysOfWeek
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Weekly repeating reminders — entirely separate from the schedule system's
 * alarms (own data, own alarms, own notification channel). Each reminder
 * keeps exactly one alarm scheduled at a time, for its next matching
 * day/time; the receiver re-chains it for the following occurrence after
 * firing, the same self-rechaining pattern as the daily briefing.
 */
object RecurringReminderScheduler {

    fun schedule(context: Context, reminder: RecurringReminder) {
        if (!reminder.isEnabled) {
            cancel(context, reminder.id)
            return
        }
        val days = reminder.daysOfWeek()
        if (days.isEmpty()) {
            cancel(context, reminder.id)
            return
        }
        val time = reminder.time ?: AlarmPreferences(context).defaultNotifyTime
        val triggerDateTime = nextOccurrence(LocalDateTime.now(), days, time)
        val triggerMillis = triggerDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        setExact(alarmManager, triggerMillis, pendingIntentFor(context, reminder.id))
    }

    fun cancel(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntentFor(context, reminderId))
    }

    /** Re-schedules every enabled reminder — used on boot and when the default briefing time changes. */
    suspend fun rescheduleAll(context: Context) {
        val dao = AppDatabase.getInstance(context).recurringReminderDao()
        dao.getAllEnabled().forEach { schedule(context, it) }
    }

    internal fun nextOccurrence(from: LocalDateTime, days: Set<DayOfWeek>, time: LocalTime): LocalDateTime {
        for (offset in 0..7) {
            val candidateDate = from.toLocalDate().plusDays(offset.toLong())
            if (candidateDate.dayOfWeek in days) {
                val candidate = LocalDateTime.of(candidateDate, time)
                if (candidate.isAfter(from)) return candidate
            }
        }
        error("days must not be empty")
    }

    private fun setExact(alarmManager: AlarmManager, triggerMillis: Long, pendingIntent: PendingIntent) {
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        } catch (e: SecurityException) {
            // SCHEDULE_EXACT_ALARM not granted (Android 12+) — fall back to an
            // inexact alarm so reminders still fire, just without precise timing.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }

    private fun pendingIntentFor(context: Context, reminderId: Long): PendingIntent {
        val intent = Intent(context, RecurringReminderAlarmReceiver::class.java).apply {
            putExtra(RecurringReminderAlarmReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
