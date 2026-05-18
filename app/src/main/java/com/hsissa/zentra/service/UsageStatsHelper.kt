package com.hsissa.zentra.service

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import com.hsissa.zentra.util.TimeFormatter
import java.util.Calendar

/**
 * Handles all UsageStatsManager interactions.
 * Fetches today's app usage and checks permission status.
 */
object UsageStatsHelper {

    /**
     * Returns true if the PACKAGE_USAGE_STATS permission has been granted.
     */
    fun hasUsagePermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }

        if (mode != AppOpsManager.MODE_ALLOWED) return false

        return try {
            val usageStatsManager =
                context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return false
            val now = System.currentTimeMillis()
            usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - PROBE_WINDOW_MILLIS, now)
            true
        } catch (_: SecurityException) {
            false
        } catch (_: RuntimeException) {
            false
        }
    }

    /**
     * Fetches today's usage summary with explicit state to support loading/error UI.
     */
    fun getTodaySummaryResult(context: Context): TodayUsageResult {
        if (!hasUsagePermission(context)) return TodayUsageResult.Error

        return try {
            val usageStatsManager =
                context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = calendar.timeInMillis
            val now = System.currentTimeMillis()

            val statsMap: Map<String, UsageStats> = usageStatsManager.queryAndAggregateUsageStats(
                startOfDay,
                now
            )

            val packageManager = context.packageManager

            val usageList = statsMap.values.asSequence()
                .filter { it.totalTimeInForeground > 0 }
                .filter { it.packageName != context.packageName }
                .mapNotNull { stats ->
                    resolveAppName(packageManager, stats.packageName)?.let { appName ->
                        AppUsageInfo(
                            packageName = stats.packageName,
                            appName = appName,
                            totalTimeMillis = stats.totalTimeInForeground,
                        )
                    }
                }
                .sortedByDescending { it.totalTimeMillis }
                .toList()

            val summary = DailyUsageSummary(
                totalScreenTimeMillis = usageList.sumOf { it.totalTimeMillis },
                topApps = usageList.take(MAX_TOP_APPS),
            )

            if (summary.totalScreenTimeMillis > 0L || summary.topApps.isNotEmpty()) {
                TodayUsageResult.Success(summary)
            } else {
                val isUnexpected = (now - startOfDay) > UNEXPECTED_EMPTY_THRESHOLD_MILLIS
                TodayUsageResult.Empty(summary, isUnexpected)
            }
        } catch (_: SecurityException) {
            TodayUsageResult.Error
        } catch (_: RuntimeException) {
            TodayUsageResult.Error
        }
    }

    private fun resolveAppName(packageManager: PackageManager, packageName: String): String? {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    private const val MAX_TOP_APPS = 3
    private const val UNEXPECTED_EMPTY_THRESHOLD_MILLIS = 15 * 60 * 1000L // Treat empty data as expected for the first 15 minutes after local midnight (startOfDay).
    private const val PROBE_WINDOW_MILLIS = 60 * 60 * 1000L
}

sealed class TodayUsageResult {
    data class Success(val summary: DailyUsageSummary) : TodayUsageResult()
    data class Empty(val summary: DailyUsageSummary, val isUnexpected: Boolean) : TodayUsageResult()
    data object Error : TodayUsageResult()
}

data class DailyUsageSummary(
    val totalScreenTimeMillis: Long,
    val topApps: List<AppUsageInfo>,
) {
    companion object {
        val EMPTY = DailyUsageSummary(
            totalScreenTimeMillis = 0L,
            topApps = emptyList(),
        )
    }
}

/**
 * Simple data class representing one app's usage info.
 */
data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val totalTimeMillis: Long
) {
    /** Convenience property: total usage time in minutes. */
    val totalTimeMinutes: Long get() = totalTimeMillis / 1000 / 60

    /** Formatted string like "1h 23m" or "45m". */
    val formattedTime: String
        get() = TimeFormatter.formatMillis(totalTimeMillis)
}
