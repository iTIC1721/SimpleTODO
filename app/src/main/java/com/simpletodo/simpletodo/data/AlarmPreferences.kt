package com.simpletodo.simpletodo.data

import android.content.Context
import androidx.core.content.edit
import java.time.LocalTime

/**
 * User-configurable defaults for schedule reminders (기획서 3.3):
 * date-only schedules notify at a fixed default time, date+time schedules
 * notify a configurable number of minutes beforehand.
 */
class AlarmPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var defaultNotifyHour: Int
        get() = prefs.getInt(KEY_HOUR, DEFAULT_HOUR)
        set(value) = prefs.edit { putInt(KEY_HOUR, value) }

    var defaultNotifyMinute: Int
        get() = prefs.getInt(KEY_MINUTE, 0)
        set(value) = prefs.edit { putInt(KEY_MINUTE, value) }

    var leadMinutes: Int
        get() = prefs.getInt(KEY_LEAD_MINUTES, DEFAULT_LEAD_MINUTES)
        set(value) = prefs.edit { putInt(KEY_LEAD_MINUTES, value) }

    /** Off ("알림 안 함"): time-specific schedules get no alert of their own, only the briefing. */
    var scheduleAlertEnabled: Boolean
        get() = prefs.getBoolean(KEY_SCHEDULE_ALERT_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_SCHEDULE_ALERT_ENABLED, value) }

    val defaultNotifyTime: LocalTime
        get() = LocalTime.of(defaultNotifyHour, defaultNotifyMinute)

    companion object {
        private const val PREFS_NAME = "alarm_settings"
        private const val KEY_HOUR = "default_notify_hour"
        private const val KEY_MINUTE = "default_notify_minute"
        private const val KEY_LEAD_MINUTES = "lead_minutes"
        private const val KEY_SCHEDULE_ALERT_ENABLED = "schedule_alert_enabled"
        const val DEFAULT_HOUR = 8
        const val DEFAULT_LEAD_MINUTES = 15
    }
}
