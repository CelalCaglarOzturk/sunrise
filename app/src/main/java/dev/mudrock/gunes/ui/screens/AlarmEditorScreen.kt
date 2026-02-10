package dev.mudrock.gunes.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.mudrock.gunes.data.Alarm
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditorScreen(
    alarm: Alarm?,
    onSave: (Alarm) -> Unit,
    onCancel: () -> Unit
) {
    var time by remember { mutableStateOf(alarm?.time ?: "08:00") }
    var label by remember { mutableStateOf(alarm?.label ?: "") }
    var useSunTime by remember { mutableStateOf(alarm?.useSunTime ?: false) }
    var sunOffset by remember { mutableIntStateOf(alarm?.sunOffset ?: 0) }

    // Simplification for brevity: Assuming time input is a text field for now or time picker
    // For proper UI, we'd use a TimePicker

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    if (alarm == null) "Add Alarm" else "Edit Alarm",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = useSunTime,
                        onCheckedChange = { useSunTime = it }
                    )
                    Text("Use Sun Time")
                }

                if (useSunTime) {
                    OutlinedTextField(
                        value = sunOffset.toString(),
                        onValueChange = { sunOffset = it.toIntOrNull() ?: 0 },
                        label = { Text("Offset (minutes)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Time (HH:mm)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCancel) {
                        Text("Cancel")
                    }
                    Button(onClick = {
                        val newAlarm = alarm?.copy(
                            time = time,
                            label = label,
                            useSunTime = useSunTime,
                            sunOffset = sunOffset
                        ) ?: Alarm(
                            id = UUID.randomUUID().toString(),
                            time = time,
                            enabled = true,
                            label = label,
                            useSunTime = useSunTime,
                            sunOffset = sunOffset,
                            repeatDays = "",
                            deleteAfterUse = false,
                            soundUri = "",
                            soundName = "Default",
                            vibrate = true,
                            volume = 100,
                            gradualVolume = false,
                            gradualDuration = 0
                        )
                        onSave(newAlarm)
                    }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
