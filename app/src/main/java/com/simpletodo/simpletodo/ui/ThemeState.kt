package com.simpletodo.simpletodo.ui

import com.simpletodo.simpletodo.data.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the current theme override in memory so the app UI (Compose) and the
 * widget refresh path can both react to a change made in Settings without
 * needing to restart the Activity.
 */
object ThemeState {
    private val _mode = MutableStateFlow(ThemeMode.SYSTEM)
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun set(mode: ThemeMode) {
        _mode.value = mode
    }
}
