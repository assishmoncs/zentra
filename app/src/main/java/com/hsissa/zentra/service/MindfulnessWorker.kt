package com.hsissa.zentra.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hsissa.zentra.core.AppLimitManager
import com.hsissa.zentra.core.ScoreManager
import com.hsissa.zentra.core.SettingsManager
import com.hsissa.zentra.data.local.AppDatabase
import com.hsissa.zentra.util.NotificationHelper
import com.hsissa.zentra.widget.ZentraWidgetProvider

class MindfulnessWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settingsManager = SettingsManager.getInstance(applicationContext)

        val result = UsageStatsHelper.getTodaySummaryResult(applicationContext)
        if (result is TodayUsageResult.Success) {
            val summary = result.summary

            // Keep cached Focus Score and Home Screen Widget synchronized in the background
            val score = ScoreManager.computeScore(summary.weightedScreenTimeMillis)
            settingsManager.setLastFocusScore(score)
            ZentraWidgetProvider.notifyWidgetDataChanged(applicationContext)

            // Respect configured Quiet Hours before dispatching mindfulness interventions
            if (settingsManager.isWithinQuietHours()) {
                return Result.success()
            }

            val limitManager = AppLimitManager(AppDatabase.getDatabase(applicationContext).usageDao())
            val exceededLimitApps = summary.fullUsageList.filter { appUsage ->
                limitManager.isLimitExceeded(appUsage.packageName, appUsage.totalTimeMillis)
            }

            val distractingApps = if (exceededLimitApps.isNotEmpty()) {
                exceededLimitApps
            } else {
                summary.topApps.filter {
                    it.category == AppCategory.DISTRACTING && it.totalTimeMinutes >= DEFAULT_DISTRACTING_THRESHOLD_MINUTES
                }
            }

            if (distractingApps.isNotEmpty()) {
                val primaryApp = distractingApps.first().appName
                val appNames = distractingApps.joinToString(", ") { it.appName }
                NotificationHelper.showMindfulnessNotification(
                    context = applicationContext,
                    title = "Mindfulness Check-in",
                    message = "You've spent significant time on $appNames today. Tap for a short mindful pause.",
                    exceededAppName = primaryApp
                )
            }
        }

        return Result.success()
    }

    companion object {
        private const val DEFAULT_DISTRACTING_THRESHOLD_MINUTES = 30
    }
}
