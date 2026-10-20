package com.simpletodo.simpletodo.ui

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.simpletodo.simpletodo.data.AlarmPreferences
import com.simpletodo.simpletodo.data.ThemeMode
import com.simpletodo.simpletodo.data.ThemePreferences
import com.simpletodo.simpletodo.notification.AlarmScheduler
import com.simpletodo.simpletodo.notification.RecurringReminderScheduler
import com.simpletodo.simpletodo.widget.ScheduleWidgetUpdater
import kotlinx.coroutines.launch
import java.time.LocalTime

private fun canScheduleExactAlarms(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    return alarmManager.canScheduleExactAlarms()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val preferences = remember { AlarmPreferences(context) }
    val themePreferences = remember { ThemePreferences(context) }
    val coroutineScope = rememberCoroutineScope()

    var notifyTime by remember { mutableStateOf(preferences.defaultNotifyTime) }
    var leadMinutesText by remember { mutableStateOf(preferences.leadMinutes.toString()) }
    var showTimePicker by remember { mutableStateOf(false) }
    var exactAlarmGranted by remember { mutableStateOf(canScheduleExactAlarms(context)) }
    var themeMode by remember { mutableStateOf(themePreferences.themeMode) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("설정") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("닫기") }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("테마", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val options = listOf(
                        ThemeMode.SYSTEM to "시스템 기본",
                        ThemeMode.LIGHT to "라이트",
                        ThemeMode.DARK to "다크",
                    )
                    options.forEach { (mode, label) ->
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = {
                                themeMode = mode
                                themePreferences.themeMode = mode
                                ThemeState.set(mode)
                                coroutineScope.launch { ScheduleWidgetUpdater.updateAll(context) }
                            },
                            label = { Text(label) },
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("매일 아침 브리핑 시각", style = MaterialTheme.typography.labelLarge)
                Text(
                    "이 시각에 그날 등록된 모든 일정을 하나의 알림으로 모아 보여드려요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AssistChip(
                    onClick = { showTimePicker = true },
                    label = { Text(notifyTime.toDisplayString()) },
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("시간이 있는 일정 — 별도 알림", style = MaterialTheme.typography.labelLarge)
                Text(
                    "시간까지 지정한 일정은 아침 브리핑과 별개로, 그 시각 몇 분 전에 한 번 더 알려드려요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = leadMinutesText,
                    onValueChange = { leadMinutesText = it.filter(Char::isDigit).take(4) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text("분 전") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
            }

            if (!exactAlarmGranted) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "정확한 시각에 알림을 받으려면 권한이 필요합니다. 허용하지 않아도 알림은 오지만 시각이 다소 부정확할 수 있습니다.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Button(onClick = {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }) {
                            Text("권한 설정 열기")
                        }
                    }
                }
            }

            Button(
                onClick = {
                    preferences.defaultNotifyHour = notifyTime.hour
                    preferences.defaultNotifyMinute = notifyTime.minute
                    preferences.leadMinutes = leadMinutesText.toIntOrNull()?.coerceAtLeast(0)
                        ?: AlarmPreferences.DEFAULT_LEAD_MINUTES
                    AlarmScheduler.scheduleDailyBriefing(context)
                    coroutineScope.launch { RecurringReminderScheduler.rescheduleAll(context) }
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("저장")
            }
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = notifyTime.hour,
            initialMinute = notifyTime.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    notifyTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
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
