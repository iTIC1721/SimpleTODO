package com.simpletodo.simpletodo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.simpletodo.simpletodo.data.Schedule
import com.simpletodo.simpletodo.ui.theme.mutedText
import java.time.LocalDate
import java.time.LocalTime

// The checkbox's 48dp touch box is pulled in so its 22dp square sits on the
// page gutter; text then starts at a fixed column for every row.
internal val CheckboxStartPadding = PageGutter - 13.dp
internal val RowTextStart = CheckboxStartPadding + 48.dp

private fun Schedule.isOverdue(): Boolean {
    val today = LocalDate.now()
    return when {
        date.isBefore(today) -> true
        date.isEqual(today) && time != null -> time.isBefore(LocalTime.now())
        else -> false
    }
}

private fun Schedule.dateLine(): String {
    val dateText = date.toDisplayString()
    return time?.let { "$dateText · ${it.toDisplayString()}" } ?: dateText
}

@Composable
fun CurrentScheduleRow(
    schedule: Schedule,
    onComplete: (Schedule) -> Unit,
    onEdit: (Schedule) -> Unit,
    modifier: Modifier = Modifier,
) {
    val overdue = schedule.isOverdue()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable { onEdit(schedule) }
            .padding(start = CheckboxStartPadding, end = PageGutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TaskCheckbox(checked = false, onClick = { onComplete(schedule) }, contentDescription = "완료")
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp),
        ) {
            Text(
                text = schedule.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (schedule.isImportant) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = schedule.dateLine(),
                style = MaterialTheme.typography.bodySmall,
                color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.mutedText,
            )
        }
        if (schedule.isImportant) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "중요",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(18.dp),
            )
        } else {
            // Keeps the title column the same width whether or not a row is important.
            Spacer(Modifier.width(30.dp))
        }
    }
}

@Composable
fun PastScheduleRow(
    schedule: Schedule,
    onRestore: (Schedule) -> Unit,
    onDelete: (Schedule) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(start = CheckboxStartPadding, end = PageGutter - 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TaskCheckbox(checked = true, onClick = { onRestore(schedule) }, contentDescription = "완료 취소")
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp),
        ) {
            Text(
                text = schedule.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val completed = schedule.completedAt?.let { " · ${completedText(schedule.date, it)}" }.orEmpty()
            Text(
                text = schedule.date.toDisplayString() + completed,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.mutedText,
            )
        }
        IconButton(onClick = { onDelete(schedule) }) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "삭제",
                tint = MaterialTheme.mutedText,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
