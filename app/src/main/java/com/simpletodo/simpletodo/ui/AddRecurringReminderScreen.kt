package com.simpletodo.simpletodo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.simpletodo.simpletodo.data.AlarmPreferences
import com.simpletodo.simpletodo.data.RecurringReminder
import com.simpletodo.simpletodo.data.daysOfWeek
import com.simpletodo.simpletodo.ui.theme.ButtonShape
import com.simpletodo.simpletodo.ui.theme.mutedText
import java.time.DayOfWeek
import java.time.LocalTime

@Composable
fun AddRecurringReminderScreen(
    onSave: (title: String, days: Set<DayOfWeek>, time: LocalTime?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialReminder: RecurringReminder? = null,
) {
    val context = LocalContext.current
    val briefingTime = remember { AlarmPreferences(context).defaultNotifyTime }
    var title by rememberSaveable { mutableStateOf(initialReminder?.title ?: "") }
    var selectedDays by remember { mutableStateOf(initialReminder?.daysOfWeek() ?: emptySet()) }
    var timeEnabled by rememberSaveable { mutableStateOf(initialReminder?.time != null) }
    var time by remember { mutableStateOf(initialReminder?.time ?: LocalTime.of(9, 0)) }
    var showTimePicker by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    Scaffold(
        modifier = modifier,
        topBar = {
            ScreenTopBar(
                title = if (initialReminder != null) "반복 알림 수정" else "반복 알림 추가",
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
                    enabled = title.isNotBlank() && selectedDays.isNotEmpty(),
                    onClick = {
                        onSave(title, selectedDays, if (timeEnabled) time else null)
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
                label = { Text("알림 내용") },
                shape = ButtonShape,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    unfocusedLabelColor = MaterialTheme.mutedText,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            FieldGroup(label = "요일") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    DayOfWeek.entries.forEach { day ->
                        DayToggle(
                            label = koreanDayOfWeekLabels.getValue(day),
                            selected = day in selectedDays,
                            onClick = {
                                selectedDays = if (day in selectedDays) selectedDays - day else selectedDays + day
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
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
                } else {
                    Text(
                        "시간 미지정 시 아침 브리핑 시각(${briefingTime.toDisplayString()})에 알림 발송",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.mutedText,
                    )
                }
            }
        }
    }

    if (showTimePicker) {
        AppTimePickerDialog(
            initialTime = time,
            onTimeSelected = { time = it },
            onDismiss = { showTimePicker = false },
        )
    }
}

private val DayShape = RoundedCornerShape(10.dp)

/** One day of the week: seven equal cells that fill the row, accent when picked. */
@Composable
private fun DayToggle(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(DayShape)
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, DayShape)
            .border(1.dp, if (selected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant, DayShape)
            .clickable(role = Role.Checkbox, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        )
    }
}
