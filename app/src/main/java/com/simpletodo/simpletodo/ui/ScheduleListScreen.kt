package com.simpletodo.simpletodo.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.simpletodo.simpletodo.data.RecurringReminder
import com.simpletodo.simpletodo.data.Schedule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleListScreen(
    viewModel: ScheduleViewModel,
    onAddClick: () -> Unit,
    onScheduleClick: (Schedule) -> Unit,
    onSettingsClick: () -> Unit,
    onAddReminderClick: () -> Unit,
    onReminderClick: (RecurringReminder) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentSchedules by viewModel.currentSchedules.collectAsState()
    val pastSchedules by viewModel.pastSchedules.collectAsState()
    val keyword by viewModel.searchKeyword.collectAsState()
    val startDate by viewModel.searchStartDate.collectAsState()
    val endDate by viewModel.searchEndDate.collectAsState()
    val recurringReminders by viewModel.recurringReminders.collectAsState()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = remember { listOf("현재 일정", "완료한 일정", "반복 알림") }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    TextButton(onClick = onSettingsClick) { Text("설정") }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = if (selectedTab == 2) onAddReminderClick else onAddClick) {
                Text(text = "+", style = MaterialTheme.typography.headlineMedium)
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            SecondaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                    )
                }
            }
            when (selectedTab) {
                0 -> CurrentScheduleTab(
                    schedules = currentSchedules,
                    onComplete = viewModel::completeSchedule,
                    onEdit = onScheduleClick,
                    modifier = Modifier.weight(1f),
                )
                1 -> PastScheduleTab(
                    schedules = pastSchedules,
                    keyword = keyword,
                    startDate = startDate,
                    endDate = endDate,
                    onKeywordChange = viewModel::updateSearchKeyword,
                    onDateRangeChange = viewModel::updateSearchDateRange,
                    onClearSearch = viewModel::clearSearch,
                    onRestore = viewModel::restoreSchedule,
                    onDelete = viewModel::deleteSchedule,
                    modifier = Modifier.weight(1f),
                )
                else -> RecurringReminderTab(
                    reminders = recurringReminders,
                    onToggleEnabled = viewModel::setReminderEnabled,
                    onEdit = onReminderClick,
                    onDelete = viewModel::deleteReminder,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
