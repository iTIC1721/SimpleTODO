package com.simpletodo.simpletodo.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.simpletodo.simpletodo.data.AlarmPreferences
import com.simpletodo.simpletodo.data.Schedule
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Two separate reminder mechanisms (기획서 3.3, revised): every day at a
 * configurable time, a single briefing notification summarizes all of that
 * day's schedules (date-only and time-specific alike). Schedules that also
 * carry a specific time additionally get their own individual reminder a
 * configurable number of minutes beforehand.
 */
object AlarmScheduler {

    private const val BRIEFING_REQUEST_CODE = -1

    /** Only time-specific schedules get their own alarm — date-only ones are covered by the daily briefing. */
    fun schedule(context: Context, schedule: Schedule) {
        val time = schedule.time ?: return
        val preferences = AlarmPreferences(context)
        val triggerDateTime = LocalDateTime.of(schedule.date, time).minusMinutes(preferences.leadMinutes.toLong())
        val triggerMillis = triggerDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (triggerMillis <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = pendingIntentFor(context, schedule.id)
        setExact(alarmManager, triggerMillis, pendingIntent)
    }

    fun cancel(context: Context, scheduleId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntentFor(context, scheduleId))
    }

    /**
     * Schedules (or re-chains) the next briefing strictly after [after]. The
     * receiver passes the occurrence that just fired, so an alarm delivered
     * slightly early can't re-schedule itself for the same day.
     */
    fun scheduleDailyBriefing(context: Context, after: LocalDateTime = LocalDateTime.now()) {
        val preferences = AlarmPreferences(context)
        val triggerDateTime = nextBriefing(after, preferences.defaultNotifyTime)
        val triggerMillis = triggerDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyBriefingReceiver::class.java).apply {
            putExtra(RecurringReminderScheduler.EXTRA_TRIGGER_AT_MILLIS, triggerMillis)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            BRIEFING_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        setExact(alarmManager, triggerMillis, pendingIntent)
    }

    internal fun nextBriefing(after: LocalDateTime, time: LocalTime): LocalDateTime {
        val candidate = LocalDateTime.of(after.toLocalDate(), time)
        return if (candidate.isAfter(after)) candidate else candidate.plusDays(1)
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

    private fun pendingIntentFor(context: Context, scheduleId: Long): PendingIntent {
        val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            putExtra(ScheduleAlarmReceiver.EXTRA_SCHEDULE_ID, scheduleId)
        }
        return PendingIntent.getBroadcast(
            context,
            scheduleId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
