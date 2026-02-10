package dev.mudrock.gunes.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import dev.mudrock.gunes.ui.MainViewModel

@Composable
fun PomodoroScreen(viewModel: MainViewModel) {
    var timerType by remember { mutableStateOf("Work") } // Work, Short Break, Long Break
    var timeLeft by remember { mutableIntStateOf(25 * 60) }
    var isRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning, timeLeft) {
        if (isRunning && timeLeft > 0) {
            delay(1000L)
            timeLeft--
            if (timeLeft == 0) {
                isRunning = false
                // Play sound or notification here
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            FilterChip(
                selected = timerType == "Work",
                onClick = {
                    timerType = "Work"
                    timeLeft = 25 * 60
                    isRunning = false
                },
                label = { Text("Work") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = timerType == "Short Break",
                onClick = {
                    timerType = "Short Break"
                    timeLeft = 5 * 60
                    isRunning = false
                },
                label = { Text("Short Break") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = timerType == "Long Break",
                onClick = {
                    timerType = "Long Break"
                    timeLeft = 15 * 60
                    isRunning = false
                },
                label = { Text("Long Break") }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = formatTime(timeLeft),
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row {
            Button(onClick = { isRunning = !isRunning }) {
                Icon(
                    if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = {
                isRunning = false
                timeLeft = when (timerType) {
                    "Work" -> 25 * 60
                    "Short Break" -> 5 * 60
                    "Long Break" -> 15 * 60
                    else -> 25 * 60
                }
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset")
            }
        }
    }
}
