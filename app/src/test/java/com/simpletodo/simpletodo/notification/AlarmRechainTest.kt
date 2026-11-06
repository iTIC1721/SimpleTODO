package com.simpletodo.simpletodo.notification

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * An alarm delivered slightly before its trigger time must re-chain to the
 * *next* occurrence, not to the one that is currently firing — otherwise the
 * same notification is posted twice in a row (one notification, two buzzes).
 */
class AlarmRechainTest {

    private val everyDay = DayOfWeek.entries.toSet()
    private val seven = LocalTime.of(7, 0)
    private val firing = LocalDateTime.of(2026, 11, 9, 7, 0) // Monday 07:00
    private val firingMillis = firing.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun earlyDelivery_usingNowAlone_picksTheSameOccurrence() {
        // Documents the original bug: 200 ms early, "now" still precedes 07:00.
        val now = firing.minusNanos(200_000_000)
        assertEquals(firing, RecurringReminderScheduler.nextOccurrence(now, everyDay, seven))
    }

    @Test
    fun earlyDelivery_recurringReminder_rechainsToNextDay() {
        val base = RecurringReminderScheduler.rechainBase(firingMillis, nowMillis = firingMillis - 200)
        assertEquals(firing.plusDays(1), RecurringReminderScheduler.nextOccurrence(base, everyDay, seven))
    }

    @Test
    fun earlyDelivery_weekdaysOnly_fridayRechainsToMonday() {
        val friday = LocalDateTime.of(2026, 11, 13, 7, 0)
        val fridayMillis = friday.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        val base = RecurringReminderScheduler.rechainBase(fridayMillis, nowMillis = fridayMillis - 200)
        assertEquals(friday.plusDays(3), RecurringReminderScheduler.nextOccurrence(base, weekdays, seven))
    }

    @Test
    fun lateDelivery_rechainsFromNow() {
        val base = RecurringReminderScheduler.rechainBase(firingMillis, nowMillis = firingMillis + 5 * 60_000)
        assertEquals(firing.plusDays(1), RecurringReminderScheduler.nextOccurrence(base, everyDay, seven))
    }

    @Test
    fun missingTriggerExtra_fallsBackToNow() {
        // Alarms scheduled by the previous app version carry no trigger time.
        val nowMillis = firingMillis + 1_000
        val base = RecurringReminderScheduler.rechainBase(-1L, nowMillis = nowMillis)
        assertEquals(firing.plusDays(1), RecurringReminderScheduler.nextOccurrence(base, everyDay, seven))
    }

    @Test
    fun earlyDelivery_dailyBriefing_rechainsToNextDay() {
        val base = RecurringReminderScheduler.rechainBase(firingMillis, nowMillis = firingMillis - 200)
        assertEquals(firing.plusDays(1), AlarmScheduler.nextBriefing(base, seven))
    }
}
