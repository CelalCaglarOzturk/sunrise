package dev.mudrock.gunes.service

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.mudrock.gunes.util.SunCalc
import dev.mudrock.gunes.util.SunTimes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

class SunTimeWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val prefs = applicationContext.getSharedPreferences("sunrise_prefs", Context.MODE_PRIVATE)
                val lat = prefs.getFloat("last_lat", Float.MAX_VALUE)
                val lng = prefs.getFloat("last_lng", Float.MAX_VALUE)
                
                if (lat == Float.MAX_VALUE || lng == Float.MAX_VALUE) {
                    Log.d(TAG, "No cached location, skipping sun time update")
                    return@withContext Result.success()
                }
                
                updateCachedSunTimes(applicationContext, lat.toDouble(), lng.toDouble())
                Log.d(TAG, "Sun times updated successfully")
                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update sun times", e)
                Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "SunTimeWorker"
        const val WORK_NAME = "sun_time_update"

        fun schedule(context: Context, location: Location? = null) {
            if (location != null) {
                cacheLocation(context, location)
            }
            
            val workRequest = PeriodicWorkRequestBuilder<SunTimeWorker>(
                24, TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun cacheLocation(context: Context, location: Location) {
            context.getSharedPreferences("sunrise_prefs", Context.MODE_PRIVATE)
                .edit()
                .putFloat("last_lat", location.latitude.toFloat())
                .putFloat("last_lng", location.longitude.toFloat())
                .putLong("last_location_update", System.currentTimeMillis())
                .apply()
            
            updateCachedSunTimes(context, location.latitude, location.longitude)
        }

        fun updateCachedSunTimes(context: Context, lat: Double, lng: Double) {
            try {
                val now = Date()
                val sunTimesList = mutableListOf<String>()
                
                for (i in 0..4) {
                    val date = Date(now.time + i * 24 * 60 * 60 * 1000L)
                    try {
                        val sunTimes = SunCalc.getTimes(date, lat, lng)
                        sunTimesList.add("${date.time},${sunTimes.sunrise.time},${sunTimes.sunset.time}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to calculate sun times for day $i", e)
                    }
                }
                
                if (sunTimesList.isNotEmpty()) {
                    context.getSharedPreferences("sunrise_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putLong("last_update", System.currentTimeMillis())
                        .putString("sun_times", sunTimesList.joinToString(";"))
                        .apply()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update cached sun times", e)
            }
        }

        fun getCachedSunTimes(context: Context): List<SunTimes>? {
            return try {
                val prefs = context.getSharedPreferences("sunrise_prefs", Context.MODE_PRIVATE)
                val sunTimesString = prefs.getString("sun_times", null) ?: return null
                
                sunTimesString.split(";").mapNotNull { dayData ->
                    try {
                        val parts = dayData.split(",")
                        if (parts.size == 3) {
                            SunTimes(
                                date = Date(parts[0].toLong()),
                                sunrise = Date(parts[1].toLong()),
                                sunset = Date(parts[2].toLong())
                            )
                        } else if (parts.size == 2) {
                            // Legacy format fallback - use sunrise as date reference
                            val sunriseTime = parts[0].toLong()
                            val sunsetTime = parts[1].toLong()
                            SunTimes(
                                date = Date(sunriseTime),
                                sunrise = Date(sunriseTime),
                                sunset = Date(sunsetTime)
                            )
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                }.takeIf { it.isNotEmpty() }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get cached sun times", e)
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