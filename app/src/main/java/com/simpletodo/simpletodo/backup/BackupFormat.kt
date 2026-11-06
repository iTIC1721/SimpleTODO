package com.simpletodo.simpletodo.backup

import com.simpletodo.simpletodo.data.RecurringReminder
import com.simpletodo.simpletodo.data.Schedule
import com.simpletodo.simpletodo.data.ThemeMode
import com.simpletodo.simpletodo.data.daysOfWeekToMask
import com.simpletodo.simpletodo.data.maskToDaysOfWeek
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Settings carried along in a backup file. */
data class BackupSettings(
    val briefingTime: LocalTime,
    val leadMinutes: Int,
    val themeMode: ThemeMode,
)

data class BackupData(
    val schedules: List<Schedule>,
    val recurringReminders: List<RecurringReminder>,
    val settings: BackupSettings?,
)

class InvalidBackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * The backup file format — plain, human-readable JSON that is independent of
 * the Room schema, so a backup taken on one app version (or one signing key)
 * can be restored on another. Row ids are deliberately not stored; restored
 * rows get fresh ids.
 */
object BackupFormat {
    private const val FORMAT_ID = "simpletodo-backup"
    const val FORMAT_VERSION = 1

    fun encode(data: BackupData, exportedAt: Instant = Instant.now(), appVersion: String = ""): String {
        val root = JSONObject()
            .put("format", FORMAT_ID)
            .put("version", FORMAT_VERSION)
            .put("appVersion", appVersion)
            .put("exportedAt", exportedAt.toString())

        data.settings?.let { settings ->
            root.put(
                "settings",
                JSONObject()
                    .put("briefingTime", settings.briefingTime.toString())
                    .put("leadMinutes", settings.leadMinutes)
                    .put("theme", settings.themeMode.name),
            )
        }

        root.put("schedules", JSONArray().apply {
            data.schedules.forEach { s ->
                put(
                    JSONObject()
                        .put("title", s.title)
                        .put("date", s.date.toString())
                        .put("time", s.time?.toString() ?: JSONObject.NULL)
                        .put("isImportant", s.isImportant)
                        .put("isCompleted", s.isCompleted)
                        .put("completedAt", s.completedAt?.toString() ?: JSONObject.NULL)
                        .put("createdAt", s.createdAt.toString()),
                )
            }
        })

        root.put("recurringReminders", JSONArray().apply {
            data.recurringReminders.forEach { r ->
                put(
                    JSONObject()
                        .put("title", r.title)
                        .put("days", JSONArray(maskToDaysOfWeek(r.daysOfWeekMask).sorted().map { it.name }))
                        .put("time", r.time?.toString() ?: JSONObject.NULL)
                        .put("isEnabled", r.isEnabled)
                        .put("createdAt", r.createdAt.toString()),
                )
            }
        })

        return root.toString(2)
    }

    /** Parses a backup file, rejecting anything that isn't a well-formed SimpleTODO backup. */
    fun decode(json: String): BackupData {
        try {
            val root = JSONObject(json)
            if (root.optString("format") != FORMAT_ID) {
                throw InvalidBackupException("SimpleTODO 백업 파일 형식이 아닙니다.")
            }
            val version = root.getInt("version")
            if (version > FORMAT_VERSION) {
                throw InvalidBackupException("상위 버전의 앱에서 생성된 백업 파일입니다. 앱을 최신 버전으로 업데이트한 후 다시 시도해 주십시오.")
            }

            val settings = root.optJSONObject("settings")?.let { s ->
                BackupSettings(
                    briefingTime = LocalTime.parse(s.getString("briefingTime")),
                    leadMinutes = s.getInt("leadMinutes").coerceAtLeast(0),
                    themeMode = runCatching { ThemeMode.valueOf(s.getString("theme")) }.getOrDefault(ThemeMode.SYSTEM),
                )
            }

            val schedules = root.getJSONArray("schedules").objects().map { o ->
                Schedule(
                    title = o.getString("title"),
                    date = LocalDate.parse(o.getString("date")),
                    time = o.optStringOrNull("time")?.let(LocalTime::parse),
                    isImportant = o.optBoolean("isImportant", false),
                    isCompleted = o.optBoolean("isCompleted", false),
                    completedAt = o.optStringOrNull("completedAt")?.let(Instant::parse),
                    createdAt = Instant.parse(o.getString("createdAt")),
                )
            }

            val reminders = root.getJSONArray("recurringReminders").objects().map { o ->
                val days = o.getJSONArray("days").let { arr ->
                    (0 until arr.length()).mapTo(mutableSetOf()) { DayOfWeek.valueOf(arr.getString(it)) }
                }
                RecurringReminder(
                    title = o.getString("title"),
                    daysOfWeekMask = daysOfWeekToMask(days),
                    time = o.optStringOrNull("time")?.let(LocalTime::parse),
                    isEnabled = o.optBoolean("isEnabled", true),
                    createdAt = Instant.parse(o.getString("createdAt")),
                )
            }

            return BackupData(schedules, reminders, settings)
        } catch (e: InvalidBackupException) {
            throw e
        } catch (e: Exception) {
            throw InvalidBackupException("백업 파일을 읽을 수 없습니다. 파일이 손상되었을 가능성이 있습니다.", e)
        }
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else getString(key)
}
