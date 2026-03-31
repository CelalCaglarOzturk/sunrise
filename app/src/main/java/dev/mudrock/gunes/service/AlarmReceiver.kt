package dev.mudrock.gunes.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dev.mudrock.gunes.data.AlarmDao
import dev.mudrock.gunes.SunriseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra("ALARM_ID")
        
        if (alarmId == null) {
            Log.e(TAG, "Received alarm trigger with no ALARM_ID")
            return
        }
        
        Log.d(TAG, "Alarm triggered: $alarmId")

        val app = try {
            context.applicationContext as SunriseApp
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get SunriseApp", e)
            return
        }
        
        val alarmDao = app.container.alarmDao
        val alarmScheduler = app.container.alarmScheduler

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alarm = alarmDao.getAlarmById(alarmId)
                
                if (alarm != null && alarm.enabled) {
                    val serviceIntent = Intent(context, AlarmService::class.java).apply {
                        putExtra("ALARM_ID", alarm.id)
                        putExtra("ALARM_LABEL", alarm.label)
                        putExtra("SOUND_URI", alarm.soundUri)
                        putExtra("VOLUME", alarm.volume)
                        putExtra("GRADUAL_VOLUME", alarm.gradualVolume)
                        putExtra("GRADUAL_DURATION", alarm.gradualDuration)
                        putExtra("VIBRATE", alarm.vibrate)
                    }

                    try {
                        context.startForegroundService(serviceIntent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start AlarmService", e)
                    }

                    if (alarm.deleteAfterUse) {
                        alarmDao.deleteAlarm(alarm)
                    } else {
                        alarmScheduler.scheduleAlarm(alarm)
                    }
                } else {
                    Log.d(TAG, "Alarm $alarmId not found or disabled, skipping")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling alarm", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
