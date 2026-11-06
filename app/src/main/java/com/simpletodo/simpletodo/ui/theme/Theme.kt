package com.simpletodo.simpletodo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Material's container/tint roles are all pointed at the neutral ramp so no
// component falls back to the default lavender tints. Dynamic (wallpaper)
// color stays off: the app keeps its own violet accent.
private val LightColorScheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = Color.White,
    primaryContainer = LightRaised,
    onPrimaryContainer = LightTextPrimary,
    secondary = LightTextSecondary,
    onSecondary = Color.White,
    secondaryContainer = LightRaised,
    onSecondaryContainer = LightTextPrimary,
    tertiary = LightTextSecondary,
    onTertiary = Color.White,
    tertiaryContainer = LightRaised,
    onTertiaryContainer = LightTextPrimary,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightBackground,
    onSurface = LightTextPrimary,
    surfaceVariant = LightRaised,
    onSurfaceVariant = LightTextSecondary,
    surfaceTint = Color.Transparent,
    surfaceBright = LightSurface,
    surfaceDim = LightRaised,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightSurface,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightSurface,
    surfaceContainerHighest = LightRaised,
    outline = LightTextMuted,
    outlineVariant = LightHairline,
    error = LightError,
    onError = Color.White,
    inverseSurface = LightTextPrimary,
    inverseOnSurface = LightBackground,
    inversePrimary = DarkAccent,
    scrim = Color.Black,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkAccent,
    onPrimary = DarkBackground,
    primaryContainer = DarkRaised,
    onPrimaryContainer = DarkTextPrimary,
    secondary = DarkTextSecondary,
    onSecondary = DarkBackground,
    secondaryContainer = DarkRaised,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = DarkTextSecondary,
    onTertiary = DarkBackground,
    tertiaryContainer = DarkRaised,
    onTertiaryContainer = DarkTextPrimary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkBackground,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkRaised,
    onSurfaceVariant = DarkTextSecondary,
    surfaceTint = Color.Transparent,
    surfaceBright = DarkRaised,
    surfaceDim = DarkBackground,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurface,
    surfaceContainerHighest = DarkRaised,
    outline = DarkTextMuted,
    outlineVariant = DarkHairline,
    error = DarkError,
    onError = DarkBackground,
    inverseSurface = DarkTextPrimary,
    inverseOnSurface = DarkBackground,
    inversePrimary = LightAccent,
    scrim = Color.Black,
)

// Radius by size instead of pills everywhere: controls 8–10, cards and the
// FAB 12–16, dialogs and sheets 20.
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

/** Shape for full-width and outlined buttons, which Material otherwise draws as pills. */
val ButtonShape = RoundedCornerShape(10.dp)

@Composable
fun SimpleTODOTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content,
    )
}

/** The neutral text role for meta and captions — quieter than onSurfaceVariant. */
val MaterialTheme.mutedText: Color
    @Composable get() = colorScheme.outline
