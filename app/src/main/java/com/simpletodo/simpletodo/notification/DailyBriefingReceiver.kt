package com.simpletodo.simpletodo.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simpletodo.simpletodo.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Fires once a day at the configured briefing time, then re-chains itself for tomorrow. */
class DailyBriefingReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(context).scheduleDao()
                val todaySchedules = dao.getSchedulesForDate(LocalDate.now())
                NotificationHelper.showBriefing(context, todaySchedules)
                AlarmScheduler.scheduleDailyBriefing(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
