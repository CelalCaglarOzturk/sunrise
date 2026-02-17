package dev.mudrock.gunes.data

import android.content.Context
import android.os.Build
import android.text.format.DateFormat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val defaultClockMode: String = "normal",
    val use24HourFormat: Boolean = true,
    val showSeconds: Boolean = false,
    val language: String = "en",
    val pomodoroRemainingSeconds: Int = 25 * 60,
    val pomodoroPhase: String = "WORK",
    val pomodoroIsRunning: Boolean = false,
    val pomodoroWorkDuration: Int = 25,
    val pomodoroShortBreakDuration: Int = 5,
    val pomodoroLongBreakDuration: Int = 15,
    val pomodoroSessionsBeforeLongBreak: Int = 4,
    val pomodoroCurrentSessionCount: Int = 0,
    val pomodoroAutoStartBreaks: Boolean = false,
    val pomodoroAutoStartWork: Boolean = false,
    val pomodoroLastUpdateTime: Long = 0
)

class SettingsRepository(private val context: Context, private val gson: Gson) {
    private val dataStore = context.dataStore

    private object Keys {
        val DEFAULT_CLOCK_MODE = stringPreferencesKey("default_clock_mode")
        val USE_24_HOUR_FORMAT = booleanPreferencesKey("use_24_hour_format")
        val SHOW_SECONDS = booleanPreferencesKey("show_seconds")
        val LANGUAGE = stringPreferencesKey("language")
        val POMODORO_REMAINING_SECONDS = intPreferencesKey("pomodoro_remaining_seconds")
        val POMODORO_PHASE = stringPreferencesKey("pomodoro_phase")
        val POMODORO_IS_RUNNING = booleanPreferencesKey("pomodoro_is_running")
        val POMODORO_WORK_DURATION = intPreferencesKey("pomodoro_work_duration")
        val POMODORO_SHORT_BREAK_DURATION = intPreferencesKey("pomodoro_short_break_duration")
        val POMODORO_LONG_BREAK_DURATION = intPreferencesKey("pomodoro_long_break_duration")
        val POMODORO_SESSIONS_BEFORE_LONG_BREAK = intPreferencesKey("pomodoro_sessions_before_long_break")
        val POMODORO_CURRENT_SESSION_COUNT = intPreferencesKey("pomodoro_current_session_count")
        val POMODORO_AUTO_START_BREAKS = booleanPreferencesKey("pomodoro_auto_start_breaks")
        val POMODORO_AUTO_START_WORK = booleanPreferencesKey("pomodoro_auto_start_work")
        val POMODORO_LAST_UPDATE_TIME = longPreferencesKey("pomodoro_last_update_time")
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
    }

    companion object {
        private val SUPPORTED_LANGUAGES = listOf("en", "tr", "es", "de", "fr", "it", "pt", "ru", "ja", "zh", "ar")
        
        fun getSystemLanguage(): String {
            val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Locale.getDefault()
            } else {
                Locale.getDefault()
            }
            val langCode = locale.language
            return if (langCode in SUPPORTED_LANGUAGES) langCode else "en"
        }
        
