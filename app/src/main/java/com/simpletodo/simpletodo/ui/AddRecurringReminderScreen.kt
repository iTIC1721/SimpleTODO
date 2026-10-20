package com.simpletodo.simpletodo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.simpletodo.simpletodo.data.RecurringReminder
import com.simpletodo.simpletodo.data.daysOfWeek
import java.time.DayOfWeek
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringReminderScreen(
    onSave: (title: String, days: Set<DayOfWeek>, time: LocalTime?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialReminder: RecurringReminder? = null,
) {
    var title by rememberSaveable { mutableStateOf(initialReminder?.title ?: "") }
    var selectedDays by remember { mutableStateOf(initialReminder?.daysOfWeek() ?: emptySet()) }
    var timeEnabled by rememberSaveable { mutableStateOf(initialReminder?.time != null) }
    var time by remember { mutableStateOf(initialReminder?.time ?: LocalTime.of(9, 0)) }
    var showTimePicker by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (initialReminder != null) "반복 알림 수정" else "반복 알림 추가") },
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
                label = { Text("알림 내용") },
                modifier = Modifier.fillMaxWidth(),
            )

            Text("요일")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DayOfWeek.entries.forEach { day ->
                    FilterChip(
                        selected = day in selectedDays,
                        onClick = {
                            selectedDays = if (day in selectedDays) selectedDays - day else selectedDays + day
                        },
                        label = { Text(koreanDayOfWeekLabels.getValue(day)) },
                    )
                }
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
            if (!timeEnabled) {
                Text(
                    "시간을 지정하지 않으면 설정의 아침 브리핑 시각에 알림을 보내드려요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Button(
                onClick = {
                    onSave(title, selectedDays, if (timeEnabled) time else null)
                    onBack()
                },
                enabled = title.isNotBlank() && selectedDays.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("저장")
            }
        }
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
