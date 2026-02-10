package dev.mudrock.gunes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.mudrock.gunes.ui.MainScreen
import dev.mudrock.gunes.ui.MainViewModel
import dev.mudrock.gunes.ui.ViewModelFactory
import dev.mudrock.gunes.ui.theme.SunriseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appContainer = (application as SunriseApp).container

        setContent {
            SunriseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: MainViewModel = viewModel(
                        factory = ViewModelFactory(appContainer)
                    )
                    MainScreen(viewModel)
                }
            }
        }
    }
}
