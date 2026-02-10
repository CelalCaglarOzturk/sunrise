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
fun TimerScreen(viewModel: MainViewModel) {
    // Simple Timer Implementation
    var timeSeconds by remember { mutableIntStateOf(0) }
    var isRunning by remember { mutableStateOf(false) }
    var inputTime by remember { mutableStateOf("60") }

    LaunchedEffect(isRunning, timeSeconds) {
        if (isRunning && timeSeconds > 0) {
            delay(1000L)
            timeSeconds--
            if (timeSeconds == 0) {
                isRunning = false
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = formatTime(timeSeconds),
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (!isRunning && timeSeconds == 0) {
            OutlinedTextField(
                value = inputTime,
                onValueChange = { inputTime = it.filter { char -> char.isDigit() } },
                label = { Text("Seconds") }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                timeSeconds = inputTime.toIntOrNull() ?: 0
                isRunning = true
            }) {
                Text("Start")
            }
        } else {
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
                    timeSeconds = 0
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset")
                }
            }
        }
    }
}

fun formatTime(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format("%02d:%02d:%02d", h, m, s)
    } else {
        String.format("%02d:%02d", m, s)
    }
}
