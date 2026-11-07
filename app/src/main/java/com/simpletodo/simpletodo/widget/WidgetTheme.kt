package com.simpletodo.simpletodo.widget

import android.content.Context
import com.simpletodo.simpletodo.data.ThemeMode
import com.simpletodo.simpletodo.data.ThemePreferences

/**
 * The widget's layouts take their colors from @color/widget_* resources,
 * which the home screen resolves with the SYSTEM's night mode — so under the
 * app's "시스템 기본" theme the widget follows a system switch immediately,
 * without the app running. A forced in-app light/dark theme can't be
 * expressed that way, so for those the colors below are applied in code
 * (setTextColor/setColorFilter), overriding the resources.
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

/** The colors to apply in code, or null under "시스템 기본" (the layouts' resources apply). */
fun forcedWidgetColors(themeMode: ThemeMode): WidgetColors? = when (themeMode) {
    ThemeMode.DARK -> DARK_COLORS
    ThemeMode.LIGHT -> LIGHT_COLORS
    ThemeMode.SYSTEM -> null
}

fun widgetThemeMode(context: Context): ThemeMode = ThemePreferences(context).themeMode
