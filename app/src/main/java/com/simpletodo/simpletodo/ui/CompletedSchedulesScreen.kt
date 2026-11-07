package com.simpletodo.simpletodo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.simpletodo.simpletodo.ui.theme.ButtonShape
import com.simpletodo.simpletodo.ui.theme.mutedText

/**
 * Completed schedules, reached from the bottom of the 일정 list rather than
 * kept as a tab of their own: searchable by keyword and date range, with
 * un-completing (the checked box) and deleting per row.
 */
@Composable
fun CompletedSchedulesScreen(
    viewModel: ScheduleViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val schedules by viewModel.pastSchedules.collectAsState()
    val keyword by viewModel.searchKeyword.collectAsState()
    val startDate by viewModel.searchStartDate.collectAsState()
    val endDate by viewModel.searchEndDate.collectAsState()

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    val filtering = keyword.isNotBlank() || startDate != null || endDate != null

    BackHandler(onBack = onBack)

    Scaffold(
        modifier = modifier,
        topBar = { ScreenTopBar(title = "완료한 일정", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            Column(
                modifier = Modifier.padding(start = PageGutter, end = PageGutter, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = viewModel::updateSearchKeyword,
                    placeholder = { Text("검색", color = MaterialTheme.mutedText) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                    },
                    singleLine = true,
                    shape = ButtonShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        unfocusedLeadingIconColor = MaterialTheme.mutedText,
                        focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DateFilterChip(text = startDate?.toDisplayString() ?: "시작일", onClick = { showStartPicker = true })
                    DateFilterChip(text = endDate?.toDisplayString() ?: "종료일", onClick = { showEndPicker = true })
                    if (filtering) {
                        TextButton(onClick = viewModel::clearSearch) {
                            Text("초기화", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (schedules.isEmpty()) {
                EmptyState(
                    text = if (filtering) "검색 결과 없음" else "완료한 일정 없음",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)) {
                    itemsIndexed(schedules, key = { _, it -> it.id }) { index, schedule ->
                        PastScheduleRow(
                            schedule = schedule,
                            onRestore = viewModel::restoreSchedule,
                            onDelete = viewModel::deleteSchedule,
                        )
                        if (index < schedules.lastIndex) RowDivider()
                    }
                }
            }
        }
    }

    if (showStartPicker) {
        AppDatePickerDialog(
            initialDate = startDate,
            onDateSelected = { viewModel.updateSearchDateRange(it, endDate) },
            onDismiss = { showStartPicker = false },
        )
    }
    if (showEndPicker) {
        AppDatePickerDialog(
            initialDate = endDate,
            onDateSelected = { viewModel.updateSearchDateRange(startDate, it) },
            onDismiss = { showEndPicker = false },
        )
    }
}

@Composable
private fun DateFilterChip(text: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(text) },
        leadingIcon = {
            Icon(Icons.Outlined.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
        },
        colors = AssistChipDefaults.assistChipColors(leadingIconContentColor = MaterialTheme.mutedText),
        border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = MaterialTheme.colorScheme.outlineVariant),
    )
}
