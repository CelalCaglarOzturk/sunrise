package dev.mudrock.gunes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.mudrock.gunes.data.AppContainer

class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(
                container.alarmDao,
                container.settingsRepository,
                container.locationManager,
                container.alarmScheduler,
                container.backupRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
