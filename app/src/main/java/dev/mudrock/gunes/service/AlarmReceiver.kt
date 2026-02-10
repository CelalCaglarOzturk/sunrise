package dev.mudrock.gunes.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.AndroidEntryPoint
import dev.mudrock.gunes.data.AlarmDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    lateinit var alarmDao: AlarmDao

    override fun onReceive(context: Context, intent: Intent) {
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

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }

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
