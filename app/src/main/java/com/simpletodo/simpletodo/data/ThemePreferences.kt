package com.simpletodo.simpletodo.data

import android.content.Context
import androidx.core.content.edit

class ThemePreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() = prefs.getString(KEY_MODE, null)
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM
        set(value) = prefs.edit { putString(KEY_MODE, value.name) }

    companion object {
        private const val PREFS_NAME = "theme_settings"
        private const val KEY_MODE = "theme_mode"
    }
}
