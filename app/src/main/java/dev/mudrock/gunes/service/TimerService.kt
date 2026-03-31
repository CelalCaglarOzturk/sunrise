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
    
    private var timerSeconds: Int = 0
    private var remainingSeconds: Int = 0
    private var timerLabel: String = ""
    private var isTimerRunning: Boolean = false
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
                val totalSeconds = intent.getIntExtra(EXTRA_TOTAL_SECONDS, 0)
                val label = intent.getStringExtra(EXTRA_LABEL) ?: "Timer"
                startTimer(totalSeconds, label)
            }
            ACTION_STOP -> {
                stopTimer()
            }
            ACTION_FINISH -> {
                playFinishSound()
            }
        }
        return START_NOT_STICKY
    }

    fun startTimer(totalSeconds: Int, label: String) {
        timerSeconds = totalSeconds
        remainingSeconds = totalSeconds
        timerLabel = label
        isTimerRunning = true
        
        startForeground(NOTIFICATION_ID, createNotification(remainingSeconds, label, false))
        
        wakeLock?.acquire(10 * 60 * 1000L)
        
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isTimerRunning && remainingSeconds > 0) {
                delay(1000)
                remainingSeconds--
                updateNotification()
                onTickCallback?.invoke(remainingSeconds)
                
                if (remainingSeconds <= 0) {
                    onTimerFinish()
                    break
                }
            }
        }
    }

    fun pauseTimer() {
        isTimerRunning = false
        timerJob?.cancel()
        wakeLock?.release()
        updateNotification()
    }

    fun resumeTimer() {
        if (remainingSeconds > 0) {
            isTimerRunning = true
            wakeLock?.acquire(10 * 60 * 1000L)
            
            timerJob?.cancel()
            timerJob = scope.launch {
                while (isTimerRunning && remainingSeconds > 0) {
                    delay(1000)
                    remainingSeconds--
                    updateNotification()
                    onTickCallback?.invoke(remainingSeconds)
                    
                    if (remainingSeconds <= 0) {
                        onTimerFinish()
                        break
                    }
                }
            }
        }
    }

    fun stopTimer() {
        isTimerRunning = false
        timerJob?.cancel()
        remainingSeconds = timerSeconds
        stopMediaPlayer()
    }

    private fun onTimerFinish() {
        isTimerRunning = false
        wakeLock?.release()
        updateNotification()
        onFinishCallback?.invoke()
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

    fun setOnTickCallback(callback: (Int) -> Unit) {
        onTickCallback = callback
    }

    fun setOnFinishCallback(callback: () -> Unit) {
        onFinishCallback = callback
    }

    fun getRemainingSeconds(): Int = remainingSeconds
    fun isRunning(): Boolean = isTimerRunning

    private fun createNotification(remaining: Int, label: String, isFinished: Boolean): android.app.Notification {
        createNotificationChannel()
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NAVIGATE_TO", "timer")
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (isFinished) "$label - Finished!" else label)
            .setContentText(if (isFinished) "Timer completed" else formatTime(remaining))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(!isFinished)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)

        if (!isFinished) {
            builder.addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
        }

        return builder.build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(remainingSeconds, timerLabel, remainingSeconds <= 0))
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
        wakeLock?.release()
    }

    companion object {
        const val CHANNEL_ID = "timer_service_channel"
        const val NOTIFICATION_ID = 1001
        
        const val ACTION_START = "dev.mudrock.gunes.TIMER_START"
        const val ACTION_STOP = "dev.mudrock.gunes.TIMER_STOP"
        const val ACTION_FINISH = "dev.mudrock.gunes.TIMER_FINISH"
        
        const val EXTRA_TOTAL_SECONDS = "total_seconds"
        const val EXTRA_LABEL = "label"
    }
}