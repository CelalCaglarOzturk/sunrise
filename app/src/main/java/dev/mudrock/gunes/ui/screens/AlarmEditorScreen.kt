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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.mudrock.gunes.R
import dev.mudrock.gunes.data.Alarm
import dev.mudrock.gunes.ui.MainViewModel
import kotlinx.coroutines.launch
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

    // Form fields - use current time as default for new alarms
    val currentTime = remember {
        val calendar = Calendar.getInstance()
        String.format(java.util.Locale.US, "%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
    }
    var time by remember { mutableStateOf(alarm?.time ?: currentTime) }
    var label by remember { mutableStateOf(alarm?.label ?: "") }
    var useSunTime by remember { mutableStateOf(alarm?.useSunTime ?: false) }
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

    // Calculate sunOffset from hours and minutes
    val sunOffset = remember(sunOffsetHours, sunOffsetMinutes) {
        if (sunOffsetHours < 0) {
            sunOffsetHours * 60 - sunOffsetMinutes
        } else {
            sunOffsetHours * 60 + sunOffsetMinutes
        }
    }

    // Initialize sun offset hours/minutes from total offset when alarm changes
    LaunchedEffect(alarm?.id) {
        if (alarm != null) {
            val absOffset = kotlin.math.abs(alarm.sunOffset)
            sunOffsetHours = (absOffset / 60) * if (alarm.sunOffset < 0) -1 else 1
            sunOffsetMinutes = absOffset % 60
            repeatDays = parseRepeatDays(alarm.repeatDays)
        } else {
            // Reset to defaults for new alarm
            sunOffsetHours = 0
            sunOffsetMinutes = 0
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
                            enabled = true,
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
                    // Quick select buttons - Order: once, daily, weekdays, weekends
                    val isOnce = repeatDays.isEmpty()
                    val isDaily = repeatDays.size == 7
                    val isWeekdays = repeatDays.size == 5 && repeatDays.containsAll(setOf(1, 2, 3, 4, 5))
                    val isWeekends = repeatDays.size == 2 && repeatDays.containsAll(setOf(0, 6))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RepeatOptionButton(
                            label = stringResource(R.string.once),
                            selected = isOnce,
                            onClick = { repeatDays = emptySet() },
                            modifier = Modifier.weight(1f)
                        )
                        RepeatOptionButton(
                            label = stringResource(R.string.daily),
                            selected = isDaily,
                            onClick = { repeatDays = setOf(0, 1, 2, 3, 4, 5, 6) },
                            modifier = Modifier.weight(1f)
                        )
                        RepeatOptionButton(
                            label = stringResource(R.string.weekdays),
                            selected = isWeekdays,
                            onClick = { repeatDays = setOf(1, 2, 3, 4, 5) },
                            modifier = Modifier.weight(1f)
                        )
                        RepeatOptionButton(
                            label = stringResource(R.string.weekends),
                            selected = isWeekends,
                            onClick = { repeatDays = setOf(0, 6) },
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
                                // Default option
                                TextButton(
                                    onClick = {
                                        soundName = "Default"
                                        soundUri = ""
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
                                        Text(stringResource(R.string.select_sound))
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
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        onClick = { deleteAfterUse = !deleteAfterUse }
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
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFFFF0000),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.delete_after_use),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color(0xFFFF0000)
                                )
                            }
                            Switch(
                                checked = deleteAfterUse,
                                onCheckedChange = { deleteAfterUse = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFFF0000),
                                    checkedTrackColor = Color(0xFFFF0000).copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
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
        modifier = modifier
            .height(64.dp),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun RepeatOptionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline

    Surface(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor
            )
        }
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
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
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
    val items = range.toList()
    val itemIndex = items.indexOf(value).coerceAtLeast(0)
    // Account for 2 padding items: actual item is at lazyColumn index (2 + itemIndex)
    // For it to be at center (position 1), firstVisibleIndex should be (2 + itemIndex) - 1 = itemIndex + 1
    val initialScrollIndex = itemIndex + 1
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialScrollIndex)
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    
    var selectedValue by remember { mutableIntStateOf(value) }
    
    LaunchedEffect(value) {
        if (value != selectedValue) {
            val index = items.indexOf(value).coerceAtLeast(0)
            listState.animateScrollToItem(index + 1)
        }
    }
    
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val scrollOffset = listState.firstVisibleItemScrollOffset
        val firstVisibleIndex = listState.firstVisibleItemIndex
        
        val centerPositionPx = with(density) { 60.dp.toPx() }
        val itemHeightPx = with(density) { 40.dp.toPx() }
        
        val firstItemTop = -scrollOffset
        val centerItemIndex = firstVisibleIndex + ((centerPositionPx - firstItemTop) / itemHeightPx).toInt()
        
        // Account for the 2 padding items at the top
        val actualItemIndex = centerItemIndex - 2
        
        val newValue = items.getOrNull(actualItemIndex)
        
        if (newValue != null && newValue != selectedValue) {
            selectedValue = newValue
            onValueChange(newValue)
        }
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
            items(2) {
                Box(modifier = Modifier.height(40.dp))
            }
            items(items.size) { index ->
                val item = items[index]
                val isSelected = item == selectedValue

                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth()
                        .clickable {
                            coroutineScope.launch {
                                listState.animateScrollToItem(index + 1)
                            }
                        },
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
            items(2) {
                Box(modifier = Modifier.height(40.dp))
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
