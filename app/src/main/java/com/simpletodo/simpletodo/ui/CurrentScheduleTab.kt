package com.simpletodo.simpletodo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.simpletodo.simpletodo.data.Schedule
import com.simpletodo.simpletodo.data.scheduleBucketLabel
import com.simpletodo.simpletodo.ui.theme.mutedText
import java.time.LocalDate

/** Bottom padding so the last row clears the floating add button. */
internal val ListBottomPadding = 96.dp

@Composable
fun CurrentScheduleTab(
    schedules: List<Schedule>,
    completedCount: Int,
    onComplete: (Schedule) -> Unit,
    onEdit: (Schedule) -> Unit,
    onOpenCompleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (schedules.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("일정 없음", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.mutedText)
            if (completedCount > 0) {
                CompletedLink(
                    count = completedCount,
                    onClick = onOpenCompleted,
                    fillWidth = false,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                )
            }
        }
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
        if (completedCount > 0) {
            item(key = "completed_link") {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = PageGutter),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    CompletedLink(
                        count = completedCount,
                        onClick = onOpenCompleted,
                        fillWidth = true,
                        contentPadding = PaddingValues(start = PageGutter, end = PageGutter - 4.dp),
                    )
                }
            }
        }
    }
}

/** "완료한 일정 · 12건 ›" — the way into the completed schedules screen. */
@Composable
private fun CompletedLink(
    count: Int,
    onClick: () -> Unit,
    fillWidth: Boolean,
    contentPadding: PaddingValues,
) {
    Row(
        modifier = (if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = 52.dp)
            .clickable(onClick = onClick)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "완료한 일정",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            // Full-width footer: count and chevron sit at the right edge.
            // Empty-state link: everything stays together in the center.
            modifier = if (fillWidth) Modifier.weight(1f) else Modifier,
        )
        Text(
            "${count}건",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.mutedText,
            modifier = Modifier.padding(start = 8.dp),
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.mutedText,
            modifier = Modifier
                .padding(start = 2.dp)
                .size(20.dp),
        )
    }
}

private const val OVERDUE_BUCKET = "기한 지남"
