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
// Mirrors the app's graphite ramp and single violet accent (ui/theme/Color.kt).
data class WidgetColors(
    val text: Int,
    val muted: Int,
    val accent: Int,
    val error: Int,
)

private val LIGHT_COLORS = WidgetColors(
    text = 0xFF171717.toInt(),
    muted = 0xFF737373.toInt(),
    accent = 0xFF624DE6.toInt(),
    error = 0xFFDC2626.toInt(),
)

private val DARK_COLORS = WidgetColors(
    text = 0xFFEDEDED.toInt(),
    muted = 0xFF85858B.toInt(),
    accent = 0xFFA996FF.toInt(),
    error = 0xFFE5484D.toInt(),
)

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
