package dev.mudrock.gunes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.WbTwilight
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
import android.Manifest
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import dev.mudrock.gunes.R
import dev.mudrock.gunes.data.Alarm
import dev.mudrock.gunes.ui.MainViewModel
import dev.mudrock.gunes.util.SunTimes
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AlarmListScreen(viewModel: MainViewModel) {
    val alarms by viewModel.alarms.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val sunTimes by viewModel.sunTimes.collectAsState()
    val location by viewModel.location.collectAsState()
    val context = LocalContext.current

    var showEditor by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    // Location permission state
    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    val sortedAlarms = remember(alarms) {
        alarms.sortedWith(compareBy<Alarm> { !it.enabled }.thenBy { it.time })
    }
    
    val locationError by viewModel.locationError.collectAsState()
    
    // Request location permission on first launch for sun-based features
    LaunchedEffect(Unit) {
        if (!locationPermissionState.status.isGranted) {
            // Auto-request permission for sun-based clock feature
            locationPermissionState.launchPermissionRequest()
        }
    }

    // Update location when permission is granted
    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
            viewModel.updateLocation()
        }
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
                                showMenu = false
                                showDeleteConfirm = true
                            },
                            colors = MenuDefaults.itemColors(textColor = Color(0xFFFF0000))
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
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
            }
            
            // Add Alarm Button
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        editingAlarm = null
                        showEditor = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.add_alarm),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Upcoming Sun Times Section
            item {
                Spacer(modifier = Modifier.height(24.dp))
                SunTimesSection(
                    sunTimes = sunTimes,
                    hasLocationPermission = locationPermissionState.status.isGranted,
                    locationError = locationError,
                    onRequestPermission = {
                        if (locationPermissionState.status.shouldShowRationale) {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        } else {
                            locationPermissionState.launchPermissionRequest()
                        }
                    },
                    onRetry = { viewModel.updateLocation() }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showEditor) {
        AlarmEditorScreen(
            alarm = editingAlarm,
            viewModel = viewModel,
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

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_all)) },
            text = { Text(stringResource(R.string.delete_all_confirmation)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllAlarms()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF0000)
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun DigitalClock(settings: dev.mudrock.gunes.data.AppSettings, sunriseTime: Date?) {
    var currentTime by remember { mutableStateOf(Date()) }

    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()) }
    val timeFormat24h = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val timeFormat12h = remember { SimpleDateFormat("h:mm:ss a", Locale.getDefault()) }
    val timeFormat24hNoSec = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeFormat12hNoSec = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

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
        if (useSunBased && sunriseTime != null) {
            val sunBasedInfo = calculateSunBasedTime(currentTime, sunriseTime, settings.showSeconds)
            
            Text(
                text = sunBasedInfo.timeString,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = dateFormat.format(currentTime),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.WbSunny,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.sun_based_clock),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            sunBasedInfo.dayInfo?.let { info ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = info,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            val timeFormat = if (settings.use24HourFormat) {
                if (settings.showSeconds) timeFormat24h else timeFormat24hNoSec
            } else {
                if (settings.showSeconds) timeFormat12h else timeFormat12hNoSec
            }
            val timeString = timeFormat.format(currentTime)

            Text(
                text = timeString,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = dateFormat.format(currentTime),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

data class SunBasedTimeInfo(
    val timeString: String,
    val dayInfo: String?
)

fun calculateSunBasedTime(currentTime: Date, sunriseTime: Date, showSeconds: Boolean): SunBasedTimeInfo {
    val diffMillis = currentTime.time - sunriseTime.time
    val millisInDay = 24 * 60 * 60 * 1000L
    
    val dayCount = (diffMillis / millisInDay).toInt()
    val remainderMillis = diffMillis % millisInDay
    
    val isBeforeSunrise = remainderMillis < 0
    
    val displayMillis = if (isBeforeSunrise) {
        millisInDay + remainderMillis
    } else {
        remainderMillis
    }
    
    val totalMinutes = displayMillis / 60000.0
    val hours = (totalMinutes / 60).toInt()
    val mins = (totalMinutes % 60).toInt()
    val secs = ((totalMinutes % 1) * 60).toInt()
    
    val timeString = if (showSeconds) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, mins, secs)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", hours, mins)
    }
    
    val dayInfo = when {
        isBeforeSunrise -> null
        dayCount == 0 -> null
        dayCount == 1 -> "Day 2"
        else -> "Day ${dayCount + 1}"
    }
    
    return SunBasedTimeInfo(timeString, dayInfo)
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
                        AlarmBadge(icon = Icons.AutoMirrored.Filled.TrendingUp, label = null)
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
                        tint = Color(0xFFFF0000)
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

@Composable
fun SunTimesSection(
    sunTimes: List<SunTimes>,
    hasLocationPermission: Boolean,
    locationError: String?,
    onRequestPermission: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            stringResource(R.string.upcoming_sun_times),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Show sun times if we have them (even if there's a transient location error)
        if (sunTimes.isNotEmpty()) {
            sunTimes.take(3).forEachIndexed { index, times ->
                SunTimeRow(
                    sunTimes = times,
                    isToday = index == 0,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        } else {
            // No sun times available - show appropriate message
            when {
                !hasLocationPermission -> {
                    // Permission not granted - ask for permission
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.location_permission_required),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.location_permission_desc_sun),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onRequestPermission,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(stringResource(R.string.grant_permission))
                            }
                        }
                    }
                }
                locationError != null -> {
                    // Permission granted but location error
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.LocationOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.location_error),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.location_error_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onRetry,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text(stringResource(R.string.try_again))
                            }
                        }
                    }
                }
                else -> {
                    // Loading state
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SunTimeRow(
    sunTimes: SunTimes,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    val cardColor = if (isToday) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val borderColor = if (isToday) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Day label
            val dayLabel = when {
                isToday -> stringResource(R.string.today)
                isTomorrowDate(sunTimes.date) -> stringResource(R.string.tomorrow)
                else -> dateFormat.format(sunTimes.date)
            }
            
            Text(
                text = dayLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Sunrise and Sunset times
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sunrise
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WbTwilight,
                        contentDescription = stringResource(R.string.sunrise),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = timeFormat.format(sunTimes.sunrise),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Sunset
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WbTwilight,
                        contentDescription = stringResource(R.string.sunset),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = timeFormat.format(sunTimes.sunset),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

fun isTomorrowDate(date: Date): Boolean {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, 1)
    val tomorrow = calendar.time
    val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    return dateFormat.format(date) == dateFormat.format(tomorrow)
}
