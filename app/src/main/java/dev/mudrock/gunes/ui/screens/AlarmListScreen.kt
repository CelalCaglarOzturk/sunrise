package dev.mudrock.gunes.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.mudrock.gunes.R
import dev.mudrock.gunes.data.Alarm
import dev.mudrock.gunes.ui.MainViewModel

import java.text.SimpleDateFormat

import java.util.Date

import java.util.Locale

import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class)

@Composable

fun AlarmListScreen(viewModel: MainViewModel) {

    val alarms by viewModel.alarms.collectAsState()

    val settings by viewModel.settings.collectAsState()

    val sunTimes by viewModel.sunTimes.collectAsState()


    var showEditor by remember { mutableStateOf(false) }

    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }

    var showMenu by remember { mutableStateOf(false) }



    Scaffold(

        topBar = {

            CenterAlignedTopAppBar(

                title = { Text(stringResource(R.string.alarms)) },

                actions = {

                    IconButton(onClick = { showMenu = true }) {

                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.bulk_actions))

                    }

                    DropdownMenu(

                        expanded = showMenu,

                        onDismissRequest = { showMenu = false }

                    ) {

                        DropdownMenuItem(

                            text = { Text(stringResource(R.string.disable_all)) },

                            onClick = {

                                viewModel.disableAllAlarms()

                                showMenu = false

                            }

                        )

                        DropdownMenuItem(

                            text = { Text(stringResource(R.string.enable_all)) },

                            onClick = {

                                viewModel.enableAllAlarms()

                                showMenu = false

                            }

                        )

                        DropdownMenuItem(

                            text = { Text(stringResource(R.string.delete_all)) },

                            onClick = {

                                viewModel.deleteAllAlarms()

                                showMenu = false

                            },

                            colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.error)

                        )

                    }

                }

            )

        },

        floatingActionButton = {

            FloatingActionButton(onClick = {

                editingAlarm = null

                showEditor = true

            }, containerColor = MaterialTheme.colorScheme.primary) {

                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_alarm))

            }

        }

    ) { padding ->

        LazyColumn(

            modifier = Modifier

                .fillMaxSize()

                .padding(padding)

        ) {

            item {

                DigitalClock(

                    settings = settings,

                    sunriseTime = sunTimes.firstOrNull()?.sunrise

                )

            }



            if (alarms.isEmpty()) {

                item {

                    Box(

                        modifier = Modifier.fillMaxWidth().padding(32.dp),

                        contentAlignment = Alignment.Center

                    ) {

                        Text(
                            stringResource(R.string.no_alarms_yet),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )

                    }

                }

            } else {

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

fun DigitalClock(settings: dev.mudrock.gunes.data.AppSettings, sunriseTime: Date?) {

    var currentTime by remember { mutableStateOf(Date()) }



    LaunchedEffect(Unit) {

        while (true) {

            currentTime = Date()

            delay(1000)

        }

    }


    val useSunBased = settings.defaultClockMode == "sun"



    Column(

        modifier = Modifier

            .fillMaxWidth()

            .padding(16.dp),

        horizontalAlignment = Alignment.CenterHorizontally

    ) {

        val timeString = if (useSunBased && sunriseTime != null) {

            val diffMillis = currentTime.time - sunriseTime.time

            val diffMinutes = diffMillis / 60000.0

            val isBefore = diffMinutes < 0

            val absMinutes = Math.abs(diffMinutes)

            val hours = (absMinutes / 60).toInt()

            val mins = (absMinutes % 60).toInt()

            val secs = ((absMinutes % 1) * 60).toInt()


            val sign = if (isBefore) "-" else "+"

            val format = if (settings.showSeconds) "%s%02d:%02d:%02d" else "%s%02d:%02d"

            if (settings.showSeconds) {

                String.format(Locale.getDefault(), format, sign, hours, mins, secs)

            } else {

                String.format(Locale.getDefault(), format, sign, hours, mins)

            }

        } else {

            val pattern = if (settings.use24HourFormat) {

                if (settings.showSeconds) "HH:mm:ss" else "HH:mm"

            } else {

                if (settings.showSeconds) "h:mm:ss a" else "h:mm a"

            }

            SimpleDateFormat(pattern, Locale.getDefault()).format(currentTime)

        }



        Text(

            text = timeString,

            style = MaterialTheme.typography.displayLarge,

            color = MaterialTheme.colorScheme.primary

        )


        val datePattern = "EEEE, MMMM d, yyyy"

        Text(

            text = SimpleDateFormat(datePattern, Locale.getDefault()).format(currentTime),

            style = MaterialTheme.typography.bodyLarge,

            color = MaterialTheme.colorScheme.secondary

        )



        if (useSunBased) {

            Text(

                text = stringResource(R.string.sun_based_clock),

                style = MaterialTheme.typography.labelMedium,

                color = MaterialTheme.colorScheme.tertiary

            )

        }

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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                        if (offset == 0) stringResource(R.string.sunrise) else if (offset > 0) "+${offset}m ${
                            stringResource(
                                R.string.minutes_after_sunrise
                            )
                        }" else "${offset}m ${stringResource(R.string.minutes_before_sunrise)}"
                    Text(
                        text,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        alarm.time,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (alarm.label.isNotEmpty()) {
                    Text(
                        alarm.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_alarm),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                Switch(checked = alarm.enabled, onCheckedChange = onToggle)
            }
        }
    }
}
