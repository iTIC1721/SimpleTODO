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

fun Set<DayOfWeek>.toDisplayString(): String =
    DayOfWeek.entries.filter { it in this }.joinToString(",") { koreanDayOfWeekLabels.getValue(it) }