        fun isSystem24HourFormat(context: Context): Boolean {
            return DateFormat.is24HourFormat(context)
        }
    }

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        val isFirstLaunch = prefs[Keys.FIRST_LAUNCH] ?: true
        
        // Use system defaults on first launch
        val detectedLanguage = if (isFirstLaunch) getSystemLanguage() else (prefs[Keys.LANGUAGE] ?: "en")
        val detected24HourFormat = if (isFirstLaunch) isSystem24HourFormat(context) else (prefs[Keys.USE_24_HOUR_FORMAT] ?: true)
        
        AppSettings(
            defaultClockMode = prefs[Keys.DEFAULT_CLOCK_MODE] ?: "normal",
            use24HourFormat = detected24HourFormat,
            showSeconds = prefs[Keys.SHOW_SECONDS] ?: false,
            language = detectedLanguage,
            pomodoroRemainingSeconds = prefs[Keys.POMODORO_REMAINING_SECONDS] ?: 25 * 60,
            pomodoroPhase = prefs[Keys.POMODORO_PHASE] ?: "WORK",
            pomodoroIsRunning = prefs[Keys.POMODORO_IS_RUNNING] ?: false,
            pomodoroWorkDuration = prefs[Keys.POMODORO_WORK_DURATION] ?: 25,
            pomodoroShortBreakDuration = prefs[Keys.POMODORO_SHORT_BREAK_DURATION] ?: 5,
            pomodoroLongBreakDuration = prefs[Keys.POMODORO_LONG_BREAK_DURATION] ?: 15,
            pomodoroSessionsBeforeLongBreak = prefs[Keys.POMODORO_SESSIONS_BEFORE_LONG_BREAK] ?: 4,
            pomodoroCurrentSessionCount = prefs[Keys.POMODORO_CURRENT_SESSION_COUNT] ?: 0,
            pomodoroAutoStartBreaks = prefs[Keys.POMODORO_AUTO_START_BREAKS] ?: false,
            pomodoroAutoStartWork = prefs[Keys.POMODORO_AUTO_START_WORK] ?: false,
            pomodoroLastUpdateTime = prefs[Keys.POMODORO_LAST_UPDATE_TIME] ?: 0
        )
    }
    
    suspend fun markFirstLaunchComplete() {
        dataStore.edit { prefs ->
            prefs[Keys.FIRST_LAUNCH] = false
        }
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            val isFirstLaunch = prefs[Keys.FIRST_LAUNCH] ?: true
            val detectedLanguage = if (isFirstLaunch) getSystemLanguage() else (prefs[Keys.LANGUAGE] ?: "en")
            val detected24HourFormat = if (isFirstLaunch) isSystem24HourFormat(context) else (prefs[Keys.USE_24_HOUR_FORMAT] ?: true)
            
            val current = AppSettings(
                defaultClockMode = prefs[Keys.DEFAULT_CLOCK_MODE] ?: "normal",
                use24HourFormat = detected24HourFormat,
                showSeconds = prefs[Keys.SHOW_SECONDS] ?: false,
                language = detectedLanguage,
                pomodoroRemainingSeconds = prefs[Keys.POMODORO_REMAINING_SECONDS] ?: 25 * 60,
                pomodoroPhase = prefs[Keys.POMODORO_PHASE] ?: "WORK",
                pomodoroIsRunning = prefs[Keys.POMODORO_IS_RUNNING] ?: false,
                pomodoroWorkDuration = prefs[Keys.POMODORO_WORK_DURATION] ?: 25,
                pomodoroShortBreakDuration = prefs[Keys.POMODORO_SHORT_BREAK_DURATION] ?: 5,
                pomodoroLongBreakDuration = prefs[Keys.POMODORO_LONG_BREAK_DURATION] ?: 15,
                pomodoroSessionsBeforeLongBreak = prefs[Keys.POMODORO_SESSIONS_BEFORE_LONG_BREAK] ?: 4,
                pomodoroCurrentSessionCount = prefs[Keys.POMODORO_CURRENT_SESSION_COUNT] ?: 0,
                pomodoroAutoStartBreaks = prefs[Keys.POMODORO_AUTO_START_BREAKS] ?: false,
                pomodoroAutoStartWork = prefs[Keys.POMODORO_AUTO_START_WORK] ?: false,
                pomodoroLastUpdateTime = prefs[Keys.POMODORO_LAST_UPDATE_TIME] ?: 0
            )
            val newSettings = transform(current)
            prefs[Keys.DEFAULT_CLOCK_MODE] = newSettings.defaultClockMode
            prefs[Keys.USE_24_HOUR_FORMAT] = newSettings.use24HourFormat
            prefs[Keys.SHOW_SECONDS] = newSettings.showSeconds
            prefs[Keys.LANGUAGE] = newSettings.language
            prefs[Keys.POMODORO_REMAINING_SECONDS] = newSettings.pomodoroRemainingSeconds
            prefs[Keys.POMODORO_PHASE] = newSettings.pomodoroPhase
            prefs[Keys.POMODORO_IS_RUNNING] = newSettings.pomodoroIsRunning
            prefs[Keys.POMODORO_WORK_DURATION] = newSettings.pomodoroWorkDuration
            prefs[Keys.POMODORO_SHORT_BREAK_DURATION] = newSettings.pomodoroShortBreakDuration
            prefs[Keys.POMODORO_LONG_BREAK_DURATION] = newSettings.pomodoroLongBreakDuration
            prefs[Keys.POMODORO_SESSIONS_BEFORE_LONG_BREAK] = newSettings.pomodoroSessionsBeforeLongBreak
            prefs[Keys.POMODORO_CURRENT_SESSION_COUNT] = newSettings.pomodoroCurrentSessionCount
            prefs[Keys.POMODORO_AUTO_START_BREAKS] = newSettings.pomodoroAutoStartBreaks
            prefs[Keys.POMODORO_AUTO_START_WORK] = newSettings.pomodoroAutoStartWork
            prefs[Keys.POMODORO_LAST_UPDATE_TIME] = newSettings.pomodoroLastUpdateTime
            // Mark first launch as complete when settings are updated
            prefs[Keys.FIRST_LAUNCH] = false
        }
    }
}
