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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.DateRange
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
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
    onDelete: (() -> Unit)? = null,
) {
    var title by rememberSaveable { mutableStateOf(initialSchedule?.title ?: "") }
    var date by rememberSaveable { mutableStateOf(initialSchedule?.date ?: LocalDate.now()) }
    var timeEnabled by rememberSaveable { mutableStateOf(initialSchedule?.time != null) }
    var time by remember { mutableStateOf(initialSchedule?.time ?: LocalTime.of(9, 0)) }
    var important by rememberSaveable { mutableStateOf(initialSchedule?.isImportant ?: false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Leaving with unsaved edits asks first. A new schedule counts as edited
    // once it has a title; an existing one once any field differs.
    val edited = if (initialSchedule == null) {
        title.isNotBlank()
    } else {
        title != initialSchedule.title || date != initialSchedule.date ||
            (if (timeEnabled) time else null) != initialSchedule.time ||
            important != initialSchedule.isImportant
    }
    val requestExit = { if (edited) showDiscardConfirm = true else onBack() }
    BackHandler(onBack = requestExit)

    val focusManager = LocalFocusManager.current
    val titleFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (initialSchedule == null) titleFocus.requestFocus()
    }

    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)

    Scaffold(
        modifier = modifier,
        topBar = {
            ScreenTopBar(
                title = if (initialSchedule != null) "일정 수정" else "일정 추가",
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
                // A title is one line of text: Enter finishes typing instead of adding a line break.
                onValueChange = { title = it.replace('\n', ' ') },
                label = { Text("일정 내용") },
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
            title = "일정 삭제",
            text = "이 일정을 삭제하시겠습니까?",
            confirmLabel = "삭제",
            destructive = true,
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            },
            onDismiss = { showDeleteConfirm = false },
        )
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
