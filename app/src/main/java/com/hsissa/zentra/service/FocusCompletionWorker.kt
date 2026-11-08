package com.hsissa.zentra.service

import android.content.Context
import android.content.Intent
import android.content.ComponentName
import android.app.PendingIntent
import android.service.quicksettings.TileService
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hsissa.zentra.R
import com.hsissa.zentra.ui.MainActivity

class FocusCompletionWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getLong("session_id", 0L)
        val store = FocusSessionStore(applicationContext)
        val session = store.active() ?: return Result.success()
        if (session.id != id) return Result.success()
        if (System.currentTimeMillis() < session.end) return Result.retry()
        return try {
            val completed = store.complete(id)
            TileService.requestListeningState(applicationContext, ComponentName(applicationContext, FocusTileService::class.java))
            if (completed && !FocusSessionService.isSessionActive) {
                val notifications = NotificationManagerCompat.from(applicationContext)
                if (notifications.areNotificationsEnabled()) {
                    val intent = PendingIntent.getActivity(applicationContext, 0,
                        Intent(applicationContext, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    val notification = NotificationCompat.Builder(applicationContext, FocusSessionService.CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle(if (session.isBreak) "Break Finished!" else "Focus Session Finished!")
                        .setContentText(if (session.isBreak) "Your break is complete." else "Great job staying focused.")
                        .setContentIntent(intent).setAutoCancel(true).build()
                    try { notifications.notify(FocusSessionService.NOTIFICATION_ID, notification) } catch (_: SecurityException) { }
                }
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
