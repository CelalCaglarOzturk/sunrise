package dev.mudrock.gunes.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.mudrock.gunes.SunriseApp
import dev.mudrock.gunes.data.AppContainer

class ViewModelFactory(
    private val container: AppContainer,
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(
                container.alarmDao,
                container.settingsRepository,
                container.locationManager,
                container.alarmScheduler,
                container.backupRepository,
                application
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
