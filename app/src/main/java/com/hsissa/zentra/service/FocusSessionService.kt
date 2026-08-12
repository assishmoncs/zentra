package com.hsissa.zentra.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.hsissa.zentra.R
import com.hsissa.zentra.ui.MainActivity

class FocusSessionService : Service() {

    private val binder = LocalBinder()
    private var countDownTimer: CountDownTimer? = null
    var isRunning = false
        private set
    var remainingTimeMillis: Long = 0
        private set

    var onTickListener: ((Long) -> Unit)? = null
    var onFinishListener: (() -> Unit)? = null

    inner class LocalBinder : Binder() {
        fun getService(): FocusSessionService = this@FocusSessionService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    fun startSession(durationMinutes: Int) {
        countDownTimer?.cancel()
        val totalMs = durationMinutes * 60 * 1000L
        remainingTimeMillis = totalMs
        isRunning = true

        startForeground(NOTIFICATION_ID, buildNotification("Focus Session Started", formatTime(totalMs)))

        countDownTimer = object : CountDownTimer(totalMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTimeMillis = millisUntilFinished
                onTickListener?.invoke(millisUntilFinished)
                updateNotification("Focus Session in Progress", formatTime(millisUntilFinished))
            }

            override fun onFinish() {
                isRunning = false
                remainingTimeMillis = 0
                onFinishListener?.invoke()
                updateNotification("Focus Session Finished!", "Great job staying focused.")
                stopForeground(STOP_FOREGROUND_DETACH)
            }
        }.start()
    }

    fun stopSession() {
        countDownTimer?.cancel()
        isRunning = false
        remainingTimeMillis = 0
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus Session",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress during active focus sessions"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(title: String, content: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(title, content))
    }

    private fun formatTime(millis: Long): String {
        val minutes = (millis / 1000) / 60
        val seconds = (millis / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    companion object {
        const val CHANNEL_ID = "focus_session_channel"
        const val NOTIFICATION_ID = 2001
    }
}
