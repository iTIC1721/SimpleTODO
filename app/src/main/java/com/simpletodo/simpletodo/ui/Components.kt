package com.simpletodo.simpletodo.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.simpletodo.simpletodo.ui.theme.ButtonShape
import com.simpletodo.simpletodo.ui.theme.mutedText

/** Horizontal page gutter shared by every screen. */
val PageGutter = 20.dp

private val CheckboxShape = RoundedCornerShape(6.dp)

/**
 * The app's signature mark: a rounded square that fills with the accent
 * violet when checked, echoing the checkbox in the launcher icon. The widget
 * draws the same shape (res/drawable/widget_checkbox_*.xml).
 */
@Composable
fun TaskCheckbox(
    checked: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val fill by animateColorAsState(if (checked) accent else Color.Transparent, tween(150), label = "fill")
    val border by animateColorAsState(
        if (checked) accent else MaterialTheme.mutedText,
        tween(150),
        label = "border",
    )
    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(role = Role.Checkbox, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(fill, CheckboxShape)
                .border(1.5.dp, border, CheckboxShape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
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

/** Group label inside a list, like 오늘 or 내일: muted, closer to its own rows than to the group above. */
@Composable
fun SectionHeader(label: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.mutedText) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = PageGutter, end = PageGutter, top = 24.dp, bottom = 4.dp),
    )
}

@Composable
fun RowDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(start = RowTextStart),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.mutedText)
    }
}

/** Top bar for secondary screens: a back arrow and the screen name, with optional actions on the right. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTopBar(
    title: String,
    onBack: () -> Unit,
    backLabel: String = "뒤로",
    navigationIcon: ImageVector = Icons.AutoMirrored.Outlined.ArrowBack,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(navigationIcon, contentDescription = backLabel)
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

/**
 * A yes/no question. [destructive] colors the confirm button as an error,
 * for actions that can't be undone.
 */
@Composable
fun ConfirmDialog(
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    title: String? = null,
    dismissLabel: String = "취소",
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = title?.let { { Text(it, style = MaterialTheme.typography.titleLarge) } },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmLabel,
                    color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

/**
 * Shows "[message] · 실행 취소" and returns true if the user tapped 실행 취소.
 * A snackbar already on screen is dismissed first, so a quick second action
 * replaces the first one's offer instead of queueing behind it.
 */
suspend fun SnackbarHostState.showUndo(message: String): Boolean {
    currentSnackbarData?.dismiss()
    return showSnackbar(
        message = message,
        actionLabel = "실행 취소",
        withDismissAction = false,
        duration = SnackbarDuration.Short,
    ) == SnackbarResult.ActionPerformed
}

/** The one primary action on a form screen. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.mutedText,
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

/** A labelled form field group: small secondary label above its control. */
@Composable
fun FieldGroup(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

@Composable
fun appSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
    checkedTrackColor = MaterialTheme.colorScheme.primary,
    checkedBorderColor = MaterialTheme.colorScheme.primary,
    uncheckedThumbColor = MaterialTheme.mutedText,
    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
    uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant,
)

/** A full-width row with a label on the left and a switch on the right, tappable as a whole. */
@Composable
fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Switch) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null, colors = appSwitchColors())
    }
}

/** Choice chip: neutral outline when off, accent fill when it is the current selection. */
@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        leadingIcon = leadingIcon?.let { icon ->
            { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
            iconColor = MaterialTheme.mutedText,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = Color.Transparent,
        ),
        modifier = modifier,
    )
}
