package com.simpletodo.simpletodo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.simpletodo.simpletodo.data.Schedule
import com.simpletodo.simpletodo.data.scheduleBucketLabel
import java.time.LocalDate

@Composable
fun CurrentScheduleTab(
    schedules: List<Schedule>,
    onComplete: (Schedule) -> Unit,
    onEdit: (Schedule) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (schedules.isEmpty()) {
        EmptyState(text = "등록된 일정이 없습니다", modifier = modifier.fillMaxSize())
    } else {
        val today = LocalDate.now()
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            var currentBucket: String? = null
            for (schedule in schedules) {
                val bucket = scheduleBucketLabel(schedule.date, today)
                if (bucket != currentBucket) {
                    item(key = "header_$bucket") {
                        ScheduleBucketHeader(label = bucket)
                    }
                    currentBucket = bucket
                }
                item(key = schedule.id) {
                    CurrentScheduleCard(schedule = schedule, onComplete = onComplete, onEdit = onEdit)
                }
            }
        }
    }
}

@Composable
private fun ScheduleBucketHeader(label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(text = text)
    }
}
