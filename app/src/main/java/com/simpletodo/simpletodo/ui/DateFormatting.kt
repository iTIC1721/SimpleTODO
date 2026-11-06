package com.simpletodo.simpletodo.ui

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.KOREAN)
private val completedAtFormatter = DateTimeFormatter.ofPattern("M월 d일 HH:mm", Locale.KOREAN)
private val headerDateFormatter = DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN)
private val headerWeekdayFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.KOREAN)

val koreanDayOfWeekLabels: Map<DayOfWeek, String> = mapOf(
    DayOfWeek.MONDAY to "월",
    DayOfWeek.TUESDAY to "화",
    DayOfWeek.WEDNESDAY to "수",
    DayOfWeek.THURSDAY to "목",
    DayOfWeek.FRIDAY to "금",
    DayOfWeek.SATURDAY to "토",
    DayOfWeek.SUNDAY to "일",
)

fun LocalDate.toDisplayString(): String = format(dateFormatter)

fun LocalTime.toDisplayString(): String = format(timeFormatter)

fun Instant.toDisplayString(): String =
    completedAtFormatter.format(atZone(ZoneId.systemDefault()))

/** "11월 6일" — the large date at the top of the list. */
fun LocalDate.toHeaderDateString(): String = format(headerDateFormatter)

/** "금요일" */
fun LocalDate.toWeekdayString(): String = format(headerWeekdayFormatter)

private val weekdays = setOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
)
private val weekend = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

/** "매일", "평일", "주말", or the days spelled out like "월, 수". */
fun Set<DayOfWeek>.toDisplayString(): String = when (this) {
    DayOfWeek.entries.toSet() -> "매일"
    weekdays -> "평일"
    weekend -> "주말"
    else -> DayOfWeek.entries.filter { it in this }.joinToString(", ") { koreanDayOfWeekLabels.getValue(it) }
}

/** "09:20 완료" when finished on the schedule's own day, otherwise "11월 5일 09:20 완료". */
fun completedText(scheduleDate: LocalDate, completedAt: Instant): String {
    val local = completedAt.atZone(ZoneId.systemDefault())
    return if (local.toLocalDate() == scheduleDate) {
        "${local.toLocalTime().toDisplayString()} 완료"
    } else {
        "${completedAtFormatter.format(local)} 완료"
    }
}
