package dev.mudrock.gunes.service

import android.content.Context
import android.location.Location
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.mudrock.gunes.data.AppSettings
import dev.mudrock.gunes.util.SunCalc
import dev.mudrock.gunes.util.SunTimes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

class SunTimeWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val lastLocation = try {
            val prefs = applicationContext.getSharedPreferences("sunrise_prefs", Context.MODE_PRIVATE)
            val lat = prefs.getFloat("last_lat", Float.MAX_VALUE)
            val lng = prefs.getFloat("last_lng", Float.MAX_VALUE)
            if (lat != Float.MAX_VALUE && lng != Float.MAX_VALUE) {
                val location = Location("cached")
                location.latitude = lat.toDouble()
                location.longitude = lng.toDouble()
                location
            } else null
        } catch (e: Exception) {
            null
        }

        if (lastLocation != null) {
            updateCachedSunTimes(applicationContext, lastLocation.latitude, lastLocation.longitude)
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "sun_time_update"

        fun schedule(context: Context, location: Location? = null) {
            val now = Calendar.getInstance()
            val midnight = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                add(Calendar.DAY_OF_MONTH, 1)
            }
            
            val initialDelay = midnight.timeInMillis - now.timeInMillis

            val workRequest = PeriodicWorkRequestBuilder<SunTimeWorker>(
                24, TimeUnit.HOURS
            )
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )

            if (location != null) {
                cacheLocation(context, location)
            }
        }

        fun cacheLocation(context: Context, location: Location) {
            context.getSharedPreferences("sunrise_prefs", Context.MODE_PRIVATE)
                .edit()
                .putFloat("last_lat", location.latitude.toFloat())
                .putFloat("last_lng", location.longitude.toFloat())
                .apply()
            
            updateCachedSunTimes(context, location.latitude, location.longitude)
        }

        private fun updateCachedSunTimes(context: Context, lat: Double, lng: Double) {
            val now = Date()
            val sunTimesList = mutableListOf<String>()
            
            for (i in 0..4) {
                val date = Date(now.time + i * 24 * 60 * 60 * 1000L)
                val sunTimes = SunCalc.getTimes(date, lat, lng)
                sunTimesList.add("${sunTimes.sunrise.time},${sunTimes.sunset.time}")
            }

            context.getSharedPreferences("sunrise_prefs", Context.MODE_PRIVATE)
                .edit()
                .putLong("last_update", System.currentTimeMillis())
                .putString("sun_times", sunTimesList.joinToString(";"))
                .apply()
        }

        fun getCachedSunTimes(context: Context): List<SunTimes>? {
            val prefs = context.getSharedPreferences("sunrise_prefs", Context.MODE_PRIVATE)
            val sunTimesString = prefs.getString("sun_times", null) ?: return null
            
            return try {
                sunTimesString.split(";").map { dayData ->
                    val parts = dayData.split(",")
                    SunTimes(
                        sunrise = Date(parts[0].toLong()),
                        sunset = Date(parts[1].toLong()),
                        date = Date()
                    )
                }
            } catch (e: Exception) {
                null
            }
        }

        fun needsUpdate(context: Context): Boolean {
            val prefs = context.getSharedPreferences("sunrise_prefs", Context.MODE_PRIVATE)
            val lastUpdate = prefs.getLong("last_update", 0)
            val now = System.currentTimeMillis()
            val hoursSinceUpdate = (now - lastUpdate) / (1000 * 60 * 60)
            return hoursSinceUpdate >= 24
        }
    }
}