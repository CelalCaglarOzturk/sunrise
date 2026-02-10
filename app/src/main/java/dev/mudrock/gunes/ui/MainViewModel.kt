package dev.mudrock.gunes.ui

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.mudrock.gunes.data.Alarm
import dev.mudrock.gunes.data.AlarmDao
import dev.mudrock.gunes.data.AppSettings
import dev.mudrock.gunes.data.SettingsRepository
import dev.mudrock.gunes.service.AlarmScheduler
import dev.mudrock.gunes.service.LocationManager
import dev.mudrock.gunes.util.SunCalc
import dev.mudrock.gunes.util.SunTimes
import dev.mudrock.gunes.data.BackupRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date

class MainViewModel(
    private val alarmDao: AlarmDao,
    private val settingsRepository: SettingsRepository,
    private val locationManager: LocationManager,
    private val alarmScheduler: AlarmScheduler,
    private val backupRepository: BackupRepository
) : ViewModel() {

    val alarms = alarmDao.getAllAlarms().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val settings =
        settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _location = MutableStateFlow<Location?>(null)
    val location = _location.asStateFlow()

    private val _sunTimes = MutableStateFlow<List<SunTimes>>(emptyList())
    val sunTimes = _sunTimes.asStateFlow()

    init {
        updateLocation()
    }

    fun updateLocation() {
        viewModelScope.launch {
            val loc = locationManager.getCurrentLocation()
            _location.value = loc
            if (loc != null) {
                updateSunTimes(loc)
            }
        }
    }

    private fun updateSunTimes(loc: Location) {
        val times = mutableListOf<SunTimes>()
        val now = Date()
        for (i in 0..4) {
            val date = Date(now.time + i * 24 * 60 * 60 * 1000)
            times.add(SunCalc.getTimes(date, loc.latitude, loc.longitude))
        }
        _sunTimes.value = times
    }

    fun addAlarm(alarm: Alarm) {
        viewModelScope.launch {
            alarmDao.insertAlarm(alarm)
            alarmScheduler.scheduleAlarm(alarm)
        }
    }

    fun updateAlarm(alarm: Alarm) {
        viewModelScope.launch {
            alarmDao.insertAlarm(alarm) // OnConflictStrategy.REPLACE
            alarmScheduler.scheduleAlarm(alarm)
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            alarmScheduler.cancelAlarm(alarm)
            alarmDao.deleteAlarm(alarm)
        }
    }

    fun toggleAlarm(alarm: Alarm) {
        val newAlarm = alarm.copy(enabled = !alarm.enabled)
        updateAlarm(newAlarm)
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            settingsRepository.updateSettings(transform)
        }
    }

    fun backupSettings(outputStream: java.io.OutputStream) {
        viewModelScope.launch {
            try {
                val json = backupRepository.createBackupJson()
                outputStream.use { it.write(json.toByteArray()) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun restoreSettings(inputStream: java.io.InputStream) {
        viewModelScope.launch {
            try {
                val json = inputStream.bufferedReader().use { it.readText() }
                backupRepository.restoreBackup(json)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteAllAlarms() {
        viewModelScope.launch {
            val all = alarms.value
            all.forEach { alarmScheduler.cancelAlarm(it) }
            alarmDao.deleteAll()
        }
    }

    fun disableAllAlarms() {
        viewModelScope.launch {
            val all = alarms.value
            all.forEach {
                if (it.enabled) {
                    val disabled = it.copy(enabled = false)
                    alarmDao.insertAlarm(disabled)
                    alarmScheduler.cancelAlarm(disabled)
                }
            }
        }
    }

    fun enableAllAlarms() {
        viewModelScope.launch {
            val all = alarms.value
            all.forEach {
                val enabled = it.copy(enabled = true)
                alarmDao.insertAlarm(enabled)
                alarmScheduler.scheduleAlarm(enabled)
            }
        }
    }
}
