package com.simpletodo.simpletodo.backup

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.simpletodo.simpletodo.data.AlarmPreferences
import com.simpletodo.simpletodo.data.AppDatabase
import com.simpletodo.simpletodo.data.RecurringReminder
import com.simpletodo.simpletodo.data.Schedule
import com.simpletodo.simpletodo.data.ThemeMode
import com.simpletodo.simpletodo.data.ThemePreferences
import com.simpletodo.simpletodo.data.daysOfWeekToMask
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class BackupTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val schedules = listOf(
        Schedule(
            title = "치과 \"예약\"", date = LocalDate.of(2026, 11, 20), time = LocalTime.of(14, 30),
            isImportant = true, createdAt = Instant.parse("2026-11-01T10:00:00Z"),
        ),
        Schedule(
            title = "장보기", date = LocalDate.of(2026, 11, 3), isCompleted = true,
            completedAt = Instant.parse("2026-11-03T09:15:30.123Z"), createdAt = Instant.parse("2026-11-02T08:00:00Z"),
        ),
    )
    private val reminders = listOf(
        RecurringReminder(
            title = "게임 출석체크", time = LocalTime.of(7, 0), createdAt = Instant.parse("2026-10-01T00:00:00Z"),
            daysOfWeekMask = daysOfWeekToMask(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)),
        ),
        RecurringReminder(
            title = "강아지 산책", isEnabled = false, createdAt = Instant.parse("2026-10-02T00:00:00Z"),
            daysOfWeekMask = daysOfWeekToMask(setOf(DayOfWeek.WEDNESDAY, DayOfWeek.SUNDAY)),
        ),
    )
    private val settings = BackupSettings(LocalTime.of(7, 30), 20, ThemeMode.DARK)

    @Test
    fun encodeThenDecode_roundTripsEveryField() {
        val json = BackupFormat.encode(BackupData(schedules, reminders, settings))
        val decoded = BackupFormat.decode(json)

        assertEquals(schedules, decoded.schedules)
        assertEquals(reminders, decoded.recurringReminders)
        assertEquals(settings, decoded.settings)
    }

    @Test
    fun scheduleAlertSetting_roundTripsAndIsOnForOlderBackups() {
        val off = settings.copy(scheduleAlertEnabled = false)
        val json = BackupFormat.encode(BackupData(schedules, reminders, off))
        assertEquals(off, BackupFormat.decode(json).settings)

        // A backup from before 1.2.0 has no such key; alerts were always on then.
        val older = JSONObject(json).apply { getJSONObject("settings").remove("scheduleAlertEnabled") }
        assertEquals(true, BackupFormat.decode(older.toString()).settings!!.scheduleAlertEnabled)
    }

    @Test
    fun readText_acceptsBackupsAndRejectsNonJsonAndOversizedFiles() {
        val json = BackupFormat.encode(BackupData(schedules, reminders, settings))
        assertEquals(json, BackupFormat.readText(json.byteInputStream()))
        // A byte-order mark and leading whitespace, as some editors save it, are fine.
        val withBom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "\n  $json".toByteArray()
        assertEquals(BackupFormat.decode(json), BackupFormat.decode(BackupFormat.readText(withBom.inputStream())))

        // A photo/video is rejected from its first bytes, without reading it whole.
        listOf(byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte()), ByteArray(0)).forEach {
            try {
                BackupFormat.readText(it.inputStream())
                fail("expected rejection")
            } catch (e: InvalidBackupException) {
                assert(e.message!!.contains("형식이 아닙니다"))
            }
        }

        // Over the size limit (a small limit here instead of 50MB).
        try {
            BackupFormat.readText(json.byteInputStream(), maxBytes = 100)
            fail("expected rejection")
        } catch (e: InvalidBackupException) {
            assertEquals("백업 파일이 너무 큽니다.", e.message)
        }
    }

    @Test
    fun decode_rejectsForeignJsonAndGarbage() {
        listOf("""{"hello":"world"}""", "not json at all", "").forEach { input ->
            try {
                BackupFormat.decode(input)
                fail("expected rejection for: $input")
            } catch (_: InvalidBackupException) {
            }
        }
    }

    @Test
    fun decode_rejectsNewerFormatVersion() {
        val json = BackupFormat.encode(BackupData(schedules, reminders, settings))
            .replace("\"version\": ${BackupFormat.FORMAT_VERSION}", "\"version\": 99")
        try {
            BackupFormat.decode(json)
            fail("expected rejection")
        } catch (e: InvalidBackupException) {
            assert(e.message!!.contains("상위 버전"))
        }
    }

    @Test
    fun exportThenRestore_replacesDatabaseContentsAndSettings() = runBlocking<Unit> {
        val db = AppDatabase.getInstance(context)
        val scheduleDao = db.scheduleDao()
        val reminderDao = db.recurringReminderDao()
        val file = File(context.cacheDir, "backup-test.json")

        // Source data, exported.
        scheduleDao.deleteAll(); reminderDao.deleteAll()
        scheduleDao.insertAll(schedules)
        reminderDao.insertAll(reminders)
        AlarmPreferences(context).apply {
            defaultNotifyHour = 7; defaultNotifyMinute = 30; leadMinutes = 20; scheduleAlertEnabled = false
        }
        ThemePreferences(context).themeMode = ThemeMode.DARK
        val exported = BackupManager.export(context, Uri.fromFile(file))
        assertEquals(BackupSummary(2, 2), exported)

        // Different data and settings in place, then restore over them.
        scheduleDao.deleteAll(); reminderDao.deleteAll()
        scheduleDao.insert(Schedule(title = "지워져야 할 일정", date = LocalDate.of(2026, 12, 1)))
        AlarmPreferences(context).apply {
            defaultNotifyHour = 9; defaultNotifyMinute = 0; leadMinutes = 5; scheduleAlertEnabled = true
        }
        ThemePreferences(context).themeMode = ThemeMode.LIGHT

        val restored = BackupManager.restore(context, Uri.fromFile(file))

        assertEquals(BackupSummary(2, 2), restored)
        assertEquals(schedules, scheduleDao.getAllOnce().map { it.copy(id = 0) })
        assertEquals(reminders, reminderDao.getAllOnce().map { it.copy(id = 0) })
        assertEquals(LocalTime.of(7, 30), AlarmPreferences(context).defaultNotifyTime)
        assertEquals(20, AlarmPreferences(context).leadMinutes)
        assertEquals(false, AlarmPreferences(context).scheduleAlertEnabled)
        assertEquals(ThemeMode.DARK, ThemePreferences(context).themeMode)

        AlarmPreferences(context).scheduleAlertEnabled = true
        scheduleDao.deleteAll(); reminderDao.deleteAll()
        file.delete()
    }

    @Test
    fun restoreOfInvalidFile_leavesExistingDataUntouched() = runBlocking<Unit> {
        val scheduleDao = AppDatabase.getInstance(context).scheduleDao()
        val file = File(context.cacheDir, "not-a-backup.json").apply { writeText("""{"format":"other"}""") }
        scheduleDao.deleteAll()
        scheduleDao.insert(Schedule(title = "남아 있어야 할 일정", date = LocalDate.of(2026, 12, 1)))

        try {
            BackupManager.restore(context, Uri.fromFile(file))
            fail("expected rejection")
        } catch (_: InvalidBackupException) {
        }

        assertEquals(listOf("남아 있어야 할 일정"), scheduleDao.getAllOnce().map { it.title })
        scheduleDao.deleteAll()
        file.delete()
    }
}
