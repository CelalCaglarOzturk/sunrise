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
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import dev.mudrock.gunes.MainActivity
import dev.mudrock.gunes.R
import kotlinx.coroutines.*

class PomodoroService : Service() {

    private val binder = LocalBinder()
    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    private var remainingSeconds: Int = 0
    private var currentPhase: String = "WORK"
    private var isRunning: Boolean = false
    private var onTickCallback: ((Int, String) -> Unit)? = null
    private var onPhaseEndCallback: ((String) -> Unit)? = null
    
    private var timerJob: Job? = null

    inner class LocalBinder : Binder() {
        fun getService(): PomodoroService = this@PomodoroService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PomodoroService:WakeLock")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val totalSeconds = intent.getIntExtra(EXTRA_TOTAL_SECONDS, 25 * 60)
                val phase = intent.getStringExtra(EXTRA_PHASE) ?: "WORK"
                startPomodoro(totalSeconds, phase)
            }
            ACTION_PAUSE -> {
                pausePomodoro()
            }
            ACTION_STOP -> {
                stopPomodoro()
            }
        }
        return START_NOT_STICKY
    }

    fun startPomodoro(totalSeconds: Int, phase: String) {
        remainingSeconds = totalSeconds
        currentPhase = phase
        isRunning = true
        
        startForeground(NOTIFICATION_ID, createNotification(remainingSeconds, phase, false))
        
        wakeLock?.acquire(60 * 60 * 1000L)
        
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isRunning && remainingSeconds > 0) {
                delay(1000)
                remainingSeconds--
                updateNotification()
                onTickCallback?.invoke(remainingSeconds, currentPhase)
                
                if (remainingSeconds <= 0) {
                    onPhaseFinish()
                    break
                }
            }
        }
    }

    fun pausePomodoro() {
        isRunning = false
        timerJob?.cancel()
        wakeLock?.release()
        updateNotification()
    }

    fun resumePomodoro() {
        if (remainingSeconds > 0) {
            isRunning = true
            wakeLock?.acquire(60 * 60 * 1000L)
            
            timerJob?.cancel()
            timerJob = scope.launch {
                while (isRunning && remainingSeconds > 0) {
                    delay(1000)
                    remainingSeconds--
                    updateNotification()
                    onTickCallback?.invoke(remainingSeconds, currentPhase)
                    
                    if (remainingSeconds <= 0) {
                        onPhaseFinish()
                        break
                    }
                }
            }
        }
    }

    fun stopPomodoro() {
        isRunning = false
        timerJob?.cancel()
        remainingSeconds = 0
        stopMediaPlayer()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun onPhaseFinish() {
        isRunning = false
        wakeLock?.release()
        updateNotification()
        onPhaseEndCallback?.invoke(currentPhase)
        playFinishSound()
    }

    private fun playFinishSound() {
        try {
            val uri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = false
                setVolume(0.8f, 0.8f)
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopMediaPlayer() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun setOnTickCallback(callback: (Int, String) -> Unit) {
        onTickCallback = callback
    }

    fun setOnPhaseEndCallback(callback: (String) -> Unit) {
        onPhaseEndCallback = callback
    }

    fun getRemainingSeconds(): Int = remainingSeconds
    fun getCurrentPhase(): String = currentPhase
    fun isRunning(): Boolean = isRunning

    fun updatePhase(newPhase: String, totalSeconds: Int) {
        currentPhase = newPhase
        remainingSeconds = totalSeconds
        updateNotification()
    }

    private fun createNotification(remaining: Int, phase: String, isFinished: Boolean): android.app.Notification {
        createNotificationChannel()
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NAVIGATE_TO", "pomodoro")
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = Intent(this, PomodoroService::class.java).apply {
            action = ACTION_PAUSE
        }
        val pausePendingIntent = PendingIntent.getService(
            this, 1, pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, PomodoroService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val phaseLabel = when (phase) {
            "WORK" -> "Focus"
            "SHORT_BREAK" -> "Short Break"
            "LONG_BREAK" -> "Long Break"
            else -> phase
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (isFinished) "$phaseLabel - Finished!" else phaseLabel)
            .setContentText(if (isFinished) "Phase completed" else formatTime(remaining))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(!isFinished && isRunning)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)

        if (!isFinished && isRunning) {
            builder.addAction(R.drawable.ic_stop, "Pause", pausePendingIntent)
        }
        builder.addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)

        return builder.build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(remainingSeconds, currentPhase, remainingSeconds <= 0))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pomodoro Timer",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pomodoro timer notifications"
                setSound(null, null)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun formatTime(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format("%02d:%02d", m, s)
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        scope.cancel()
        stopMediaPlayer()
        wakeLock?.release()
    }

    companion object {
        const val CHANNEL_ID = "pomodoro_service_channel"
        const val NOTIFICATION_ID = 1002
        
        const val ACTION_START = "dev.mudrock.gunes.POMODORO_START"
        const val ACTION_PAUSE = "dev.mudrock.gunes.POMODORO_PAUSE"
        const val ACTION_STOP = "dev.mudrock.gunes.POMODORO_STOP"
        
        const val EXTRA_TOTAL_SECONDS = "total_seconds"
        const val EXTRA_PHASE = "phase"
    }
}