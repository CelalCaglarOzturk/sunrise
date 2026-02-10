package dev.mudrock.gunes.data

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import dev.mudrock.gunes.service.AlarmScheduler
import dev.mudrock.gunes.service.LocationManager

class AppContainer(private val context: Context) {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "sunrise_db"
        ).build()
    }

    val alarmDao: AlarmDao by lazy {
        database.alarmDao()
    }

    val gson: Gson by lazy {
        Gson()
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context, gson)
    }

    val alarmScheduler: AlarmScheduler by lazy {
        AlarmScheduler(context)
    }

    val locationManager: LocationManager by lazy {
        LocationManager(context)
    }

    val backupRepository: BackupRepository by lazy {
        BackupRepository(context, alarmDao, settingsRepository, gson)
    }
}
