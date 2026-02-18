package dev.mudrock.gunes.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.stringResource
import dev.mudrock.gunes.R
import dev.mudrock.gunes.ui.MainViewModel
import kotlinx.coroutines.delay
import java.util.UUID
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class PomodoroPhase {
    WORK, SHORT_BREAK, LONG_BREAK
}

@Parcelize
data class PomodoroSession(
    val id: String = UUID.randomUUID().toString(),
    val startTime: Long,
    val duration: Int,
    val completed: Boolean,
    val type: PomodoroPhase
) : Parcelable

@Parcelize
data class PomodoroState(
    val currentPhase: PomodoroPhase = PomodoroPhase.WORK,
    val remainingSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val currentSessionCount: Int = 0,
    val sessions: List<PomodoroSession> = emptyList(),
    val targetEndTimeMillis: Long = 0L
) : Parcelable

@Composable
fun PomodoroScreen(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    
    var workDuration by remember { mutableIntStateOf(settings.pomodoroWorkDuration) }
    var shortBreakDuration by remember { mutableIntStateOf(settings.pomodoroShortBreakDuration) }
    var longBreakDuration by remember { mutableIntStateOf(settings.pomodoroLongBreakDuration) }
    var sessionsBeforeLongBreak by remember { mutableIntStateOf(settings.pomodoroSessionsBeforeLongBreak) }
    var autoStartBreaks by remember { mutableStateOf(settings.pomodoroAutoStartBreaks) }
    var autoStartWork by remember { mutableStateOf(settings.pomodoroAutoStartWork) }

    var pomodoroState by rememberSaveable { 
        mutableStateOf(
            PomodoroState(
                currentPhase = PomodoroPhase.valueOf(settings.pomodoroPhase),
                remainingSeconds = settings.pomodoroRemainingSeconds,
                currentSessionCount = settings.pomodoroCurrentSessionCount,
                isRunning = false
            )
        )
    }

    fun getTotalSeconds(phase: PomodoroPhase): Int = when (phase) {
        PomodoroPhase.WORK -> workDuration * 60
        PomodoroPhase.SHORT_BREAK -> shortBreakDuration * 60
        PomodoroPhase.LONG_BREAK -> longBreakDuration * 60
    }

    fun resetCurrentPhase() {
        pomodoroState = pomodoroState.copy(
            remainingSeconds = getTotalSeconds(pomodoroState.currentPhase),
            isRunning = false,
            targetEndTimeMillis = 0L
        )
    }

    fun moveToNextPhase(completed: Boolean = true) {
        val newSessionCount = if (pomodoroState.currentPhase == PomodoroPhase.WORK && completed) {
            pomodoroState.currentSessionCount + 1
        } else {
            pomodoroState.currentSessionCount
        }

        val newSession = if (completed) {
            PomodoroSession(
                startTime = System.currentTimeMillis(),
                duration = getTotalSeconds(pomodoroState.currentPhase),
                completed = true,
                type = pomodoroState.currentPhase
            )
        } else null

        val newPhase = when (pomodoroState.currentPhase) {
            PomodoroPhase.WORK -> {
                if (newSessionCount % sessionsBeforeLongBreak == 0) {
                    PomodoroPhase.LONG_BREAK
                } else {
                    PomodoroPhase.SHORT_BREAK
                }
            }
            PomodoroPhase.SHORT_BREAK, PomodoroPhase.LONG_BREAK -> PomodoroPhase.WORK
        }

        val shouldAutoStart = when (newPhase) {
            PomodoroPhase.WORK -> autoStartWork
            PomodoroPhase.SHORT_BREAK, PomodoroPhase.LONG_BREAK -> autoStartBreaks
        }

        val newRemainingSeconds = getTotalSeconds(newPhase)
        val now = System.currentTimeMillis()

        pomodoroState = pomodoroState.copy(
            currentPhase = newPhase,
            remainingSeconds = newRemainingSeconds,
            currentSessionCount = newSessionCount,
            sessions = if (newSession != null) listOf(newSession) + pomodoroState.sessions else pomodoroState.sessions,
            isRunning = shouldAutoStart,
            targetEndTimeMillis = if (shouldAutoStart) now + (newRemainingSeconds * 1000L) else 0L
        )
    }

    fun startTimer() {
        val now = System.currentTimeMillis()
        pomodoroState = pomodoroState.copy(
            isRunning = true,
            targetEndTimeMillis = now + (pomodoroState.remainingSeconds * 1000L)
        )
    }

    fun pauseTimer() {
        pomodoroState = pomodoroState.copy(
            isRunning = false,
            targetEndTimeMillis = 0L
        )
    }

    val totalSeconds = getTotalSeconds(pomodoroState.currentPhase)
    val progress = (totalSeconds - pomodoroState.remainingSeconds).toFloat() / totalSeconds.coerceAtLeast(1)

    DisposableEffect(Unit) {
        onDispose {
            viewModel.updateSettings { s ->
                s.copy(
                    pomodoroRemainingSeconds = pomodoroState.remainingSeconds,
                    pomodoroPhase = pomodoroState.currentPhase.name,
                    pomodoroIsRunning = false,
                    pomodoroWorkDuration = workDuration,
                    pomodoroShortBreakDuration = shortBreakDuration,
                    pomodoroLongBreakDuration = longBreakDuration,
                    pomodoroSessionsBeforeLongBreak = sessionsBeforeLongBreak,
                    pomodoroCurrentSessionCount = pomodoroState.currentSessionCount,
                    pomodoroAutoStartBreaks = autoStartBreaks,
                    pomodoroAutoStartWork = autoStartWork,
                    pomodoroLastUpdateTime = System.currentTimeMillis()
                )
            }
        }
    }

    LaunchedEffect(pomodoroState.isRunning, pomodoroState.targetEndTimeMillis) {
        if (pomodoroState.isRunning && pomodoroState.targetEndTimeMillis > 0) {
            while (pomodoroState.isRunning) {
                val now = System.currentTimeMillis()
                val remaining = ((pomodoroState.targetEndTimeMillis - now) / 1000L).toInt()
                
                if (remaining <= 0) {
                    pomodoroState = pomodoroState.copy(
                        remainingSeconds = 0,
                        isRunning = false,
                        targetEndTimeMillis = 0L
                    )
                    moveToNextPhase(completed = true)
                    break
                }
                
                if (remaining != pomodoroState.remainingSeconds) {
                    pomodoroState = pomodoroState.copy(remainingSeconds = remaining)
                }
                
                delay(100)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        PhaseIndicator(pomodoroState.currentPhase)

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier.size(280.dp),
            contentAlignment = Alignment.Center
        ) {
            ProgressRing(
                progress = progress.coerceIn(0f, 1f),
                modifier = Modifier.fillMaxSize()
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formatPomodoroTime(pomodoroState.remainingSeconds),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val completedWorkSessions = pomodoroState.sessions.count { it.type == PomodoroPhase.WORK && it.completed }
        val sessionsText = stringResource(R.string.sessions_completed)
        Text(
            "$completedWorkSessions $sessionsText",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!pomodoroState.isRunning) {
                Button(
                    onClick = { startTimer() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.start), fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    onClick = { pauseTimer() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Pause, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.pause))
                }
            }

            OutlinedButton(
                onClick = { resetCurrentPhase() },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
            }

            OutlinedButton(
                onClick = {
                    pauseTimer()
                    moveToNextPhase(completed = false)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = null)
            }

            OutlinedButton(
                onClick = { showSettings = true },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
            }
        }

        if (pomodoroState.sessions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.sessions_completed),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pomodoroState.sessions.take(10), key = { it.id }) { session ->
                            SessionItem(session)
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        PomodoroSettingsDialog(
            workDuration = workDuration,
            shortBreakDuration = shortBreakDuration,
            longBreakDuration = longBreakDuration,
            sessionsBeforeLongBreak = sessionsBeforeLongBreak,
            autoStartBreaks = autoStartBreaks,
            autoStartWork = autoStartWork,
            onSave = { work, shortBreak, longBreak, sessionsCount, autoBreaks, autoWork ->
                workDuration = work
                shortBreakDuration = shortBreak
                longBreakDuration = longBreak
                sessionsBeforeLongBreak = sessionsCount
                autoStartBreaks = autoBreaks
                autoStartWork = autoWork
                
                if (!pomodoroState.isRunning) {
                    pomodoroState = pomodoroState.copy(
                        remainingSeconds = getTotalSeconds(pomodoroState.currentPhase)
                    )
                }
                
                viewModel.updateSettings { s ->
                    s.copy(
                        pomodoroWorkDuration = workDuration,
                        pomodoroShortBreakDuration = shortBreakDuration,
                        pomodoroLongBreakDuration = longBreakDuration,
                        pomodoroSessionsBeforeLongBreak = sessionsBeforeLongBreak,
                        pomodoroAutoStartBreaks = autoStartBreaks,
                        pomodoroAutoStartWork = autoStartWork,
                        pomodoroRemainingSeconds = pomodoroState.remainingSeconds,
                        pomodoroLastUpdateTime = System.currentTimeMillis()
                    )
                }
                showSettings = false
            },
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
fun PhaseIndicator(phase: PomodoroPhase) {
    val icon = when (phase) {
        PomodoroPhase.WORK -> Icons.Default.Psychology
        PomodoroPhase.SHORT_BREAK, PomodoroPhase.LONG_BREAK -> Icons.Default.Coffee
    }
    
    val label = when (phase) {
        PomodoroPhase.WORK -> stringResource(R.string.work)
        PomodoroPhase.SHORT_BREAK -> stringResource(R.string.short_break)
        PomodoroPhase.LONG_BREAK -> stringResource(R.string.long_break)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Text(
            label,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier) {
        val strokeWidth = 8.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val center = Offset(size.width / 2, size.height / 2)

        drawCircle(
            color = backgroundColor,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth)
        )

        val sweepAngle = 360 * progress
        drawArc(
            color = primaryColor,
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun SessionItem(session: PomodoroSession) {
    val icon = when (session.type) {
        PomodoroPhase.WORK -> Icons.Default.Psychology
        else -> Icons.Default.Coffee
    }

    val label = when (session.type) {
        PomodoroPhase.WORK -> stringResource(R.string.work)
        PomodoroPhase.SHORT_BREAK -> stringResource(R.string.short_break)
        PomodoroPhase.LONG_BREAK -> stringResource(R.string.long_break)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (session.completed) 2.dp else 1.dp,
                color = if (session.completed) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            formatPomodoroTime(session.duration),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        if (session.completed) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun PomodoroSettingsDialog(
    workDuration: Int,
    shortBreakDuration: Int,
    longBreakDuration: Int,
    sessionsBeforeLongBreak: Int,
    autoStartBreaks: Boolean,
    autoStartWork: Boolean,
    onSave: (Int, Int, Int, Int, Boolean, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var tempWorkDuration by remember { mutableIntStateOf(workDuration) }
    var tempShortBreakDuration by remember { mutableIntStateOf(shortBreakDuration) }
    var tempLongBreakDuration by remember { mutableIntStateOf(longBreakDuration) }
    var tempSessionsBeforeLongBreak by remember { mutableIntStateOf(sessionsBeforeLongBreak) }
    var tempAutoStartBreaks by remember { mutableStateOf(autoStartBreaks) }
    var tempAutoStartWork by remember { mutableStateOf(autoStartWork) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.pomodoro_settings),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                DurationSlider(
                    label = stringResource(R.string.work_duration),
                    value = tempWorkDuration,
                    range = 1..60,
                    onValueChange = { tempWorkDuration = it }
                )

                DurationSlider(
                    label = stringResource(R.string.short_break_duration),
                    value = tempShortBreakDuration,
                    range = 1..30,
                    onValueChange = { tempShortBreakDuration = it }
                )

                DurationSlider(
                    label = stringResource(R.string.long_break_duration),
                    value = tempLongBreakDuration,
                    range = 1..60,
                    onValueChange = { tempLongBreakDuration = it }
                )

                DurationSlider(
                    label = stringResource(R.string.long_break_after_short_sessions),
                    value = tempSessionsBeforeLongBreak,
                    range = 1..10,
                    onValueChange = { tempSessionsBeforeLongBreak = it },
                    suffix = ""
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.auto_start_breaks),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = tempAutoStartBreaks,
                        onCheckedChange = { tempAutoStartBreaks = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.auto_start_work),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = tempAutoStartWork,
                        onCheckedChange = { tempAutoStartWork = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = {
                            onSave(
                                tempWorkDuration,
                                tempShortBreakDuration,
                                tempLongBreakDuration,
                                tempSessionsBeforeLongBreak,
                                tempAutoStartBreaks,
                                tempAutoStartWork
                            )
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
}

@Composable
fun DurationSlider(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    suffix: String = "min"
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "$label ($value $suffix)",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = range.last - range.first - 1,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

fun formatPomodoroTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
}
