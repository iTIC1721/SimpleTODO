package com.simpletodo.simpletodo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.simpletodo.simpletodo.data.Schedule
import com.simpletodo.simpletodo.ui.theme.ButtonShape
import com.simpletodo.simpletodo.ui.theme.mutedText
import java.time.LocalDate
import java.time.LocalTime

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

    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)

    Scaffold(
        modifier = modifier,
        topBar = {
            ScreenTopBar(
                title = if (initialSchedule != null) "일정 수정" else "일정 추가",
                onBack = onBack,
                backLabel = "취소",
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = PageGutter, vertical = 12.dp),
            ) {
                PrimaryButton(
                    text = "저장",
                    enabled = title.isNotBlank(),
                    onClick = {
                        onSave(title, date, if (timeEnabled) time else null, important)
                        onBack()
                    },
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PageGutter, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("일정 내용") },
                shape = ButtonShape,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    unfocusedLabelColor = MaterialTheme.mutedText,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            FieldGroup(label = "날짜") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip(label = "오늘", selected = date == today, onClick = { date = today })
                    ChoiceChip(label = "내일", selected = date == tomorrow, onClick = { date = tomorrow })
                    ChoiceChip(
                        label = if (date == today || date == tomorrow) "날짜 선택" else date.toDisplayString(),
                        selected = date != today && date != tomorrow,
                        onClick = { showDatePicker = true },
                        leadingIcon = Icons.Outlined.DateRange,
                    )
                }
            }

            Column {
                SwitchRow(label = "시간 지정", checked = timeEnabled, onCheckedChange = { timeEnabled = it })
                if (timeEnabled) {
                    // A value to tap and change, not a selection — so it stays neutral.
                    ChoiceChip(
                        label = time.toDisplayString(),
                        selected = false,
                        onClick = { showTimePicker = true },
                    )
                }
                SwitchRow(
                    label = "중요 일정",
                    checked = important,
                    onCheckedChange = { important = it },
                    icon = Icons.Filled.Star,
                    iconTint = if (important) MaterialTheme.colorScheme.primary else MaterialTheme.mutedText,
                    modifier = Modifier.padding(top = 8.dp),
                )
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
        AppTimePickerDialog(
            initialTime = time,
            onTimeSelected = { time = it },
            onDismiss = { showTimePicker = false },
        )
    }
}
