package dev.mudrock.gunes.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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

    suspend fun scheduleAlarm(alarm: Alarm) {
        if (!alarm.enabled) {
            cancelAlarm(alarm)
            return
        }

        val location = locationManager.getCurrentLocation()
        val nextTime = calculateNextAlarmTime(alarm, location) ?: return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
            putExtra("ALARM_LABEL", alarm.label)
            putExtra("ALARM_SOUND", alarm.soundUri)
            putExtra("ALARM_VIBRATE", alarm.vibrate)
        }

        // Use a unique ID based on hash of UUID string for the PendingIntent
        val requestCode = alarm.id.hashCode()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextTime,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // Handle permission exception
            e.printStackTrace()
        }
    }

    fun cancelAlarm(alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun calculateNextAlarmTime(alarm: Alarm, location: AndroidLocation?): Long? {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        // Parse repeat days
        val repeatDays = if (alarm.repeatDays.isEmpty()) {
            emptyList()
        } else {
            alarm.repeatDays.split(",").map { it.toInt() }
        }

        // Check next 7 days
        for (i in 0..7) {
            calendar.timeInMillis = now
            calendar.add(Calendar.DAY_OF_YEAR, i)

            // If repeating, check if this day is enabled
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sunday
            if (repeatDays.isNotEmpty() && !repeatDays.contains(dayOfWeek)) {
                continue
            }

            var alarmTimeMillis: Long

            if (alarm.useSunTime && location != null) {
                val sunTimes = SunCalc.getTimes(
                    calendar.time,
                    location.latitude,
                    location.longitude
                )

                // Sunrise time + offset
                alarmTimeMillis = sunTimes.sunrise.time + (alarm.sunOffset * 60 * 1000)
            } else {
                val (hours, minutes) = alarm.time.split(":").map { it.toInt() }
                calendar.set(Calendar.HOUR_OF_DAY, hours)
                calendar.set(Calendar.MINUTE, minutes)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                alarmTimeMillis = calendar.timeInMillis
            }

            if (alarmTimeMillis > now) {
                // If it's a one-time alarm (no repeat days) and we are checking future days,
                // we only accept it if i == 0 (today) or if it's tomorrow and time hasn't passed today.
                // Actually simpler: if repeatDays is empty, we just take the first valid future time.
                // If repeatDays is NOT empty, we take the first valid future time that matches the day.
                return alarmTimeMillis
            }
        }
        return null
    }
}
