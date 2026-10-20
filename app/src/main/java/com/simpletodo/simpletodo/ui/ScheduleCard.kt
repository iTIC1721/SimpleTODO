package com.simpletodo.simpletodo.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.simpletodo.simpletodo.data.Schedule
import com.simpletodo.simpletodo.ui.theme.ImportantAccent
import com.simpletodo.simpletodo.ui.theme.SimpleTODOTheme
import java.time.LocalDate
import java.time.LocalTime

private fun Schedule.isOverdue(): Boolean {
    val today = LocalDate.now()
    return when {
        date.isBefore(today) -> true
        date.isEqual(today) && time != null -> time.isBefore(LocalTime.now())
        else -> false
    }
}

@Composable
fun CurrentScheduleCard(
    schedule: Schedule,
    onComplete: (Schedule) -> Unit,
    onEdit: (Schedule) -> Unit,
    modifier: Modifier = Modifier,
) {
    val overdue = schedule.isOverdue()
    Card(onClick = { onEdit(schedule) }, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = false, onCheckedChange = { onComplete(schedule) })
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (schedule.isImportant) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "중요",
                            tint = ImportantAccent,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = schedule.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (schedule.isImportant) ImportantAccent else MaterialTheme.colorScheme.onSurface,
                    )
                }
                val dateText = schedule.date.toDisplayString()
                val timeText = schedule.time?.toDisplayString()
                val subtitleColor = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                Text(
                    text = if (timeText != null) "$dateText $timeText" else dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtitleColor,
                )
                if (overdue) {
                    Text(
                        text = "기한 지남",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
fun PastScheduleCard(
    schedule: Schedule,
    onRestore: (Schedule) -> Unit,
    onDelete: (Schedule) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = true, onCheckedChange = { onRestore(schedule) })
            Column(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f),
            ) {
                Text(text = schedule.title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = schedule.date.toDisplayString(),
                    style = MaterialTheme.typography.bodyMedium,
                )
                schedule.completedAt?.let {
                    Text(
                        text = "완료: ${it.toDisplayString()}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            IconButton(onClick = { onDelete(schedule) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "삭제",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CurrentScheduleCardPreview() {
    SimpleTODOTheme {
        CurrentScheduleCard(
            schedule = Schedule(title = "치과 예약", date = LocalDate.now()),
            onComplete = {},
            onEdit = {},
        )
    }
}
