package dev.mudrock.gunes.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import dev.mudrock.gunes.R
import dev.mudrock.gunes.ui.screens.*

sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Alarms : Screen("alarms", "Alarms", Icons.Default.Alarm)
    object Timer : Screen("timer", "Timer", Icons.Default.Timer)
    object Stopwatch : Screen("stopwatch", "Stopwatch", Icons.Default.Timelapse)
    object Sun : Screen("sun", "Sun", Icons.Default.WbSunny)
    object Pomodoro : Screen("pomodoro", "Focus", Icons.Default.HourglassEmpty)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val screens = listOf(
        Screen.Alarms,
        Screen.Timer,
        Screen.Stopwatch,
        Screen.Sun,
        Screen.Pomodoro,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.label, style = MaterialTheme.typography.labelSmall) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Alarms.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Alarms.route) {
                AlarmListScreen(viewModel = viewModel)
            }
            composable(Screen.Timer.route) {
                TimerScreen()
            }
            composable(Screen.Stopwatch.route) {
                StopwatchScreen()
            }
            composable(Screen.Sun.route) {
                SunTimesScreen(viewModel = viewModel)
            }
            composable(Screen.Pomodoro.route) {
                PomodoroScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
