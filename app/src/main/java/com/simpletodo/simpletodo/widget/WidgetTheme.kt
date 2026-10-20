package com.simpletodo.simpletodo.widget

import android.content.Context
import android.content.res.Configuration
import com.simpletodo.simpletodo.data.ThemeMode
import com.simpletodo.simpletodo.data.ThemePreferences

/**
 * RemoteViews resolves @color references using the SYSTEM's current night
 * mode (via the values/values-night resource qualifier), which can't be
 * overridden by an in-app preference. To let the widget follow a user choice
 * independent of the system setting, colors are resolved to explicit ARGB
 * ints here and applied with setTextColor/setInt instead.
 */
data class WidgetColors(
    val background: Int,
    val text: Int,
    val subText: Int,
    val headerText: Int,
    val divider: Int,
)

private val LIGHT_COLORS = WidgetColors(
    background = 0xFFFFFFFF.toInt(),
    text = 0xFF1C1B1F.toInt(),
    subText = 0xFF49454F.toInt(),
    headerText = 0xFF6650A4.toInt(),
    divider = 0xFFE0E0E0.toInt(),
)

private val DARK_COLORS = WidgetColors(
    background = 0xFF1C1B1F.toInt(),
    text = 0xFFE6E1E5.toInt(),
    subText = 0xFFCAC4D0.toInt(),
    headerText = 0xFFD0BCFF.toInt(),
    divider = 0xFF3A3742.toInt(),
)

// Matches ui.theme.ImportantAccent — one tone, since it reads clearly on both
// a light and a dark widget background.
val WIDGET_IMPORTANT_ACCENT = 0xFFFFAB00.toInt()

fun resolveWidgetIsDark(context: Context): Boolean {
    return when (ThemePreferences(context).themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> {
            val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            uiMode == Configuration.UI_MODE_NIGHT_YES
        }
    }
}

fun widgetColorsFor(isDark: Boolean): WidgetColors = if (isDark) DARK_COLORS else LIGHT_COLORS
