package dev.mudrock.gunes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.mudrock.gunes.ui.MainScreen
import dev.mudrock.gunes.ui.MainViewModel
import dev.mudrock.gunes.ui.ViewModelFactory
import dev.mudrock.gunes.ui.theme.SunriseTheme
import dev.mudrock.gunes.util.LocaleHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appContainer = (application as SunriseApp).container
        
        // Load saved language and apply it
        val settings = runBlocking { appContainer.settingsRepository.settings.first() }
        val context = LocaleHelper.setLocale(this, settings.language)

        setContent {
            val viewModel: MainViewModel = viewModel(
                factory = ViewModelFactory(appContainer)
            )
            
            // Observe language changes
            val settings by viewModel.settings.collectAsState()
            
            // Update locale when language changes
            LocaleHelper.setLocale(this, settings.language)
            
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
}
