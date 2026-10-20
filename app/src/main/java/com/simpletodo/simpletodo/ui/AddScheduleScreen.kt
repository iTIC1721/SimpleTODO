package com.simpletodo.simpletodo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.simpletodo.simpletodo.data.Schedule
import com.simpletodo.simpletodo.ui.theme.ImportantAccent
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScheduleScreen(
    onSave: (title: String, date: LocalDate, time: LocalTime?, isImportant: Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialSchedule: Schedule? = null,
) {
    var title by rememberSaveable { mutableStateOf(initialSchedule?.title ?: "") }
    var date by rememberSaveable { mutableStateOf(initialSchedule?.date ?: LocalDate.now()) }
    var timeEnabled by rememberSaveable { mutableStateOf(initialSchedule?.time != null) }
    var time by remember { mutableStateOf(initialSchedule?.time ?: LocalTime.of(9, 0)) }
    var important by rememberSaveable { mutableStateOf(initialSchedule?.isImportant ?: false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (initialSchedule != null) "일정 수정" else "일정 추가") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("취소") }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("일정 내용") },
                modifier = Modifier.fillMaxWidth(),
            )

            Text("날짜")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = date == LocalDate.now(),
                    onClick = { date = LocalDate.now() },
                    label = { Text("오늘") },
                )
                FilterChip(
                    selected = date == LocalDate.now().plusDays(1),
                    onClick = { date = LocalDate.now().plusDays(1) },
                    label = { Text("내일") },
                )
                AssistChip(
                    onClick = { showDatePicker = true },
                    label = { Text(date.toDisplayString()) },
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = timeEnabled,
                    onCheckedChange = { timeEnabled = it },
                )
                Text("시간 지정")
                if (timeEnabled) {
                    Spacer(Modifier.width(8.dp))
                    AssistChip(
                        onClick = { showTimePicker = true },
                        label = { Text(time.toDisplayString()) },
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = important,
                    onClick = { important = !important },
                    label = { Text("중요") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ImportantAccent,
                        selectedLabelColor = Color.Black,
                    ),
                )
            }

            Button(
                onClick = {
                    onSave(title, date, if (timeEnabled) time else null, important)
                    onBack()
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("저장")
            }
        }
    }

    if (showDatePicker) {
        AppDatePickerDialog(
            initialDate = date,
            onDateSelected = { date = it },
            onDismiss = { showDatePicker = false },
        )
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    time = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("취소") }
            },
            text = { TimePicker(state = timePickerState) },
        )
    }
}
