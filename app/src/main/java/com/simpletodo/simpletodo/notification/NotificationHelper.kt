package com.simpletodo.simpletodo.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.simpletodo.simpletodo.MainActivity
import com.simpletodo.simpletodo.R
import com.simpletodo.simpletodo.data.RecurringReminder
import com.simpletodo.simpletodo.data.Schedule
import java.time.format.DateTimeFormatter
import java.util.Locale

object NotificationHelper {
    private const val CHANNEL_ID = "schedule_reminders"
    private const val BRIEFING_NOTIFICATION_ID = 0

    // Separate channel from schedule reminders, per the recurring-reminder
    // feature's requirement of being managed entirely independently — the
    // user can mute/configure one without affecting the other.
    private const val RECURRING_CHANNEL_ID = "recurring_reminders"
    private const val RECURRING_NOTIFICATION_ID_BASE = 100_000

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.KOREAN)

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "일정 알림",
                NotificationManager.IMPORTANCE_HIGH,
            )
            manager.createNotificationChannel(channel)
        }
    }

    fun ensureRecurringChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(RECURRING_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                RECURRING_CHANNEL_ID,
                "반복 알림",
                NotificationManager.IMPORTANCE_HIGH,
            )
            manager.createNotificationChannel(channel)
        }
    }

    /** The per-schedule reminder — only used for schedules that carry a specific time. */
    fun show(context: Context, schedule: Schedule) {
        if (!ensureChannelAndPermission(context)) return

        val contentText = schedule.time?.let { "오늘 ${it.format(timeFormatter)} 일정" } ?: "오늘 일정"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(schedule.title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent(context, schedule.id.toInt()))
            .addAction(R.drawable.ic_notification, "완료", completePendingIntent(context, schedule.id))
            .build()

        NotificationManagerCompat.from(context).notify(schedule.id.toInt(), notification)
    }

    /** The once-a-day briefing summarizing every schedule registered for today. */
    fun showBriefing(context: Context, schedules: List<Schedule>) {
        if (schedules.isEmpty()) return
        if (!ensureChannelAndPermission(context)) return

        val inboxStyle = NotificationCompat.InboxStyle()
        schedules.forEach { schedule ->
            val line = schedule.time?.let { "${schedule.title} · ${it.format(timeFormatter)}" } ?: schedule.title
            inboxStyle.addLine(line)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("오늘 일정 ${schedules.size}개")
            .setContentText(schedules.joinToString(", ") { it.title })
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent(context, BRIEFING_NOTIFICATION_ID))
            .build()

        NotificationManagerCompat.from(context).notify(BRIEFING_NOTIFICATION_ID, notification)
    }

    /** A weekly repeating reminder firing — entirely separate from schedule notifications. */
    fun showRecurringReminder(context: Context, reminder: RecurringReminder) {
        if (!hasNotificationPermission(context)) return
        ensureRecurringChannel(context)

        val notificationId = RECURRING_NOTIFICATION_ID_BASE + reminder.id.toInt()
        val notification = NotificationCompat.Builder(context, RECURRING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(reminder.title)
            .setContentText("반복 알림")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent(context, notificationId))
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun ensureChannelAndPermission(context: Context): Boolean {
        ensureChannel(context)
        return hasNotificationPermission(context)
    }

    private fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun completePendingIntent(context: Context, scheduleId: Long): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_COMPLETE
            putExtra(NotificationActionReceiver.EXTRA_SCHEDULE_ID, scheduleId)
        }
        return PendingIntent.getBroadcast(
            context,
            scheduleId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun openAppPendingIntent(context: Context, requestCode: Int): PendingIntent {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
