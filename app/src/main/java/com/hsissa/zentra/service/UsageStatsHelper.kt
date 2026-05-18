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
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
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
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Fetches today's usage summary.
     * Excludes the app itself and packages with zero usage.
     */
    fun getTodaySummary(context: Context): DailyUsageSummary {
        if (!hasUsagePermission(context)) return DailyUsageSummary.EMPTY

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

        return DailyUsageSummary(
            totalScreenTimeMillis = usageList.sumOf { it.totalTimeMillis },
            topApps = usageList.take(MAX_TOP_APPS),
        )
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
