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
import java.time.LocalDate

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
            val limits = limitManager.getAllLimits().associateBy { it.packageName }
            val exceededLimitApps = summary.fullUsageList.filter { appUsage ->
                limits[appUsage.packageName]?.let { it.isEnabled && appUsage.totalTimeMillis >= it.limitMinutes * 60_000L } == true
            }

            val defaultApps = if (settingsManager.areDefaultRemindersEnabled()) {
                summary.fullUsageList.filter {
                    it.packageName !in limits && it.category == AppCategory.DISTRACTING && it.totalTimeMinutes >= settingsManager.getDefaultReminderMinutes()
                }
            } else emptyList()
            val distractingApps = exceededLimitApps + defaultApps

            val today = LocalDate.now().toString()
            distractingApps.filterNot { settingsManager.wasRemindedToday(it.packageName, today) }.forEach { app ->
                val sent = NotificationHelper.showMindfulnessNotification(
                    context = applicationContext,
                    title = "Mindfulness Check-in",
                    message = "You've spent significant time on ${app.appName} today. Tap for a short mindful pause.",
                    exceededAppName = app.appName
                )
                if (sent) settingsManager.markRemindedToday(app.packageName, today)
            }
        } else if (result is TodayUsageResult.Error || (result is TodayUsageResult.Empty && result.isUnexpected)) {
            settingsManager.clearLastFocusScore()
            ZentraWidgetProvider.notifyWidgetDataChanged(applicationContext)
        }

        return Result.success()
    }

}
