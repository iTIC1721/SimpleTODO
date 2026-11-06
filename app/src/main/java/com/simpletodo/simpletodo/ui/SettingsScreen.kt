package com.simpletodo.simpletodo.ui

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.simpletodo.simpletodo.backup.BackupManager
import com.simpletodo.simpletodo.backup.InvalidBackupException
import com.simpletodo.simpletodo.data.AlarmPreferences
import com.simpletodo.simpletodo.data.ThemeMode
import com.simpletodo.simpletodo.data.ThemePreferences
import com.simpletodo.simpletodo.notification.AlarmScheduler
import com.simpletodo.simpletodo.notification.RecurringReminderScheduler
import com.simpletodo.simpletodo.ui.theme.ButtonShape
import com.simpletodo.simpletodo.ui.theme.mutedText
import com.simpletodo.simpletodo.widget.ScheduleWidgetUpdater
import kotlinx.coroutines.launch
import java.time.LocalDate

private fun canScheduleExactAlarms(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    return alarmManager.canScheduleExactAlarms()
}

@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenAppInfo: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val preferences = remember { AlarmPreferences(context) }
    val themePreferences = remember { ThemePreferences(context) }
    val coroutineScope = rememberCoroutineScope()

    var notifyTime by remember { mutableStateOf(preferences.defaultNotifyTime) }
    var leadMinutesText by remember { mutableStateOf(preferences.leadMinutes.toString()) }
    var showTimePicker by remember { mutableStateOf(false) }
    val exactAlarmGranted = remember { canScheduleExactAlarms(context) }
    var themeMode by remember { mutableStateOf(themePreferences.themeMode) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var backupInProgress by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        backupInProgress = true
        coroutineScope.launch {
            val message = runCatching { BackupManager.export(context, uri) }.fold(
                onSuccess = { "백업이 완료되었습니다.\n일정 ${it.scheduleCount}건, 반복 알림 ${it.reminderCount}건" },
                onFailure = { "백업에 실패했습니다. ${it.message}" },
            )
            backupInProgress = false
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    // Any file type: a backup passed through a messenger or cloud drive often
    // loses its JSON mime type. The content itself is validated on restore.
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> pendingRestoreUri = uri }

    Scaffold(
        modifier = modifier,
        topBar = { ScreenTopBar(title = "설정", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PageGutter)
                .padding(bottom = 32.dp),
        ) {
            SettingsSection(title = "화면") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        ThemeMode.SYSTEM to "시스템 기본",
                        ThemeMode.LIGHT to "라이트",
                        ThemeMode.DARK to "다크",
                    ).forEach { (mode, label) ->
                        ChoiceChip(
                            label = label,
                            selected = themeMode == mode,
                            onClick = {
                                themeMode = mode
                                themePreferences.themeMode = mode
                                ThemeState.set(mode)
                                coroutineScope.launch { ScheduleWidgetUpdater.updateAll(context) }
                            },
                        )
                    }
                }
            }

            SettingsSection(title = "알림") {
                SettingRow(
                    title = "아침 브리핑",
                    description = "그날 일정을 이 시각에 알림 하나로 모아 발송",
                ) {
                    ChoiceChip(label = notifyTime.toDisplayString(), selected = false, onClick = { showTimePicker = true })
                }
                SettingRow(
                    title = "시간 지정 일정 알림",
                    description = "브리핑과 별도로 지정 시각 전 추가 알림 발송",
                ) {
                    OutlinedTextField(
                        value = leadMinutesText,
                        onValueChange = { leadMinutesText = it.filter(Char::isDigit).take(4) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        suffix = { Text("분 전", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        shape = ButtonShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                        modifier = Modifier.width(116.dp),
                    )
                }
                if (!exactAlarmGranted) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                    ) {
                        Text(
                            "정확한 시각에 알림을 받으려면 '알람 및 리마인더' 권한이 필요합니다. 권한을 허용하지 않으면 알림이 다소 지연될 수 있습니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        TextButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.align(Alignment.End),
                        ) { Text("권한 설정 열기") }
                    }
                }
                PrimaryButton(
                    text = "저장",
                    onClick = {
                        preferences.defaultNotifyHour = notifyTime.hour
                        preferences.defaultNotifyMinute = notifyTime.minute
                        preferences.leadMinutes = leadMinutesText.toIntOrNull()?.coerceAtLeast(0)
                            ?: AlarmPreferences.DEFAULT_LEAD_MINUTES
                        AlarmScheduler.scheduleDailyBriefing(context)
                        coroutineScope.launch { RecurringReminderScheduler.rescheduleAll(context) }
                        onBack()
                    },
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            SettingsSection(title = "백업") {
                Text(
                    "일정, 반복 알림, 설정을 파일 하나로 저장\n복원 시 현재 데이터는 백업 내용으로 대체",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    QuietButton(
                        text = "파일로 백업",
                        enabled = !backupInProgress,
                        onClick = { exportLauncher.launch("SimpleTODO-backup-${LocalDate.now()}.json") },
                    )
                    QuietButton(
                        text = "파일에서 복원",
                        enabled = !backupInProgress,
                        onClick = { restoreLauncher.launch(arrayOf("*/*")) },
                    )
                }
            }

            SettingsSection(title = "정보") {
                val version = remember { appVersionText(context).substringBefore(" ") }
                LinkRow(label = "앱 정보", value = "버전 $version", onClick = onOpenAppInfo)
            }
        }
    }

    pendingRestoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            title = { Text("백업 복원", style = MaterialTheme.typography.titleLarge) },
            text = { Text("현재 저장된 일정과 반복 알림이 모두 삭제되고 백업 파일의 내용으로 대체됩니다. 복원을 진행하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestoreUri = null
                    backupInProgress = true
                    coroutineScope.launch {
                        val message = runCatching { BackupManager.restore(context, uri) }.fold(
                            onSuccess = {
                                // Settings were restored too — refresh this screen's
                                // copies so a later "저장" doesn't overwrite them.
                                notifyTime = preferences.defaultNotifyTime
                                leadMinutesText = preferences.leadMinutes.toString()
                                themeMode = themePreferences.themeMode
                                "복원이 완료되었습니다.\n일정 ${it.scheduleCount}건, 반복 알림 ${it.reminderCount}건"
                            },
                            onFailure = {
                                if (it is InvalidBackupException) it.message.orEmpty()
                                else "복원에 실패했습니다. ${it.message}"
                            },
                        )
                        backupInProgress = false
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                }) { Text("복원", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreUri = null }) {
                    Text("취소", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }

    if (showTimePicker) {
        AppTimePickerDialog(
            initialTime = notifyTime,
            onTimeSelected = { notifyTime = it },
            onDismiss = { showTimePicker = false },
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(top = 24.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.mutedText,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        content()
    }
}

/** A setting: title and one line of explanation on the left, its control on the right. */
@Composable
private fun SettingRow(title: String, description: String, control: @Composable () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            control()
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun QuietButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = ButtonShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
    }
}
