package com.simpletodo.simpletodo.notification

import android.content.Context
import com.simpletodo.simpletodo.data.AlarmPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalTime

/**
 * Applies the notification settings the moment they change: stores the value
 * and re-registers every alarm that depends on it. The settings screen has no
 * save button, so nothing here waits for one.
 *
 * Rescheduling runs on an app-lifetime scope — the lead time is applied as the
 * settings screen closes, which would cancel a screen-scoped coroutine.
 */
object AlarmSettings {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val rescheduleLock = Mutex()

    /** Moves the daily briefing, and the recurring reminders that have no time of their own. */
    fun setBriefingTime(context: Context, time: LocalTime) {
        val app = context.applicationContext
        val preferences = AlarmPreferences(app)
        if (preferences.defaultNotifyTime == time) return
        preferences.defaultNotifyHour = time.hour
        preferences.defaultNotifyMinute = time.minute
        AlarmScheduler.scheduleDailyBriefing(app)
        scope.launch { rescheduleLock.withLock { RecurringReminderScheduler.rescheduleAll(app) } }
    }

    /** Changes how early time-specific schedules alert, for existing schedules as well as new ones. */
    fun setLeadMinutes(context: Context, minutes: Int) {
        val app = context.applicationContext
        val preferences = AlarmPreferences(app)
        if (preferences.leadMinutes == minutes) return
        preferences.leadMinutes = minutes
        scope.launch { rescheduleLock.withLock { AlarmScheduler.rescheduleAll(app) } }
    }
}
