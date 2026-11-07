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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.simpletodo.simpletodo.data.Schedule
import com.simpletodo.simpletodo.ui.theme.ButtonShape
import com.simpletodo.simpletodo.ui.theme.mutedText
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Completed schedules, reached from the bottom of the 일정 list rather than
 * kept as a tab of their own: searchable by keyword and schedule date, with
 * un-completing (the checked box) and deleting per row, or several at once
 * in selection mode (선택 in the top bar, or a long press on a row).
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

    var selecting by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<Long>()) }
    var showDeleteSelectedConfirm by remember { mutableStateOf(false) }
    // Only rows still shown count: a search typed mid-selection hides the rest.
    val selected = schedules.filter { it.id in selectedIds }
    val allSelected = schedules.isNotEmpty() && selected.size == schedules.size
    fun exitSelection() {
        selecting = false
        selectedIds = emptySet()
    }
    LaunchedEffect(schedules.isEmpty()) {
        if (schedules.isEmpty()) exitSelection()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun delete(targets: List<Schedule>) {
        viewModel.deleteSchedules(targets)
        val message = if (targets.size == 1) "일정이 삭제되었습니다." else "일정 ${targets.size}건이 삭제되었습니다."
        scope.launch {
            if (snackbarHostState.showUndo(message)) viewModel.undoDelete(targets)
        }
    }
    fun restore(schedule: Schedule) {
        viewModel.restoreSchedule(schedule)
        scope.launch {
            if (snackbarHostState.showUndo("완료가 취소되었습니다.")) {
                viewModel.completeSchedule(schedule, schedule.completedAt ?: Instant.now())
            }
        }
    }

    BackHandler(onBack = onBack)
    // Registered after the one above, so while selecting, back leaves selection first.
    BackHandler(enabled = selecting) { exitSelection() }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (selecting) {
                ScreenTopBar(
                    title = if (selected.isEmpty()) "항목 선택" else "${selected.size}건 선택",
                    onBack = ::exitSelection,
                    backLabel = "선택 종료",
                    navigationIcon = Icons.Outlined.Close,
                    actions = {
                        TextButton(
                            onClick = {
                                selectedIds = if (allSelected) emptySet() else schedules.map { it.id }.toSet()
                            },
                        ) {
                            Text(
                                if (allSelected) "전체 해제" else "전체 선택",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(
                            onClick = { showDeleteSelectedConfirm = true },
                            enabled = selected.isNotEmpty(),
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = "선택 항목 삭제")
                        }
                    },
                )
            } else {
                ScreenTopBar(
                    title = "완료한 일정",
                    onBack = onBack,
                    actions = {
                        if (schedules.isNotEmpty()) {
                            TextButton(onClick = { selecting = true }) {
                                Text("선택", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                )
            }
        },
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
                    ChoiceChip(
                        label = startDate?.let { "${it.toHeaderDateString()}부터" } ?: "시작일",
                        selected = startDate != null,
                        onClick = { showStartPicker = true },
                        leadingIcon = Icons.Outlined.DateRange,
                    )
                    ChoiceChip(
                        label = endDate?.let { "${it.toHeaderDateString()}까지" } ?: "종료일",
                        selected = endDate != null,
                        onClick = { showEndPicker = true },
                        leadingIcon = Icons.Outlined.DateRange,
                    )
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
                            onRestore = ::restore,
                            onDelete = { delete(listOf(it)) },
                            selecting = selecting,
                            selected = schedule.id in selectedIds,
                            onToggleSelected = {
                                selectedIds = if (schedule.id in selectedIds) {
                                    selectedIds - schedule.id
                                } else {
                                    selectedIds + schedule.id
                                }
                            },
                            onLongPress = {
                                selecting = true
                                selectedIds = selectedIds + schedule.id
                            },
                        )
                        if (index < schedules.lastIndex) RowDivider()
                    }
                }
            }
        }
    }

    // Deleting several at once asks first; the undo snackbar still follows.
    if (showDeleteSelectedConfirm) {
        ConfirmDialog(
            title = "일정 삭제",
            text = "선택한 일정 ${selected.size}건을 삭제하시겠습니까?",
            confirmLabel = "삭제",
            destructive = true,
            onConfirm = {
                showDeleteSelectedConfirm = false
                delete(selected)
                exitSelection()
            },
            onDismiss = { showDeleteSelectedConfirm = false },
        )
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
