package com.simpletodo.simpletodo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.simpletodo.simpletodo.data.AlarmPreferences
import com.simpletodo.simpletodo.data.RecurringReminder
import com.simpletodo.simpletodo.data.daysOfWeek
import com.simpletodo.simpletodo.ui.theme.mutedText

@Composable
fun RecurringReminderTab(
    reminders: List<RecurringReminder>,
    onToggleEnabled: (RecurringReminder, Boolean) -> Unit,
    onEdit: (RecurringReminder) -> Unit,
    onDelete: (RecurringReminder) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (reminders.isEmpty()) {
        EmptyState(text = "반복 알림 없음", modifier = modifier.fillMaxSize())
        return
    }
    val context = LocalContext.current
    // Reminders without their own time fire at the briefing time — show the
    // actual time instead of a vague "브리핑 시각".
    val briefingTime = remember { AlarmPreferences(context).defaultNotifyTime }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = ListBottomPadding),
    ) {
        itemsIndexed(reminders, key = { _, it -> it.id }) { index, reminder ->
            RecurringReminderRow(
                reminder = reminder,
                timeText = reminder.time?.toDisplayString() ?: "${briefingTime.toDisplayString()} 브리핑",
                onToggleEnabled = { onToggleEnabled(reminder, it) },
                onClick = { onEdit(reminder) },
                onDelete = { onDelete(reminder) },
            )
            if (index < reminders.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = PageGutter),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

@Composable
private fun RecurringReminderRow(
    reminder: RecurringReminder,
    timeText: String,
    onToggleEnabled: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .padding(start = PageGutter, end = PageGutter - 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp),
        ) {
            Text(
                text = reminder.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (reminder.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.mutedText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${reminder.daysOfWeek().toDisplayString()} · $timeText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.mutedText,
            )
        }
        Switch(
            checked = reminder.isEnabled,
            onCheckedChange = onToggleEnabled,
            colors = appSwitchColors(),
            modifier = Modifier.padding(start = 12.dp),
        )
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "삭제",
                tint = MaterialTheme.mutedText,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
