package dev.mudrock.gunes.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import dev.mudrock.gunes.ui.MainViewModel

@Composable
fun StopwatchScreen() {
    var timeMillis by remember { mutableLongStateOf(0L) }
    var isRunning by remember { mutableStateOf(false) }
    var laps by remember { mutableStateOf(listOf<Long>()) }

    LaunchedEffect(isRunning) {
        val startTime = System.currentTimeMillis() - timeMillis
        while (isRunning) {
            timeMillis = System.currentTimeMillis() - startTime
            delay(50L) // Update every 50ms
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = formatMillis(timeMillis),
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(onClick = { isRunning = !isRunning }) {
                Icon(
                    if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = {
                if (isRunning) {
                    laps = laps + timeMillis
                } else {
                    timeMillis = 0
                    laps = emptyList()
                }
            }) {
                Icon(
                    if (isRunning) Icons.Default.Flag else Icons.Default.Refresh,
                    contentDescription = null
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            itemsIndexed(laps.reversed()) { index, lapTime ->
                val lapNum = laps.size - index
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Lap $lapNum")
                    Text(formatMillis(lapTime))
                }
                HorizontalDivider()
            }
        }
    }
}

fun formatMillis(millis: Long): String {
    val seconds = millis / 1000
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    val ms = (millis % 1000) / 10
    return String.format("%02d:%02d.%02d", m, s, ms)
}
