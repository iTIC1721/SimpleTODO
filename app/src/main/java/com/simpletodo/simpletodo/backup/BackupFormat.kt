package com.simpletodo.simpletodo.backup

import com.simpletodo.simpletodo.data.RecurringReminder
import com.simpletodo.simpletodo.data.Schedule
import com.simpletodo.simpletodo.data.ThemeMode
import com.simpletodo.simpletodo.data.daysOfWeekToMask
import com.simpletodo.simpletodo.data.maskToDaysOfWeek
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Settings carried along in a backup file. */
data class BackupSettings(
    val briefingTime: LocalTime,
    val leadMinutes: Int,
    val themeMode: ThemeMode,
    val scheduleAlertEnabled: Boolean = true,
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

    /**
     * Upper bound on a backup file's size. A schedule takes about 180–240
     * bytes, so this is roughly 220,000 schedules — far beyond real use (20 a
     * day for 30 years). It only exists so that picking the wrong large file
     * can't exhaust memory, since the file is read whole.
     */
    const val MAX_BACKUP_BYTES = 50L * 1024 * 1024

    private const val NOT_A_BACKUP = "SimpleTODO 백업 파일 형식이 아닙니다."
    private const val TOO_LARGE = "백업 파일이 너무 큽니다."

    /**
     * Reads a backup file's text, rejecting obvious non-backups before reading
     * them whole: a backup is JSON, so its first character (after an optional
     * byte-order mark and whitespace) is '{' — a photo or video fails that at
     * once, however large. Anything over [maxBytes] is rejected as well.
     */
    fun readText(input: InputStream, maxBytes: Long = MAX_BACKUP_BYTES): String {
        val stream = input.buffered()
        stream.mark(PEEK_BYTES)
        val head = ByteArray(PEEK_BYTES)
        val headLength = stream.read(head).coerceAtLeast(0)
        val first = head.take(headLength).dropBom().firstOrNull { it.toInt().toChar() !in " \t\r\n" }
        if (first?.toInt()?.toChar() != '{') throw InvalidBackupException(NOT_A_BACKUP)
        stream.reset()

        val out = ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val read = stream.read(buffer)
            if (read < 0) break
            total += read
            if (total > maxBytes) throw InvalidBackupException(TOO_LARGE)
            out.write(buffer, 0, read)
        }
        return out.toString(Charsets.UTF_8.name()).removePrefix("\uFEFF")
    }

    private const val PEEK_BYTES = 64

    private fun List<Byte>.dropBom(): List<Byte> =
        if (size >= 3 && this[0] == 0xEF.toByte() && this[1] == 0xBB.toByte() && this[2] == 0xBF.toByte()) drop(3) else this

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
                    .put("theme", settings.themeMode.name)
                    .put("scheduleAlertEnabled", settings.scheduleAlertEnabled),
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
                throw InvalidBackupException(NOT_A_BACKUP)
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
                    // Added in 1.2.0; older backups don't have it and had alerts on.
                    scheduleAlertEnabled = s.optBoolean("scheduleAlertEnabled", true),
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
