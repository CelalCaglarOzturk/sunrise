package dev.mudrock.gunes.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val defaultClockMode: String = "normal",
    val use24HourFormat: Boolean = true,
    val showSeconds: Boolean = false,
    val language: String = "en"
)

class SettingsRepository(private val context: Context, private val gson: Gson) {
    private val dataStore = context.dataStore

    private object Keys {
        val DEFAULT_CLOCK_MODE = stringPreferencesKey("default_clock_mode")
        val USE_24_HOUR_FORMAT = booleanPreferencesKey("use_24_hour_format")
        val SHOW_SECONDS = booleanPreferencesKey("show_seconds")
        val LANGUAGE = stringPreferencesKey("language")
    }

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            defaultClockMode = prefs[Keys.DEFAULT_CLOCK_MODE] ?: "normal",
            use24HourFormat = prefs[Keys.USE_24_HOUR_FORMAT] ?: true,
            showSeconds = prefs[Keys.SHOW_SECONDS] ?: false,
            language = prefs[Keys.LANGUAGE] ?: "en"
        )
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            val current = AppSettings(
                defaultClockMode = prefs[Keys.DEFAULT_CLOCK_MODE] ?: "normal",
                use24HourFormat = prefs[Keys.USE_24_HOUR_FORMAT] ?: true,
                showSeconds = prefs[Keys.SHOW_SECONDS] ?: false,
                language = prefs[Keys.LANGUAGE] ?: "en"
            )
            val newSettings = transform(current)
            prefs[Keys.DEFAULT_CLOCK_MODE] = newSettings.defaultClockMode
            prefs[Keys.USE_24_HOUR_FORMAT] = newSettings.use24HourFormat
            prefs[Keys.SHOW_SECONDS] = newSettings.showSeconds
            prefs[Keys.LANGUAGE] = newSettings.language
        }
    }
}
