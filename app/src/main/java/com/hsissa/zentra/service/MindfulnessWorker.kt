package com.hsissa.zentra.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hsissa.zentra.util.NotificationHelper

class MindfulnessWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val result = UsageStatsHelper.getTodaySummaryResult(applicationContext)
        
        if (result is TodayUsageResult.Success) {
            val summary = result.summary
            
            // Example logic: Notify if any distracting app is in top 3 and has > 30 mins
            val distractingApps = summary.topApps.filter { 
                it.category == AppCategory.DISTRACTING && it.totalTimeMinutes >= 30 
            }

            if (distractingApps.isNotEmpty()) {
                val appNames = distractingApps.joinToString(", ") { it.appName }
                NotificationHelper.showMindfulnessNotification(
                    applicationContext,
                    "Mindfulness Check-in",
                    "You've spent some time on $appNames today. Maybe take a short screen-free break?"
                )
            }
        }

        return Result.success()
    }
}
