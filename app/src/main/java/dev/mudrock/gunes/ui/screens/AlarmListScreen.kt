package dev.mudrock.gunes.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.mudrock.gunes.data.Alarm
import dev.mudrock.gunes.ui.MainViewModel

@Composable
fun AlarmListScreen(viewModel: MainViewModel) {
    val alarms by viewModel.alarms.collectAsState()
    var showEditor by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingAlarm = null
                showEditor = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Alarm")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(alarms) { alarm ->
                AlarmItem(
                    alarm = alarm,
                    onToggle = { viewModel.toggleAlarm(alarm) },
                    onEdit = {
                        editingAlarm = alarm
                        showEditor = true
                    },
                    onDelete = { viewModel.deleteAlarm(alarm) }
                )
            }
        }
    }

    if (showEditor) {
        AlarmEditorScreen(
            alarm = editingAlarm,
            onSave = { alarm ->
                if (editingAlarm == null) {
                    viewModel.addAlarm(alarm)
                } else {
                    viewModel.updateAlarm(alarm)
                }
                showEditor = false
            },
            onCancel = { showEditor = false }
        )
    }
}

@Composable
fun AlarmItem(
    alarm: Alarm,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onEdit() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (alarm.useSunTime) {
                    val offset = alarm.sunOffset
                    val text =
                        if (offset == 0) "Sunrise" else if (offset > 0) "+${offset}m after sunrise" else "${offset}m before sunrise"
                    Text(text, style = MaterialTheme.typography.headlineMedium)
                } else {
                    Text(alarm.time, style = MaterialTheme.typography.headlineLarge)
                }
                if (alarm.label.isNotEmpty()) {
                    Text(alarm.label, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
                Switch(checked = alarm.enabled, onCheckedChange = onToggle)
            }
        }
    }
}
