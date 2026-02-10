package dev.mudrock.gunes.data

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.flow.first

data class BackupData(
    val alarms: List<Alarm>,
    val settings: AppSettings
)

class BackupRepository(
    private val context: Context,
    private val alarmDao: AlarmDao,
    private val settingsRepository: SettingsRepository,
    private val gson: Gson
) {
    suspend fun createBackupJson(): String {
        val alarms = alarmDao.getAllAlarms().first()
        val settings = settingsRepository.settings.first()
        val backup = BackupData(alarms, settings)
        return gson.toJson(backup)
    }

    suspend fun restoreBackup(json: String) {
        val backup = gson.fromJson(json, BackupData::class.java)

        // Restore Alarms
        alarmDao.deleteAll()
        backup.alarms.forEach { alarmDao.insertAlarm(it) }

        // Restore Settings
        settingsRepository.updateSettings {
            backup.settings
        }
    }
}
