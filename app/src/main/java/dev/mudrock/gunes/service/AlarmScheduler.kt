package dev.mudrock.gunes.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import dev.mudrock.gunes.data.Alarm
import dev.mudrock.gunes.util.SunCalc
import dev.mudrock.gunes.util.SunTimes
import java.util.Calendar
import java.util.Date
import android.location.Location as AndroidLocation

class AlarmScheduler(
    private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val locationManager = LocationManager(context)

    companion object {
        private const val TAG = "AlarmScheduler"
    }

    suspend fun scheduleAlarm(alarm: Alarm) {
        if (!alarm.enabled) {
            cancelAlarm(alarm)
            return
        }

        val location = locationManager.getCurrentLocation()
        val nextTime = calculateNextAlarmTime(alarm, location)
        
        if (nextTime == null) {
            Log.w(TAG, "Could not calculate next alarm time for ${alarm.id}")
            return
        }

        Log.d(TAG, "Scheduling alarm ${alarm.id} for ${Date(nextTime)}")

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "dev.mudrock.gunes.ALARM_TRIGGER"
            putExtra("ALARM_ID", alarm.id)
        }

        val requestCode = alarm.id.hashCode()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.cancel(pendingIntent)
            
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextTime,
                pendingIntent
            )
            
            Log.d(TAG, "Alarm ${alarm.id} scheduled successfully")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling alarm - likely missing SCHEDULE_EXACT_ALARM permission", e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception scheduling alarm", e)
        }
    }

    fun cancelAlarm(alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d(TAG, "Alarm ${alarm.id} cancelled")
    }

    private fun calculateNextAlarmTime(alarm: Alarm, location: AndroidLocation?): Long? {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        val repeatDays = if (alarm.repeatDays.isEmpty()) {
            emptyList()
        } else {
            alarm.repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }
        }

        for (i in 0..7) {
            calendar.timeInMillis = now
            calendar.add(Calendar.DAY_OF_YEAR, i)

            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
            if (repeatDays.isNotEmpty() && !repeatDays.contains(dayOfWeek)) {
                continue
            }

            var alarmTimeMillis: Long

            if (alarm.useSunTime && location != null) {
                try {
                    val sunTimes = SunCalc.getTimes(
                        calendar.time,
                        location.latitude,
                        location.longitude
                    )

                    if (sunTimes.sunrise == null) {
                        Log.w(TAG, "Null sunrise time for day $i")
                        continue
                    }

                    alarmTimeMillis = sunTimes.sunrise.time + (alarm.sunOffset * 60 * 1000L)
                } catch (e: Exception) {
                    Log.e(TAG, "Error calculating sun time", e)
                    continue
                }
            } else if (alarm.useSunTime && location == null) {
                Log.w(TAG, "Sun-based alarm ${alarm.id} but no location available")
                continue
            } else {
                val (hours, minutes) = alarm.time.split(":").map { it.toInt() }
                calendar.set(Calendar.HOUR_OF_DAY, hours)
                calendar.set(Calendar.MINUTE, minutes)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                alarmTimeMillis = calendar.timeInMillis
            }

            if (alarmTimeMillis > now) {
                return alarmTimeMillis
            }
        }
        
        Log.w(TAG, "No valid alarm time found in next 7 days for alarm ${alarm.id}")
        return null
    }
}
