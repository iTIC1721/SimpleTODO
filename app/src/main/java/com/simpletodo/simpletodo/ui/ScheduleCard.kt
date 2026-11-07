package com.simpletodo.simpletodo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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

/**
 * A completed schedule. Normally the checked box un-completes it and the
 * trash icon deletes it; in selection mode the whole row toggles a selection
 * mark instead, and a long press starts selection mode.
 */
@Composable
fun PastScheduleRow(
    schedule: Schedule,
    onRestore: (Schedule) -> Unit,
    onDelete: (Schedule) -> Unit,
    modifier: Modifier = Modifier,
    selecting: Boolean = false,
    selected: Boolean = false,
    onToggleSelected: () -> Unit = {},
    onLongPress: () -> Unit = {},
) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
            .combinedClickable(
                onClick = { if (selecting) onToggleSelected() },
                onLongClick = if (selecting) null else {
                    {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress()
                    }
                },
            )
            .padding(start = CheckboxStartPadding, end = PageGutter - 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selecting) {
            SelectionMark(selected = selected)
        } else {
            TaskCheckbox(checked = true, onClick = { onRestore(schedule) }, contentDescription = "완료 취소")
        }
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
            Text(
                text = schedule.completedLine(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.mutedText,
            )
        }
        if (selecting) {
            // Same width as the trash button, so text doesn't reflow on entering selection.
            Spacer(Modifier.width(48.dp))
        } else {
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
}

/**
 * "11월 7일 (토) 00:30 · 13:05 완료": the schedule's own date and time first,
 * then when it was completed — the date is left out when it's the same day.
 */
private fun Schedule.completedLine(): String {
    val scheduled = date.toDisplayString() + (time?.let { " ${it.toDisplayString()}" } ?: "")
    val completed = completedAt?.let { " · ${completedText(date, it)}" }.orEmpty()
    return scheduled + completed
}

/** Selection-mode mark: a circle, so it isn't mistaken for the square completion checkbox. */
@Composable
private fun SelectionMark(selected: Boolean) {
    val accent = MaterialTheme.colorScheme.primary
    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(if (selected) accent else Color.Transparent, CircleShape)
                .border(1.5.dp, if (selected) accent else MaterialTheme.mutedText, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
