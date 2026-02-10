package dev.mudrock.gunes.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import dev.mudrock.gunes.AlarmActivity
import dev.mudrock.gunes.R
import kotlinx.coroutines.*

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var volumeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getStringExtra("ALARM_ID") ?: return START_NOT_STICKY
        val label = intent.getStringExtra("ALARM_LABEL") ?: "Alarm"
        val soundUriString = intent.getStringExtra("SOUND_URI")
        val volume = intent.getIntExtra("VOLUME", 100)
        val gradualVolume = intent.getBooleanExtra("GRADUAL_VOLUME", false)
        val gradualDuration = intent.getIntExtra("GRADUAL_DURATION", 0)
        val shouldVibrate = intent.getBooleanExtra("VIBRATE", true)

        startForeground(1, createNotification(label, alarmId))

        startAlarm(soundUriString, volume, gradualVolume, gradualDuration, shouldVibrate)

        // Launch Activity
        val activityIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_LABEL", label)
        }
        startActivity(activityIntent)

        return START_STICKY
    }

    private fun createNotification(label: String, alarmId: String): android.app.Notification {
        val channelId = "alarm_service_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Alarm Service", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_LABEL", label)
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(label)
            .setContentText("Alarm is ringing")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .build()
    }

    private fun startAlarm(
        soundUriString: String?,
        targetVolume: Int,
        gradual: Boolean,
        duration: Int,
        vibrate: Boolean
    ) {
        // Sound
        try {
            val uri = if (!soundUriString.isNullOrEmpty()) Uri.parse(soundUriString) else RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_ALARM
            )
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = true
                prepare()
            }

            if (gradual) {
                mediaPlayer?.setVolume(0.01f, 0.01f)
                mediaPlayer?.start()
                startGradualVolume(targetVolume, duration)
            } else {
                val vol = targetVolume / 100f
                mediaPlayer?.setVolume(vol, vol)
                mediaPlayer?.start()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Vibrate
        if (vibrate) {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            val pattern = longArrayOf(0, 1000, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        }
    }

    private fun startGradualVolume(targetVolume: Int, durationSeconds: Int) {
        volumeJob = scope.launch {
            val steps = durationSeconds * 10 // Update every 100ms
            if (steps <= 0) return@launch
            val maxVolume = targetVolume / 100f
            val stepSize = maxVolume / steps

            for (i in 1..steps) {
                val currentVol = 0.01f + (stepSize * i)
                if (currentVol > maxVolume) break
                mediaPlayer?.setVolume(currentVol, currentVol)
                delay(100)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        vibrator?.cancel()
        volumeJob?.cancel()
        scope.cancel()
    }
}
