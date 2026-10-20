package com.simpletodo.simpletodo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.simpletodo.simpletodo.data.Schedule
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastScheduleTab(
    schedules: List<Schedule>,
    keyword: String,
    startDate: LocalDate?,
    endDate: LocalDate?,
    onKeywordChange: (String) -> Unit,
    onDateRangeChange: (LocalDate?, LocalDate?) -> Unit,
    onClearSearch: () -> Unit,
    onRestore: (Schedule) -> Unit,
    onDelete: (Schedule) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = keyword,
            onValueChange = onKeywordChange,
            label = { Text("일정 내용 검색") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AssistChip(
                onClick = { showStartPicker = true },
                label = { Text(startDate?.toDisplayString() ?: "시작일") },
            )
            AssistChip(
                onClick = { showEndPicker = true },
                label = { Text(endDate?.toDisplayString() ?: "종료일") },
            )
            if (keyword.isNotBlank() || startDate != null || endDate != null) {
                TextButton(onClick = onClearSearch) { Text("초기화") }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (schedules.isEmpty()) {
            EmptyState(text = "완료한 일정이 없습니다", modifier = Modifier.fillMaxSize())
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(schedules, key = { it.id }) { schedule ->
                    PastScheduleCard(schedule = schedule, onRestore = onRestore, onDelete = onDelete)
                }
            }
        }
    }

    if (showStartPicker) {
        AppDatePickerDialog(
            initialDate = startDate,
            onDateSelected = { onDateRangeChange(it, endDate) },
            onDismiss = { showStartPicker = false },
        )
    }
    if (showEndPicker) {
        AppDatePickerDialog(
            initialDate = endDate,
            onDateSelected = { onDateRangeChange(startDate, it) },
            onDismiss = { showEndPicker = false },
        )
    }
}
