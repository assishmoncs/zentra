package com.hsissa.zentra.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.hsissa.zentra.R
import com.hsissa.zentra.ui.MindfulnessOverlayActivity

object NotificationHelper {
    private const val TAG = "NotificationHelper"
    private const val CHANNEL_ID = "mindfulness_alerts"
    private const val CHANNEL_NAME = "Mindfulness Alerts"
    private const val CHANNEL_DESC = "Notifications to help you stay mindful of your digital usage."

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showMindfulnessNotification(
        context: Context,
        title: String,
        message: String,
        exceededAppName: String? = null
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }

        val overlayIntent = Intent(context, MindfulnessOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!exceededAppName.isNullOrBlank()) {
                putExtra(MindfulnessOverlayActivity.EXTRA_APP_NAME, exceededAppName)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            exceededAppName?.hashCode() ?: 0,
            overlayIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                if (!areNotificationsEnabled()) return false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                        .getNotificationChannel(CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE) return false
                notify(exceededAppName?.hashCode() ?: 0, builder.build())
            }
            return true
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission revoked before notify()", e)
            return false
        }
    }
}
