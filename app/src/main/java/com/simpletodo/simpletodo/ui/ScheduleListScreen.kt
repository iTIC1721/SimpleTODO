package com.simpletodo.simpletodo.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.simpletodo.simpletodo.data.RecurringReminder
import com.simpletodo.simpletodo.data.Schedule
import com.simpletodo.simpletodo.ui.theme.mutedText
import java.time.LocalDate

private val tabs = listOf("현재 일정", "완료한 일정", "반복 알림")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleListScreen(
    viewModel: ScheduleViewModel,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
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

    val today = LocalDate.now()
    val todayCount = currentSchedules.count { it.date == today }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = if (selectedTab == 2) onAddReminderClick else onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 2.dp),
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = if (selectedTab == 2) "반복 알림 추가" else "일정 추가",
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            TodayHeader(date = today, todayCount = todayCount, onSettingsClick = onSettingsClick)
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                indicator = {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(selectedTab),
                        height = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) },
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { onTabSelected(index) },
                        selectedContentColor = MaterialTheme.colorScheme.onSurface,
                        unselectedContentColor = MaterialTheme.mutedText,
                        text = { Text(title, style = MaterialTheme.typography.titleSmall) },
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

/**
 * The page title is today's date, like a desk calendar page, with how many
 * schedules are left for today underneath.
 */
@Composable
private fun TodayHeader(date: LocalDate, todayCount: Int, onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = PageGutter, end = PageGutter - 12.dp, top = 20.dp, bottom = 16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(date.toHeaderDateString(), style = MaterialTheme.typography.displaySmall)
            Text(
                text = if (todayCount == 0) "${date.toWeekdayString()} · 오늘 일정 없음" else "${date.toWeekdayString()} · 오늘 일정 ${todayCount}개",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Outlined.Settings, contentDescription = "설정", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
