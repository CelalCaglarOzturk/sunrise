package dev.mudrock.gunes.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.mudrock.gunes.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun SunTimesScreen(viewModel: MainViewModel) {
    val sunTimes by viewModel.sunTimes.collectAsState()
    val location by viewModel.location.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (location == null) {
            Text("Fetching location...", style = MaterialTheme.typography.bodyLarge)
        } else {
            LazyColumn {
                items(sunTimes) { times ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                            Text(dateFormat.format(times.date), style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Sunrise", style = MaterialTheme.typography.labelMedium)
                                    Text(timeFormat.format(times.sunrise), style = MaterialTheme.typography.bodyLarge)
                                }
                                Column {
                                    Text("Sunset", style = MaterialTheme.typography.labelMedium)
                                    Text(timeFormat.format(times.sunset), style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
