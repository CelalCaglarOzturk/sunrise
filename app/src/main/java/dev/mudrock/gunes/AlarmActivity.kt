package dev.mudrock.gunes

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.mudrock.gunes.service.AlarmService
import dev.mudrock.gunes.ui.theme.SunriseTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmActivity : ComponentActivity() {

    private val scope = CoroutineScope(Dispatchers.IO)

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        val alarmLabel = intent.getStringExtra("ALARM_LABEL") ?: "Alarm"
        val alarmId = intent.getStringExtra("ALARM_ID")

        setContent {
            SunriseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AlarmScreen(label = alarmLabel, onDismiss = {
                        stopService(Intent(this, AlarmService::class.java))
                        finish()
                    }, onSnooze = {
                        stopService(Intent(this, AlarmService::class.java))
                        if (alarmId != null) {
                            snoozeAlarm(alarmId)
                        }
                        finish()
                    })
                }
            }
        }
    }

    private fun snoozeAlarm(alarmId: String) {
        scope.launch {
            val app = application as SunriseApp
            val alarmDao = app.container.alarmDao
            val alarmScheduler = app.container.alarmScheduler
            
            val alarm = alarmDao.getAlarmById(alarmId)
            if (alarm != null) {
                val snoozeMinutes = 5
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.MINUTE, snoozeMinutes)
                
                val snoozedAlarm = alarm.copy(
                    time = String.format(java.util.Locale.US, "%02d:%02d", 
                        calendar.get(Calendar.HOUR_OF_DAY), 
                        calendar.get(Calendar.MINUTE)),
                    useSunTime = false,
                    deleteAfterUse = true,
                    enabled = true
                )
                
                alarmDao.insertAlarm(snoozedAlarm)
                alarmScheduler.scheduleAlarm(snoozedAlarm)
            }
        }
    }
}

@Composable
fun AlarmScreen(label: String, onDismiss: () -> Unit, onSnooze: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(64.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Dismiss", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onSnooze,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Snooze", style = MaterialTheme.typography.titleLarge)
        }
    }
}
