package com.simpletodo.simpletodo.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.simpletodo.simpletodo.data.RecurringReminder
import com.simpletodo.simpletodo.data.Schedule
import com.simpletodo.simpletodo.ui.theme.mutedText
import kotlinx.coroutines.launch
import java.time.LocalDate

// Completed schedules live on their own screen, reached from the bottom of
// the 일정 list — they are history, not a peer of the two lists you act on.
private val tabs = listOf("일정", "반복 알림")
private const val REMINDER_TAB = 1

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
    onCompletedClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentSchedules by viewModel.currentSchedules.collectAsState()
    val completedCount by viewModel.completedCount.collectAsState()
    val recurringReminders by viewModel.recurringReminders.collectAsState()

    val today = LocalDate.now()
    val todayCount = currentSchedules.count { it.date == today }
    // A tab index saved by an older build (which had three tabs) may be out of range.
    val tab = selectedTab.coerceIn(0, tabs.lastIndex)

    // Back from 반복 알림 goes to 일정 first; from 일정 it leaves the app.
    BackHandler(enabled = tab != 0) { onTabSelected(0) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // Completing removes the row from the list at once, so offer a way back.
    val completeWithUndo: (Schedule) -> Unit = { schedule ->
        viewModel.completeSchedule(schedule)
        scope.launch {
            if (snackbarHostState.showUndo("완료 처리되었습니다.")) viewModel.restoreSchedule(schedule)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = if (tab == REMINDER_TAB) onAddReminderClick else onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 2.dp),
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = if (tab == REMINDER_TAB) "반복 알림 추가" else "일정 추가",
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
                selectedTabIndex = tab,
                containerColor = MaterialTheme.colorScheme.background,
                indicator = {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tab),
                        height = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) },
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = tab == index,
                        onClick = { onTabSelected(index) },
                        selectedContentColor = MaterialTheme.colorScheme.onSurface,
                        unselectedContentColor = MaterialTheme.mutedText,
                        text = { Text(title, style = MaterialTheme.typography.titleSmall) },
                    )
                }
            }
            when (tab) {
                REMINDER_TAB -> RecurringReminderTab(
                    reminders = recurringReminders,
                    onToggleEnabled = viewModel::setReminderEnabled,
                    onEdit = onReminderClick,
                    modifier = Modifier.weight(1f),
                )
                else -> CurrentScheduleTab(
                    schedules = currentSchedules,
                    completedCount = completedCount,
                    onComplete = completeWithUndo,
                    onEdit = onScheduleClick,
                    onOpenCompleted = onCompletedClick,
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
