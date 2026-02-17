package dev.mudrock.gunes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val sortedAlarms = remember(alarms) {
        alarms.sortedWith(compareBy<Alarm> { !it.enabled }.thenBy { it.time })
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                },
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
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingAlarm = null
                    showEditor = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_alarm))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DigitalClock(
                    settings = settings,
                    sunriseTime = sunTimes.firstOrNull()?.sunrise
                )
            }

            if (sortedAlarms.isEmpty()) {
                item {
                    EmptyAlarmsState()
                }
            } else {
                items(sortedAlarms, key = { it.id }) { alarm ->
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
                
                item {
                    Spacer(modifier = Modifier.height(80.dp))
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
            .padding(vertical = 24.dp),
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
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary
        )

        val datePattern = "EEEE, MMMM d, yyyy"
        Text(
            text = SimpleDateFormat(datePattern, Locale.getDefault()).format(currentTime),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        if (useSunBased) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.sun_based_clock),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun EmptyAlarmsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Alarm,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No alarms yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Create your first alarm",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
    val cardAlpha = if (alarm.enabled) 1f else 0.5f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onEdit() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = alarm.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEdit() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (alarm.useSunTime) {
                        Icon(
                            Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = formatAlarmTime(alarm),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = cardAlpha)
                    )
                }

                if (alarm.label.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        alarm.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f * cardAlpha)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        formatRepeatDays(alarm),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f * cardAlpha)
                    )

                    if (alarm.deleteAfterUse) {
                        AlarmBadge(icon = Icons.Default.Delete, label = "Once")
                    }
                    if (alarm.vibrate) {
                        AlarmBadge(icon = Icons.Default.Vibration, label = null)
                    }
                    if (alarm.gradualVolume) {
                        AlarmBadge(icon = Icons.Default.TrendingUp, label = null)
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun AlarmBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String?) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            if (label != null) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

fun formatAlarmTime(alarm: Alarm): String {
    if (alarm.useSunTime) {
        val offsetAbs = Math.abs(alarm.sunOffset)
        val hours = offsetAbs / 60
        val minutes = offsetAbs % 60

        var text = ""
        if (hours > 0) text += "${hours}h "
        if (minutes > 0) text += "${minutes}m"

        return if (alarm.sunOffset >= 0) "+$text" else "-$text"
    }
    return alarm.time
}

fun formatRepeatDays(alarm: Alarm): String {
    if (alarm.repeatDays.isEmpty()) {
        return "Once"
    }
    
    val days = alarm.repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }
    
    if (days.isEmpty()) {
        return "Once"
    }
    if (days.size == 7) {
        return "Daily"
    }

    val weekdays = listOf(1, 2, 3, 4, 5)
    val weekends = listOf(0, 6)

    if (days.size == 5 && weekdays.all { it in days }) {
        return "Weekdays"
    }
    if (days.size == 2 && weekends.all { it in days }) {
        return "Weekends"
    }

    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    return days.sorted().map { dayNames[it] }.joinToString(", ")
}
