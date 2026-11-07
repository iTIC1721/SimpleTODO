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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
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
    onDelete: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val briefingTime = remember { AlarmPreferences(context).defaultNotifyTime }
    var title by rememberSaveable { mutableStateOf(initialReminder?.title ?: "") }
    var selectedDays by remember { mutableStateOf(initialReminder?.daysOfWeek() ?: emptySet()) }
    var timeEnabled by rememberSaveable { mutableStateOf(initialReminder?.time != null) }
    var time by remember { mutableStateOf(initialReminder?.time ?: LocalTime.of(9, 0)) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Leaving with unsaved edits asks first. A new reminder counts as edited
    // once it has a title; an existing one once any field differs.
    val edited = if (initialReminder == null) {
        title.isNotBlank()
    } else {
        title != initialReminder.title || selectedDays != initialReminder.daysOfWeek() ||
            (if (timeEnabled) time else null) != initialReminder.time
    }
    val requestExit = { if (edited) showDiscardConfirm = true else onBack() }
    BackHandler(onBack = requestExit)

    val focusManager = LocalFocusManager.current
    val titleFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (initialReminder == null) titleFocus.requestFocus()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            ScreenTopBar(
                title = if (initialReminder != null) "반복 알림 수정" else "반복 알림 추가",
                onBack = requestExit,
                backLabel = "취소",
                actions = {
                    if (onDelete != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "삭제", tint = MaterialTheme.mutedText)
                        }
                    }
                },
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
                // A title is one line of text: Enter finishes typing instead of adding a line break.
                onValueChange = { title = it.replace('\n', ' ') },
                label = { Text("알림 내용") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                shape = ButtonShape,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    unfocusedLabelColor = MaterialTheme.mutedText,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(titleFocus),
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
                // Why 저장 is disabled while no day is picked.
                if (selectedDays.isEmpty()) {
                    Text(
                        "요일 1개 이상 선택",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.mutedText,
                        modifier = Modifier.padding(top = 8.dp),
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

    if (showDiscardConfirm) {
        ConfirmDialog(
            text = "저장하지 않은 변경 사항이 있습니다. 저장하지 않고 나가시겠습니까?",
            confirmLabel = "나가기",
            dismissLabel = "계속 편집",
            destructive = true,
            onConfirm = {
                showDiscardConfirm = false
                onBack()
            },
            onDismiss = { showDiscardConfirm = false },
        )
    }

    if (showDeleteConfirm && onDelete != null) {
        ConfirmDialog(
            title = "반복 알림 삭제",
            text = "이 반복 알림을 삭제하시겠습니까?",
            confirmLabel = "삭제",
            destructive = true,
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            },
            onDismiss = { showDeleteConfirm = false },
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
