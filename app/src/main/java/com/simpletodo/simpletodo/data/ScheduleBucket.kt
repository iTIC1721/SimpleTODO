package com.simpletodo.simpletodo.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Shared by the app's current-schedule list and the widget, so both group into the same date buckets. */
fun scheduleBucketLabel(date: LocalDate, today: LocalDate): String {
    val daysUntil = ChronoUnit.DAYS.between(today, date)
    return when {
        daysUntil < 0 -> "기한 지남"
        daysUntil == 0L -> "오늘"
        daysUntil == 1L -> "내일"
        daysUntil in 2..7 -> "7일 이내"
        else -> "나중에"
    }
}
