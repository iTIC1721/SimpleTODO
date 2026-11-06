package com.simpletodo.simpletodo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simpletodo.simpletodo.data.AppDatabase
import com.simpletodo.simpletodo.data.RecurringReminder
import com.simpletodo.simpletodo.data.RecurringReminderDao
import com.simpletodo.simpletodo.data.Schedule
import com.simpletodo.simpletodo.data.ScheduleDao
import com.simpletodo.simpletodo.data.daysOfWeekToMask
import com.simpletodo.simpletodo.notification.AlarmScheduler
import com.simpletodo.simpletodo.notification.RecurringReminderScheduler
import com.simpletodo.simpletodo.widget.ScheduleWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

// App-lifetime scope for schedule writes and widget refreshes. Screens opened
// from the widget (its "+" button or a row) finish the Activity right after
// saving, which clears the ViewModel and would cancel work in viewModelScope
// before the database write or the widget refresh completes.
private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val dao: ScheduleDao = AppDatabase.getInstance(application).scheduleDao()
    private val reminderDao: RecurringReminderDao = AppDatabase.getInstance(application).recurringReminderDao()

    init {
        refreshWidget()
        AlarmScheduler.scheduleDailyBriefing(getApplication())
    }

    val currentSchedules: StateFlow<List<Schedule>> =
        dao.getCurrentSchedules()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _searchKeyword = MutableStateFlow("")
    val searchKeyword: StateFlow<String> = _searchKeyword.asStateFlow()

    private val _searchStartDate = MutableStateFlow<LocalDate?>(null)
    val searchStartDate: StateFlow<LocalDate?> = _searchStartDate.asStateFlow()

    private val _searchEndDate = MutableStateFlow<LocalDate?>(null)
    val searchEndDate: StateFlow<LocalDate?> = _searchEndDate.asStateFlow()

    val pastSchedules: StateFlow<List<Schedule>> =
        combine(_searchKeyword, _searchStartDate, _searchEndDate) { keyword, start, end ->
            Triple(keyword, start, end)
        }.flatMapLatest { (keyword, start, end) ->
            dao.getPastSchedules(
                keyword = keyword.trim().ifBlank { null },
                startDate = start,
                endDate = end,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateSearchKeyword(keyword: String) {
        _searchKeyword.value = keyword
    }

    fun updateSearchDateRange(start: LocalDate?, end: LocalDate?) {
        _searchStartDate.value = start
        _searchEndDate.value = end
    }

    fun clearSearch() {
        _searchKeyword.value = ""
        _searchStartDate.value = null
        _searchEndDate.value = null
    }

    fun addSchedule(title: String, date: LocalDate, time: LocalTime?, isImportant: Boolean) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        appScope.launch {
            val schedule = Schedule(title = trimmed, date = date, time = time, isImportant = isImportant)
            val id = dao.insert(schedule)
            AlarmScheduler.schedule(getApplication(), schedule.copy(id = id))
            refreshWidget()
        }
    }

    fun updateSchedule(original: Schedule, title: String, date: LocalDate, time: LocalTime?, isImportant: Boolean) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        appScope.launch {
            val updated = original.copy(title = trimmed, date = date, time = time, isImportant = isImportant)
            dao.update(updated)
            AlarmScheduler.cancel(getApplication(), original.id)
            AlarmScheduler.schedule(getApplication(), updated)
            refreshWidget()
        }
    }

    suspend fun findSchedule(id: Long): Schedule? = dao.getScheduleById(id)

    fun completeSchedule(schedule: Schedule) {
        viewModelScope.launch {
            dao.markCompleted(schedule.id, Instant.now())
            AlarmScheduler.cancel(getApplication(), schedule.id)
            refreshWidget()
        }
    }

    fun restoreSchedule(schedule: Schedule) {
        viewModelScope.launch {
            dao.markIncomplete(schedule.id)
            AlarmScheduler.schedule(getApplication(), schedule.copy(isCompleted = false, completedAt = null))
            refreshWidget()
        }
    }

    fun deleteSchedule(schedule: Schedule) {
        viewModelScope.launch {
            dao.delete(schedule)
        }
    }

    val recurringReminders: StateFlow<List<RecurringReminder>> =
        reminderDao.getAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addReminder(title: String, days: Set<DayOfWeek>, time: LocalTime?) {
        val trimmed = title.trim()
        if (trimmed.isEmpty() || days.isEmpty()) return
        viewModelScope.launch {
            val reminder = RecurringReminder(title = trimmed, daysOfWeekMask = daysOfWeekToMask(days), time = time)
            val id = reminderDao.insert(reminder)
            RecurringReminderScheduler.schedule(getApplication(), reminder.copy(id = id))
        }
    }

    fun updateReminder(original: RecurringReminder, title: String, days: Set<DayOfWeek>, time: LocalTime?) {
        val trimmed = title.trim()
        if (trimmed.isEmpty() || days.isEmpty()) return
        viewModelScope.launch {
            val updated = original.copy(title = trimmed, daysOfWeekMask = daysOfWeekToMask(days), time = time)
            reminderDao.update(updated)
            RecurringReminderScheduler.schedule(getApplication(), updated)
        }
    }

    fun deleteReminder(reminder: RecurringReminder) {
        viewModelScope.launch {
            reminderDao.delete(reminder)
            RecurringReminderScheduler.cancel(getApplication(), reminder.id)
        }
    }

    fun setReminderEnabled(reminder: RecurringReminder, enabled: Boolean) {
        viewModelScope.launch {
            val updated = reminder.copy(isEnabled = enabled)
            reminderDao.update(updated)
            if (enabled) {
                RecurringReminderScheduler.schedule(getApplication(), updated)
            } else {
                RecurringReminderScheduler.cancel(getApplication(), reminder.id)
            }
        }
    }

    private fun refreshWidget() {
        val context = getApplication<Application>()
        appScope.launch {
            ScheduleWidgetUpdater.updateAll(context)
        }
    }
}
