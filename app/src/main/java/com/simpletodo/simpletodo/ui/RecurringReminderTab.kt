package com.simpletodo.simpletodo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.simpletodo.simpletodo.data.RecurringReminder
import com.simpletodo.simpletodo.data.daysOfWeek

@Composable
fun RecurringReminderTab(
    reminders: List<RecurringReminder>,
    onToggleEnabled: (RecurringReminder, Boolean) -> Unit,
    onEdit: (RecurringReminder) -> Unit,
    onDelete: (RecurringReminder) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (reminders.isEmpty()) {
        EmptyState(text = "등록된 반복 알림이 없습니다", modifier = modifier.fillMaxSize())
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(reminders, key = { it.id }) { reminder ->
                RecurringReminderCard(
                    reminder = reminder,
                    onToggleEnabled = { onToggleEnabled(reminder, it) },
                    onClick = { onEdit(reminder) },
                    onDelete = { onDelete(reminder) },
                )
            }
        }
    }
}

@Composable
private fun RecurringReminderCard(
    reminder: RecurringReminder,
    onToggleEnabled: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = reminder.title, style = MaterialTheme.typography.bodyLarge)
                val timeText = reminder.time?.toDisplayString() ?: "브리핑 시각"
                Text(
                    text = "${reminder.daysOfWeek().toDisplayString()} · $timeText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = reminder.isEnabled, onCheckedChange = onToggleEnabled)
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "삭제",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
