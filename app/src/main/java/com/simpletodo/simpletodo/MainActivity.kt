package com.simpletodo.simpletodo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simpletodo.simpletodo.data.RecurringReminder
import com.simpletodo.simpletodo.data.Schedule
import com.simpletodo.simpletodo.data.ThemeMode
import com.simpletodo.simpletodo.data.ThemePreferences
import com.simpletodo.simpletodo.notification.NotificationHelper
import com.simpletodo.simpletodo.ui.AddRecurringReminderScreen
import com.simpletodo.simpletodo.ui.AddScheduleScreen
import com.simpletodo.simpletodo.ui.AppInfoScreen
import com.simpletodo.simpletodo.ui.OpenSourceLicensesScreen
import com.simpletodo.simpletodo.ui.ScheduleListScreen
import com.simpletodo.simpletodo.ui.ScheduleViewModel
import com.simpletodo.simpletodo.ui.SettingsScreen
import com.simpletodo.simpletodo.ui.ThemeState
import com.simpletodo.simpletodo.ui.theme.SimpleTODOTheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NotificationHelper.ensureChannel(this)
        NotificationHelper.ensureRecurringChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        ThemeState.set(ThemePreferences(this).themeMode)

        val openAddScreen = intent?.getBooleanExtra(EXTRA_OPEN_ADD, false) ?: false
        setContent {
            val themeMode by ThemeState.mode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // enableEdgeToEdge() alone picks icon colors from the *system*
            // theme, so a dark app on a light phone got dark, unreadable
            // status bar icons. Follow the app's own theme instead.
            LaunchedEffect(darkTheme) {
                val style = if (darkTheme) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            SimpleTODOTheme(darkTheme = darkTheme) {
                ScheduleApp(
                    modifier = Modifier.fillMaxSize(),
                    startOnAddScreen = openAddScreen,
                    onExit = { finish() },
                )
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_ADD = "com.simpletodo.simpletodo.EXTRA_OPEN_ADD"
    }
}

private sealed interface Screen {
    data object List : Screen
    data object Add : Screen
    data object Settings : Screen
    data object AppInfo : Screen
    data object Licenses : Screen
    data class Edit(val schedule: Schedule) : Screen
    data object AddReminder : Screen
    data class EditReminder(val reminder: RecurringReminder) : Screen
}

@Composable
fun ScheduleApp(
    modifier: Modifier = Modifier,
    startOnAddScreen: Boolean = false,
    onExit: () -> Unit = {},
) {
    val viewModel: ScheduleViewModel = viewModel()
    var screen by remember { mutableStateOf<Screen>(if (startOnAddScreen) Screen.Add else Screen.List) }
    // Held here rather than inside the list screen, so returning from an
    // add/edit screen lands back on the tab the user came from.
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    // Add was reached directly from the widget's + button, not from the app's
    // own list screen — there's nothing to "go back" to inside the app, so
    // cancelling/saving should return to the home screen instead of surfacing
    // the app's main list.
    val exitInsteadOfList = startOnAddScreen

    when (val current = screen) {
        Screen.List -> ScheduleListScreen(
            viewModel = viewModel,
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            onAddClick = { screen = Screen.Add },
            onScheduleClick = { screen = Screen.Edit(it) },
            onSettingsClick = { screen = Screen.Settings },
            onAddReminderClick = { screen = Screen.AddReminder },
            onReminderClick = { screen = Screen.EditReminder(it) },
            modifier = modifier,
        )
        Screen.Add -> AddScheduleScreen(
            onSave = viewModel::addSchedule,
            onBack = { if (exitInsteadOfList) onExit() else screen = Screen.List },
            modifier = modifier,
        )
        is Screen.Edit -> AddScheduleScreen(
            initialSchedule = current.schedule,
            onSave = { title, date, time, isImportant ->
                viewModel.updateSchedule(current.schedule, title, date, time, isImportant)
            },
            onBack = { screen = Screen.List },
            modifier = modifier,
        )
        Screen.Settings -> SettingsScreen(
            onBack = { screen = Screen.List },
            onOpenAppInfo = { screen = Screen.AppInfo },
            modifier = modifier,
        )
        Screen.AppInfo -> AppInfoScreen(
            onBack = { screen = Screen.Settings },
            onOpenLicenses = { screen = Screen.Licenses },
            modifier = modifier,
        )
        Screen.Licenses -> OpenSourceLicensesScreen(
            onBack = { screen = Screen.AppInfo },
            modifier = modifier,
        )
        Screen.AddReminder -> AddRecurringReminderScreen(
            onSave = viewModel::addReminder,
            onBack = { screen = Screen.List },
            modifier = modifier,
        )
        is Screen.EditReminder -> AddRecurringReminderScreen(
            initialReminder = current.reminder,
            onSave = { title, days, time ->
                viewModel.updateReminder(current.reminder, title, days, time)
            },
            onBack = { screen = Screen.List },
            modifier = modifier,
        )
    }
}
