package com.simpletodo.simpletodo.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.simpletodo.simpletodo.data.Schedule
import com.simpletodo.simpletodo.data.scheduleBucketLabel
import java.time.LocalDate

/** Bottom padding so the last row clears the floating add button. */
internal val ListBottomPadding = 96.dp

@Composable
fun CurrentScheduleTab(
    schedules: List<Schedule>,
    onComplete: (Schedule) -> Unit,
    onEdit: (Schedule) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (schedules.isEmpty()) {
        EmptyState(text = "일정 없음", modifier = modifier.fillMaxSize())
        return
    }
    val today = LocalDate.now()
    val groups = schedules.groupBy { scheduleBucketLabel(it.date, today) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = ListBottomPadding),
    ) {
        groups.forEach { (bucket, items) ->
            item(key = "header_$bucket") {
                SectionHeader(
                    label = bucket,
                    color = if (bucket == OVERDUE_BUCKET) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                )
            }
            items.forEachIndexed { index, schedule ->
                item(key = schedule.id) {
                    CurrentScheduleRow(schedule = schedule, onComplete = onComplete, onEdit = onEdit)
                    if (index < items.lastIndex) RowDivider()
                }
            }
        }
    }
}

private const val OVERDUE_BUCKET = "기한 지남"
