package dev.mudrock.gunes

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dev.mudrock.gunes.data.AppContainer
import dev.mudrock.gunes.service.SunTimeWorker
import java.util.concurrent.TimeUnit

class SunriseApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        
        // Schedule daily sun time update at midnight
        scheduleSunTimeUpdate()
    }
    
    private fun scheduleSunTimeUpdate() {
        val workRequest = PeriodicWorkRequestBuilder<SunTimeWorker>(
            24, TimeUnit.HOURS
        ).build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SunTimeWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
