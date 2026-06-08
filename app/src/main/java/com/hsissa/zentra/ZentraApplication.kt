package com.hsissa.zentra

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.hsissa.zentra.service.MindfulnessWorker
import com.hsissa.zentra.util.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class ZentraApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Notification Channel
        NotificationHelper.createNotificationChannel(this)
        
        // Schedule Mindfulness Worker (every 1 hour)
        scheduleMindfulnessCheck()
    }

    private fun scheduleMindfulnessCheck() {
        val mindfulnessWorkRequest = PeriodicWorkRequestBuilder<MindfulnessWorker>(
            1, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "MindfulnessCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            mindfulnessWorkRequest
        )
    }
}
