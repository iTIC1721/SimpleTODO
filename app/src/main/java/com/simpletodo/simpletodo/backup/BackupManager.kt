package com.simpletodo.simpletodo.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.simpletodo.simpletodo.data.AlarmPreferences
import com.simpletodo.simpletodo.data.AppDatabase
import com.simpletodo.simpletodo.data.ThemePreferences
import com.simpletodo.simpletodo.notification.AlarmScheduler
import com.simpletodo.simpletodo.notification.RecurringReminderScheduler
import com.simpletodo.simpletodo.ui.ThemeState
import com.simpletodo.simpletodo.widget.ScheduleWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class BackupSummary(val scheduleCount: Int, val reminderCount: Int)

/**
 * Exports everything the user owns (schedules, recurring reminders, settings)
 * to a file the user picks, and restores it again. Restoring replaces the
 * current data entirely — it is meant for moving to a fresh install, e.g.
 * across a signing-key change where Android won't allow an in-place update.
 */
object BackupManager {

    suspend fun export(context: Context, uri: Uri): BackupSummary = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val alarmPrefs = AlarmPreferences(context)
        val data = BackupData(
            schedules = db.scheduleDao().getAllOnce(),
            recurringReminders = db.recurringReminderDao().getAllOnce(),
            settings = BackupSettings(
                briefingTime = alarmPrefs.defaultNotifyTime,
                leadMinutes = alarmPrefs.leadMinutes,
                themeMode = ThemePreferences(context).themeMode,
                scheduleAlertEnabled = alarmPrefs.scheduleAlertEnabled,
            ),
        )
        val json = BackupFormat.encode(data, appVersion = appVersion(context))
        val stream = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IllegalStateException("백업 파일을 열 수 없습니다.")
        stream.bufferedWriter(Charsets.UTF_8).use { it.write(json) }
        BackupSummary(data.schedules.size, data.recurringReminders.size)
    }

    /** Throws [InvalidBackupException] before touching any data if the file isn't a valid backup. */
    suspend fun restore(context: Context, uri: Uri): BackupSummary = withContext(Dispatchers.IO) {
        val json = context.contentResolver.openInputStream(uri)
            ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: throw InvalidBackupException("백업 파일을 열 수 없습니다.")
        val data = BackupFormat.decode(json)

        val db = AppDatabase.getInstance(context)
        val scheduleDao = db.scheduleDao()
        val reminderDao = db.recurringReminderDao()

        // Alarms are keyed by row id, and restored rows get fresh ids — cancel
        // the outgoing rows' alarms first so none of them fire for deleted data.
        scheduleDao.getAllOnce().forEach { AlarmScheduler.cancel(context, it.id) }
        reminderDao.getAllOnce().forEach { RecurringReminderScheduler.cancel(context, it.id) }

        db.withTransaction {
            scheduleDao.deleteAll()
            reminderDao.deleteAll()
            scheduleDao.insertAll(data.schedules.map { it.copy(id = 0) })
            reminderDao.insertAll(data.recurringReminders.map { it.copy(id = 0) })
        }

        data.settings?.let { settings ->
            AlarmPreferences(context).apply {
                defaultNotifyHour = settings.briefingTime.hour
                defaultNotifyMinute = settings.briefingTime.minute
                leadMinutes = settings.leadMinutes
                scheduleAlertEnabled = settings.scheduleAlertEnabled
            }
            ThemePreferences(context).themeMode = settings.themeMode
            ThemeState.set(settings.themeMode)
        }

        scheduleDao.getAllOnce().filterNot { it.isCompleted }.forEach { AlarmScheduler.schedule(context, it) }
        AlarmScheduler.scheduleDailyBriefing(context)
        RecurringReminderScheduler.rescheduleAll(context)
        ScheduleWidgetUpdater.updateAll(context)

        BackupSummary(data.schedules.size, data.recurringReminders.size)
    }

    private fun appVersion(context: Context): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull().orEmpty()
}
