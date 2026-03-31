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

class TimerService : Service() {

    private val binder = LocalBinder()
    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    private var totalSeconds: Int = 0
    private var targetEndTimeMillis: Long = 0L
    private var timerLabel: String = ""
    private var isTimerRunning: Boolean = false
    private var isPaused: Boolean = false
    private var pausedRemainingSeconds: Int = 0
    private var onTickCallback: ((Int) -> Unit)? = null
    private var onFinishCallback: (() -> Unit)? = null
    
    private var timerJob: Job? = null

    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TimerService:WakeLock")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val totalSecs = intent.getIntExtra(EXTRA_TOTAL_SECONDS, 0)
                val label = intent.getStringExtra(EXTRA_LABEL) ?: "Timer"
                startTimer(totalSecs, label)
            }
            ACTION_PAUSE -> {
                pauseTimer()
            }
            ACTION_STOP -> {
                stopTimer()
            }
        }
        return START_NOT_STICKY
    }

    fun startTimer(totalSecs: Int, label: String) {
        totalSeconds = totalSecs
        targetEndTimeMillis = System.currentTimeMillis() + (totalSecs * 1000L)
        timerLabel = label
        isTimerRunning = true
        isPaused = false
        
        startForeground(NOTIFICATION_ID, createNotification(totalSecs, label, false))
        
        acquireWakeLock()
        
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isTimerRunning && isActive) {
                val remaining = calculateRemainingSeconds()
                
                if (remaining <= 0) {
                    onTimerFinish()
                    break
                }
                
                updateNotification()
                onTickCallback?.invoke(remaining)
                
                delay(100)
            }
        }
    }

    fun pauseTimer() {
        if (!isTimerRunning) return
        
        isTimerRunning = false
        isPaused = true
        pausedRemainingSeconds = calculateRemainingSeconds()
        timerJob?.cancel()
        releaseWakeLock()
        updateNotification()
        
        // Change to paused notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(pausedRemainingSeconds, timerLabel, false, true))
    }

    fun resumeTimer() {
        if (!isPaused) return
        
        isTimerRunning = true
        isPaused = false
        targetEndTimeMillis = System.currentTimeMillis() + (pausedRemainingSeconds * 1000L)
        
        acquireWakeLock()
        
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isTimerRunning && isActive) {
                val remaining = calculateRemainingSeconds()
                
                if (remaining <= 0) {
                    onTimerFinish()
                    break
                }
                
                updateNotification()
                onTickCallback?.invoke(remaining)
                
                delay(100)
            }
        }
    }

    fun stopTimer() {
        isTimerRunning = false
        isPaused = false
        timerJob?.cancel()
        stopMediaPlayer()
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun calculateRemainingSeconds(): Int {
        val now = System.currentTimeMillis()
        return ((targetEndTimeMillis - now) / 1000L).toInt().coerceAtLeast(0)
    }

    private fun onTimerFinish() {
        isTimerRunning = false
        isPaused = false
        releaseWakeLock()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(0, timerLabel, true))
        
        onFinishCallback?.invoke()
        playFinishSound()
    }

    private fun playFinishSound() {
        try {
            val uri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            
            if (uri != null) {
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
                    setOnCompletionListener { stopForeground(STOP_FOREGROUND_REMOVE) }
                    prepare()
                    start()
                }
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

    private fun acquireWakeLock() {
        try {
            wakeLock?.acquire(10 * 60 * 1000L)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setOnTickCallback(callback: (Int) -> Unit) {
        onTickCallback = callback
    }

    fun setOnFinishCallback(callback: () -> Unit) {
        onFinishCallback = callback
    }

    fun getRemainingSeconds(): Int = calculateRemainingSeconds()
    fun isRunning(): Boolean = isTimerRunning
    fun isPausedState(): Boolean = isPaused

    private fun createNotification(remaining: Int, label: String, isFinished: Boolean, isPaused: Boolean = false): android.app.Notification {
        createNotificationChannel()
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NAVIGATE_TO", "timer")
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_PAUSE
        }
        val pausePendingIntent = PendingIntent.getService(
            this, 1, pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when {
            isFinished -> "$label - Finished!"
            isPaused -> "$label (Paused)"
            else -> label
        }
        
        val content = when {
            isFinished -> "Timer completed"
            else -> formatTime(remaining)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(!isFinished)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)

        if (!isFinished && !isPaused) {
            builder.addAction(R.drawable.ic_stop, "Pause", pausePendingIntent)
        }
        if (!isFinished) {
            builder.addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
        }

        return builder.build()
    }

    private fun updateNotification() {
        val remaining = calculateRemainingSeconds()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(remaining, timerLabel, false, isPaused))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer Service",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Timer notifications"
                setSound(null, null)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun formatTime(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            String.format("%02d:%02d:%02d", h, m, s)
        } else {
            String.format("%02d:%02d", m, s)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        scope.cancel()
        stopMediaPlayer()
        releaseWakeLock()
    }

    companion object {
        const val CHANNEL_ID = "timer_service_channel"
        const val NOTIFICATION_ID = 1001
        
        const val ACTION_START = "dev.mudrock.gunes.TIMER_START"
        const val ACTION_PAUSE = "dev.mudrock.gunes.TIMER_PAUSE"
        const val ACTION_STOP = "dev.mudrock.gunes.TIMER_STOP"
        
        const val EXTRA_TOTAL_SECONDS = "total_seconds"
        const val EXTRA_LABEL = "label"
    }
}