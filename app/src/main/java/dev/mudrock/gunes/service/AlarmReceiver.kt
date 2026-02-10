package dev.mudrock.gunes.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import dev.mudrock.gunes.data.AlarmDao
import dev.mudrock.gunes.SunriseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as SunriseApp
        val alarmDao = app.container.alarmDao
        val alarmScheduler = app.container.alarmScheduler

        val alarmId = intent.getStringExtra("ALARM_ID") ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alarm = alarmDao.getAlarmById(alarmId)
                if (alarm != null) {
                    // Start Alarm Service
                    val serviceIntent = Intent(context, AlarmService::class.java).apply {
                        putExtra("ALARM_ID", alarm.id)
                        putExtra("ALARM_LABEL", alarm.label)
                        putExtra("SOUND_URI", alarm.soundUri)
                        putExtra("VOLUME", alarm.volume)
                        putExtra("GRADUAL_VOLUME", alarm.gradualVolume)
                        putExtra("GRADUAL_DURATION", alarm.gradualDuration)
                        putExtra("VIBRATE", alarm.vibrate)
                    }

                    context.startForegroundService(serviceIntent)

                    // Reschedule or Delete
                    if (alarm.deleteAfterUse) {
                        alarmDao.deleteAlarm(alarm)
                    } else {
                        alarmScheduler.scheduleAlarm(alarm)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
