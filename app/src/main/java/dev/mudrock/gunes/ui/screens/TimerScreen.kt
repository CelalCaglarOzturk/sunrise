package dev.mudrock.gunes.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import dev.mudrock.gunes.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TimerItem(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val durationSeconds: Int,
    val remainingSeconds: Int,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val startTimeMillis: Long = 0L
) : Parcelable

@Composable
fun TimerScreen() {
    var timers by rememberSaveable { mutableStateOf<List<TimerItem>>(emptyList()) }
    var showAddTimer by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (timers.isEmpty() && !showAddTimer) {
            EmptyTimerState(
                onAddClick = { showAddTimer = true }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(timers, key = { it.id }) { timer ->
                    TimerCard(
                        timer = timer,
                        onStart = {
                            timers = timers.map { item ->
                                if (item.id == timer.id) item.copy(
                                    isRunning = true,
                                    isPaused = false,
                                    startTimeMillis = System.currentTimeMillis()
                                )
                                else item
                            }
                        },
                        onPause = {
                            timers = timers.map { item ->
                                if (item.id == timer.id) item.copy(isRunning = false, isPaused = true)
                                else item
                            }
                        },
                        onReset = {
                            timers = timers.map { item ->
                                if (item.id == timer.id) item.copy(
                                    isRunning = false,
                                    isPaused = false,
                                    remainingSeconds = item.durationSeconds,
                                    startTimeMillis = 0L
                                )
                                else item
                            }
                        },
                        onDelete = {
                            timers = timers.filter { it.id != timer.id }
                        }
                    )
                }

                if (!showAddTimer) {
                    item {
                        Button(
                            onClick = { showAddTimer = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.add_timer), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (showAddTimer) {
                AddTimerSheet(
                    onAdd = { label, duration ->
                        timers = timers + TimerItem(
                            label = label,
                            durationSeconds = duration,
                            remainingSeconds = duration
                        )
                        showAddTimer = false
                    },
                    onCancel = { showAddTimer = false }
                )
            }
        }
    }

    timers.forEach { timer ->
        if (timer.isRunning && timer.remainingSeconds > 0) {
            LaunchedEffect(timer.id) {
                var elapsed = 0L
                val startRemaining = timer.remainingSeconds
                while (elapsed < startRemaining * 1000L) {
                    delay(1000)
                    elapsed += 1000
                    val newRemaining = (startRemaining - (elapsed / 1000)).toInt().coerceAtLeast(0)
                    timers = timers.map { item ->
                        if (item.id == timer.id && item.isRunning) {
                            item.copy(remainingSeconds = newRemaining)
                        } else item
                    }
                    if (newRemaining == 0) break
                }
            }
        }
    }
}

@Composable
fun EmptyTimerState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Timer,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            stringResource(R.string.no_timers_yet),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(R.string.create_your_first_timer),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.add_timer), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TimerCard(
    timer: TimerItem,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = if (timer.durationSeconds > 0) {
        (timer.durationSeconds - timer.remainingSeconds).toFloat() / timer.durationSeconds
    } else 0f

    val isFinished = timer.remainingSeconds == 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = when {
                    isFinished -> MaterialTheme.colorScheme.primary
                    timer.isRunning -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    timer.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_alarm),
                        tint = Color(0xFFFF0000)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    formatTimerTime(timer.remainingSeconds),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isFinished) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    " / ${formatTimerTime(timer.durationSeconds)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                if (!timer.isRunning && !timer.isPaused) {
                    Button(
                        onClick = onStart,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.start))
                    }
                } else if (timer.isRunning) {
                    Button(
                        onClick = onPause,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.pause))
                    }
                } else {
                    Button(
                        onClick = onStart,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.resume))
                    }
                }

                OutlinedButton(
                    onClick = onReset,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.reset))
                }
            }

            if (isFinished) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        stringResource(R.string.timer_finished),
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun AddTimerSheet(
    onAdd: (label: String, durationSeconds: Int) -> Unit,
    onCancel: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(0) }
    var seconds by remember { mutableIntStateOf(0) }
    val timerLabel = stringResource(R.string.timer)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.add_timer),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text(stringResource(R.string.label_optional)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                stringResource(R.string.duration),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WheelPicker(
                    label = stringResource(R.string.hours),
                    value = hours,
                    range = 0..23,
                    onValueChange = { hours = it },
                    modifier = Modifier.weight(1f)
                )
                WheelPicker(
                    label = stringResource(R.string.minutes),
                    value = minutes,
                    range = 0..59,
                    onValueChange = { minutes = it },
                    modifier = Modifier.weight(1f)
                )
                WheelPicker(
                    label = stringResource(R.string.seconds),
                    value = seconds,
                    range = 0..59,
                    onValueChange = { seconds = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        val totalSeconds = hours * 3600 + minutes * 60 + seconds
                        if (totalSeconds > 0) {
                            onAdd(label.ifEmpty { timerLabel }, totalSeconds)
                        }
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
        }
    }
}

@Composable
fun WheelPicker(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemHeight = 40
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val initialIndex = value - range.first
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = maxOf(0, initialIndex))

    var selectedValue by remember { mutableIntStateOf(value) }

    LaunchedEffect(value) {
        if (value != selectedValue) {
            val targetIndex = value - range.first
            listState.animateScrollToItem(targetIndex)
        }
    }

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val scrollOffset = listState.firstVisibleItemScrollOffset
        val firstVisibleIndex = listState.firstVisibleItemIndex

        val centerPositionPx = with(density) { 60.dp.toPx() }
        val itemHeightPx = with(density) { itemHeight.dp.toPx() }

        val firstItemTop = -scrollOffset
        val centerItemIndex = firstVisibleIndex + ((centerPositionPx - firstItemTop) / itemHeightPx).toInt()

        val actualItemIndex = centerItemIndex - 2

        val newValue = range.first + actualItemIndex

        if (newValue in range && newValue != selectedValue) {
            selectedValue = newValue
            onValueChange(newValue)
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .height(120.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .height(40.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        RoundedCornerShape(8.dp)
                    )
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
                items(range.count()) { index ->
                    val itemValue = range.first + index
                    val isSelected = itemValue == selectedValue
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(index)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format(java.util.Locale.US, "%02d", itemValue),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                items(2) {
                    Box(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

fun formatTimerTime(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format(java.util.Locale.US, "%02d:%02d:%02d", h, m, s)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", m, s)
    }
}
