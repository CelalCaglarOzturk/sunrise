package dev.mudrock.gunes

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.mudrock.gunes.ui.MainScreen
import dev.mudrock.gunes.ui.MainViewModel
import dev.mudrock.gunes.ui.ViewModelFactory
import dev.mudrock.gunes.ui.theme.SunriseTheme
import dev.mudrock.gunes.util.LocaleHelper

class MainActivity : ComponentActivity() {
    
    private var currentLanguage: String = "en"
    
    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("settings", Context.MODE_PRIVATE)
        currentLanguage = prefs.getString("language", null) ?: "en"
        val context = LocaleHelper.setLocale(newBase, currentLanguage)
        super.attachBaseContext(context)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appContainer = (application as SunriseApp).container
        
        LocaleHelper.applyLocaleToContext(this, currentLanguage)

        setContent {
            val viewModel: MainViewModel = viewModel(
                factory = ViewModelFactory(appContainer)
            )
            
            val settings by viewModel.settings.collectAsState()
            
            LaunchedEffect(settings.language) {
                if (settings.language != currentLanguage) {
                    currentLanguage = settings.language
                    LocaleHelper.applyLocaleToContext(this@MainActivity, settings.language)
                    recreate()
                }
            }
            
            SunriseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel)
                }
            }
        }
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LocaleHelper.applyLocaleToContext(this, currentLanguage)
    }
}
