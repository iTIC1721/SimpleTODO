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
import java.time.Instant
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

    /**
     * Schedules the reminder's next occurrence strictly after [after]. When
     * re-chaining from the receiver, [after] must be the occurrence that just
     * fired (see [rechainBase]), not merely "now" — otherwise an alarm
     * delivered a few milliseconds early re-schedules itself for the same
     * minute and the notification alerts twice.
     */
    fun schedule(context: Context, reminder: RecurringReminder, after: LocalDateTime = LocalDateTime.now()) {
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
        val triggerDateTime = nextOccurrence(after, days, time)
        val triggerMillis = triggerDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        setExact(alarmManager, triggerMillis, pendingIntentFor(context, reminder.id, triggerMillis))
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

    /**
     * The point to re-chain an alarm from: the later of now and the time the
     * alarm was scheduled for. AlarmManager can deliver an RTC alarm slightly
     * before its trigger time, and using "now" alone would then pick the very
     * occurrence that is firing.
     */
    internal fun rechainBase(scheduledTriggerMillis: Long, nowMillis: Long = System.currentTimeMillis()): LocalDateTime {
        val baseMillis = maxOf(scheduledTriggerMillis, nowMillis)
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(baseMillis), ZoneId.systemDefault())
    }

    const val EXTRA_TRIGGER_AT_MILLIS = "com.simpletodo.simpletodo.notification.EXTRA_TRIGGER_AT_MILLIS"

    private fun setExact(alarmManager: AlarmManager, triggerMillis: Long, pendingIntent: PendingIntent) {
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        } catch (e: SecurityException) {
            // SCHEDULE_EXACT_ALARM not granted (Android 12+) — fall back to an
            // inexact alarm so reminders still fire, just without precise timing.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }

    // Extras don't take part in PendingIntent matching, so cancel() (which
    // passes no trigger time) still finds the alarm scheduled with one.
    private fun pendingIntentFor(context: Context, reminderId: Long, triggerMillis: Long = -1L): PendingIntent {
        val intent = Intent(context, RecurringReminderAlarmReceiver::class.java).apply {
            putExtra(RecurringReminderAlarmReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_TRIGGER_AT_MILLIS, triggerMillis)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
