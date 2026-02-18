package dev.mudrock.gunes.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.mudrock.gunes.SunriseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val app = context.applicationContext as SunriseApp
            val alarmDao = app.container.alarmDao
            val alarmScheduler = app.container.alarmScheduler

            CoroutineScope(Dispatchers.IO).launch {
                val alarms = alarmDao.getAllAlarms().first()
                alarms.forEach { alarm ->
                    if (alarm.enabled) {
                        alarmScheduler.scheduleAlarm(alarm)
                    }
                }
            }
        }
    }
}
