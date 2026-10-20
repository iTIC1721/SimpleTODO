package com.simpletodo.simpletodo.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

/**
 * A weekly repeating reminder — entirely separate from [Schedule]. Doesn't
 * appear in the widget or the daily briefing; it has its own alarms and its
 * own notification channel (see notification/RecurringReminderScheduler.kt).
 */
@Entity(tableName = "recurring_reminder")
data class RecurringReminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val daysOfWeekMask: Int,
    /** Null means "use the daily briefing time from settings" instead of a fixed time. */
    val time: LocalTime? = null,
    val isEnabled: Boolean = true,
    val createdAt: Instant = Instant.now(),
)

fun RecurringReminder.daysOfWeek(): Set<DayOfWeek> = maskToDaysOfWeek(daysOfWeekMask)

fun daysOfWeekToMask(days: Set<DayOfWeek>): Int =
    days.fold(0) { acc, day -> acc or (1 shl (day.value - 1)) }

fun maskToDaysOfWeek(mask: Int): Set<DayOfWeek> =
    DayOfWeek.entries.filterTo(mutableSetOf()) { mask and (1 shl (it.value - 1)) != 0 }
