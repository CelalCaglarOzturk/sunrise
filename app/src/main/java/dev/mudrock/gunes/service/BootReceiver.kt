package dev.mudrock.gunes.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dev.mudrock.gunes.data.AlarmDao
import dev.mudrock.gunes.SunriseApp
import dev.mudrock.gunes.util.SunCalc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed, rescheduling alarms and updating sun times")
            
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
                    // Update sun times first
                    updateSunTimes(context)
                    
                    // Reschedule all enabled alarms
                    val alarms = alarmDao.getAllAlarms().first()
                    alarms.forEach { alarm ->
                        if (alarm.enabled) {
                            alarmScheduler.scheduleAlarm(alarm)
                        }
                    }
                    Log.d(TAG, "Rescheduled ${alarms.count { it.enabled }} alarms")
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
    
    private suspend fun updateSunTimes(context: Context) {
        try {
            val prefs = context.getSharedPreferences("sunrise_prefs", Context.MODE_PRIVATE)
            val lat = prefs.getFloat("last_lat", Float.MAX_VALUE)
            val lng = prefs.getFloat("last_lng", Float.MAX_VALUE)
            
            if (lat != Float.MAX_VALUE && lng != Float.MAX_VALUE) {
                SunTimeWorker.updateCachedSunTimes(context, lat.toDouble(), lng.toDouble())
                Log.d(TAG, "Updated sun times after boot")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update sun times", e)
        }
    }
}
