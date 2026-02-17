package dev.mudrock.gunes.ui.screens

import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.mudrock.gunes.R
import dev.mudrock.gunes.data.Alarm
import dev.mudrock.gunes.ui.MainViewModel
import java.util.Calendar
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditorScreen(
    alarm: Alarm?,
    viewModel: MainViewModel,
    onSave: (Alarm) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val location by viewModel.location.collectAsState()
    val sunTimes by viewModel.sunTimes.collectAsState()

    // Form fields
    var time by remember { mutableStateOf(alarm?.time ?: "07:00") }
    var label by remember { mutableStateOf(alarm?.label ?: "") }
    var useSunTime by remember { mutableStateOf(alarm?.useSunTime ?: false) }
    var sunOffset by remember { mutableIntStateOf(alarm?.sunOffset ?: 0) }
    var sunOffsetHours by remember { mutableIntStateOf(0) }
    var sunOffsetMinutes by remember { mutableIntStateOf(0) }
    var repeatDays by remember { mutableStateOf(parseRepeatDays(alarm?.repeatDays)) }
    var deleteAfterUse by remember { mutableStateOf(alarm?.deleteAfterUse ?: false) }
    var soundUri by remember { mutableStateOf(alarm?.soundUri ?: "") }
    var soundName by remember { mutableStateOf(alarm?.soundName ?: "Default") }
    var vibrate by remember { mutableStateOf(alarm?.vibrate ?: true) }
    var volume by remember { mutableIntStateOf(alarm?.volume ?: 50) }
    var gradualVolume by remember { mutableStateOf(alarm?.gradualVolume ?: false) }
    var gradualDuration by remember { mutableIntStateOf(alarm?.gradualDuration ?: 30) }
    var showSoundPicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Initialize sun offset hours/minutes from total offset
    LaunchedEffect(alarm) {
        if (alarm != null) {
            sunOffset = alarm.sunOffset
            val absOffset = kotlin.math.abs(alarm.sunOffset)
            sunOffsetHours = (absOffset / 60) * if (alarm.sunOffset < 0) -1 else 1
            sunOffsetMinutes = absOffset % 60
            repeatDays = parseRepeatDays(alarm.repeatDays)
        }
    }

    // Update sun offset when hours/minutes change
    LaunchedEffect(sunOffsetHours, sunOffsetMinutes) {
        sunOffset = if (sunOffsetHours < 0) {
            sunOffsetHours * 60 - sunOffsetMinutes
        } else {
            sunOffsetHours * 60 + sunOffsetMinutes
        }
    }

    // File picker launcher for custom sounds
    val audioPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            soundUri = it.toString()
            soundName = it.lastPathSegment ?: "Custom"
            showSoundPicker = false
        }
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (alarm == null) stringResource(R.string.add_alarm) else stringResource(R.string.edit_alarm),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Label
                FormSection(title = stringResource(R.string.label)) {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        placeholder = { Text(stringResource(R.string.label)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }

                // Clock Mode Toggle
                FormSection(title = stringResource(R.string.default_clock_mode)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ModeButton(
                            label = stringResource(R.string.normal_clock),
                            icon = Icons.Default.Schedule,
                            selected = !useSunTime,
                            onClick = { useSunTime = false },
                            modifier = Modifier.weight(1f)
                        )
                        ModeButton(
                            label = stringResource(R.string.sun_based_clock),
                            icon = Icons.Default.WbSunny,
                            selected = useSunTime,
                            onClick = { useSunTime = true },
                            enabled = location != null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Time Input
                if (useSunTime) {
                    FormSection(
                        title = stringResource(R.string.sun_offset)
                    ) {
                        // Preview of actual time
                        val sunriseTime = sunTimes.firstOrNull()?.sunrise
                        val previewTime = if (sunriseTime != null && location != null) {
                            val calendar = Calendar.getInstance()
                            calendar.time = sunriseTime
                            calendar.add(Calendar.MINUTE, sunOffset)
                            val hour = calendar.get(Calendar.HOUR_OF_DAY)
                            val minute = calendar.get(Calendar.MINUTE)
                            String.format("%02d:%02d", hour, minute)
                        } else "--:--"

                        Text(
                            text = previewTime,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Hours and Minutes Pickers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Hours picker
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.hours),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                NumberWheelPicker(
                                    value = sunOffsetHours,
                                    range = -12..12,
                                    onValueChange = { sunOffsetHours = it },
                                    displayFormatter = { if (it >= 0) "+$it" else "$it" }
                                )
                            }

                            Text(
                                text = ":",
                                style = MaterialTheme.typography.headlineLarge,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            // Minutes picker
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.minutes),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                NumberWheelPicker(
                                    value = sunOffsetMinutes,
                                    range = 0..59,
                                    onValueChange = { sunOffsetMinutes = it },
                                    displayFormatter = { String.format("%02d", it) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (sunOffset >= 0) {
                                "$sunOffset ${stringResource(R.string.minutes_after_sunrise)}"
                            } else {
                                "${kotlin.math.abs(sunOffset)} ${stringResource(R.string.minutes_before_sunrise)}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    FormSection(title = stringResource(R.string.time)) {
                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = time,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        if (showTimePicker) {
                            val timeParts = time.split(":")
                            val initialHour = timeParts.getOrNull(0)?.toIntOrNull() ?: 7
                            val initialMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

                            TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    time = String.format("%02d:%02d", hourOfDay, minute)
                                    showTimePicker = false
                                },
                                initialHour,
                                initialMinute,
                                settings.use24HourFormat
                            ).show()
                            showTimePicker = false
                        }
                    }
                }

                // Repeat Days
                FormSection(title = stringResource(R.string.repeat)) {
                    // Quick select buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickButton(
                            label = stringResource(R.string.weekdays),
                            onClick = { repeatDays = setOf(1, 2, 3, 4, 5) },
                            modifier = Modifier.weight(1f)
                        )
                        QuickButton(
                            label = stringResource(R.string.weekends),
                            onClick = { repeatDays = setOf(0, 6) },
                            modifier = Modifier.weight(1f)
                        )
                        QuickButton(
                            label = stringResource(R.string.daily),
                            onClick = { repeatDays = setOf(0, 1, 2, 3, 4, 5, 6) },
                            modifier = Modifier.weight(1f)
                        )
                        QuickButton(
                            label = stringResource(R.string.once),
                            onClick = { repeatDays = emptySet() },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Day buttons
                    val dayNames = listOf(
                        R.string.sunday to 0,
                        R.string.monday to 1,
                        R.string.tuesday to 2,
                        R.string.wednesday to 3,
                        R.string.thursday to 4,
                        R.string.friday to 5,
                        R.string.saturday to 6
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        dayNames.forEach { (nameRes, dayValue) ->
                            val isSelected = dayValue in repeatDays
                            DayButton(
                                label = stringResource(nameRes),
                                selected = isSelected,
                                onClick = {
                                    repeatDays = if (isSelected) {
                                        repeatDays - dayValue
                                    } else {
                                        repeatDays + dayValue
                                    }
                                }
                            )
                        }
                    }
                }

                // Alarm Sound
                FormSection(title = stringResource(R.string.alarm_sound)) {
                    OutlinedButton(
                        onClick = { showSoundPicker = !showSoundPicker },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(soundName)
                    }

                    if (showSoundPicker) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                val sounds = listOf(
                                    "Default" to "",
                                    "Gentle" to "gentle.mp3",
                                    "Loud" to "loud.mp3",
                                    "Nature" to "nature.mp3",
                                    "Digital" to "digital.mp3"
                                )

                                sounds.forEach { (name, uri) ->
                                    TextButton(
                                        onClick = {
                                            soundName = name
                                            soundUri = uri
                                            showSoundPicker = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Notifications,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(name)
                                        }
                                    }
                                }

                                Button(
                                    onClick = { audioPickerLauncher.launch("audio/*") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(Icons.Default.Folder, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.browse_files))
                                }
                            }
                        }
                    }

                    if (soundUri.isNotEmpty() && (soundUri.startsWith("file://") || soundUri.startsWith("content://"))) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Custom sounds from storage will be played by the app, not by the system notification.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Volume
                FormSection(title = "${stringResource(R.string.volume)}: ${volume}%") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VolumeMute, contentDescription = null)
                        Slider(
                            value = volume.toFloat(),
                            onValueChange = { volume = it.toInt() },
                            valueRange = 0f..100f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Icon(Icons.Default.VolumeUp, contentDescription = null)
                    }
                }

                // Vibrate Toggle
                FormSection(title = null) {
                    ToggleRow(
                        label = stringResource(R.string.vibrate),
                        icon = Icons.Default.Vibration,
                        checked = vibrate,
                        onCheckedChange = { vibrate = it }
                    )
                }

                // Gradual Volume Toggle
                FormSection(title = null) {
                    ToggleRow(
                        label = stringResource(R.string.gradual_volume),
                        icon = Icons.Default.TrendingUp,
                        checked = gradualVolume,
                        onCheckedChange = { gradualVolume = it }
                    )
                }

                // Gradual Duration (only if gradual volume is enabled)
                if (gradualVolume) {
                    FormSection(
                        title = "${stringResource(R.string.gradual_duration)}: ${gradualDuration} ${stringResource(R.string.seconds)}"
                    ) {
                        Slider(
                            value = gradualDuration.toFloat(),
                            onValueChange = { gradualDuration = it.toInt() },
                            valueRange = 10f..120f,
                            steps = 21,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                // Delete After Use Toggle
                FormSection(title = null) {
                    ToggleRow(
                        label = stringResource(R.string.delete_after_use),
                        icon = Icons.Default.Delete,
                        checked = deleteAfterUse,
                        onCheckedChange = { deleteAfterUse = it },
                        iconTint = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = {
                            val newAlarm = alarm?.copy(
                                time = time,
                                label = label,
                                useSunTime = useSunTime,
                                sunOffset = sunOffset,
                                repeatDays = repeatDays.joinToString(","),
                                deleteAfterUse = deleteAfterUse,
                                soundUri = soundUri,
                                soundName = soundName,
                                vibrate = vibrate,
                                volume = volume,
                                gradualVolume = gradualVolume,
                                gradualDuration = gradualDuration
                            ) ?: Alarm(
                                id = UUID.randomUUID().toString(),
                                time = time,
                                enabled = true,
                                label = label,
                                useSunTime = useSunTime,
                                sunOffset = sunOffset,
                                repeatDays = repeatDays.joinToString(","),
                                deleteAfterUse = deleteAfterUse,
                                soundUri = soundUri,
                                soundName = soundName,
                                vibrate = vibrate,
                                volume = volume,
                                gradualVolume = gradualVolume,
                                gradualDuration = gradualDuration
                            )
                            onSave(newAlarm)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun FormSection(
    title: String?,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        content()
    }
}

@Composable
fun ModeButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        selected -> Color.Black
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor
            )
        }
    }
}

@Composable
fun QuickButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun DayButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = 2.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ToggleRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    iconTint: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
fun NumberWheelPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    displayFormatter: (Int) -> String = { it.toString() }
) {
    val listState = rememberLazyListState()
    val items = range.toList()
    val initialIndex = items.indexOf(value).coerceAtLeast(0)

    LaunchedEffect(initialIndex) {
        listState.scrollToItem(initialIndex)
    }

    Box(
        modifier = Modifier
            .width(70.dp)
            .height(120.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            )
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Center highlight
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .align(Alignment.Center)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
        ) {
            items(items.size) { index ->
                val item = items[index]
                val isSelected = item == value

                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth()
                        .clickable { onValueChange(item) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayFormatter(item),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }

    // Update value when scrolling stops
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centerItem = listState.firstVisibleItemIndex
            val item = items.getOrNull(centerItem)
            if (item != null && item != value) {
                onValueChange(item)
            }
        }
    }
}

private fun parseRepeatDays(repeatDaysString: String?): Set<Int> {
    if (repeatDaysString.isNullOrEmpty()) return emptySet()
    return repeatDaysString
        .split(",")
        .mapNotNull { it.trim().toIntOrNull() }
        .toSet()
}
