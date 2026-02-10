package dev.mudrock.gunes

import android.app.Application
import dev.mudrock.gunes.data.AppContainer

class SunriseApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
