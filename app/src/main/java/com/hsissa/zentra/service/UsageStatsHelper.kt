package com.hsissa.zentra.service

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
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
     * Fetches today's usage stats sorted by total foreground time descending.
     * Excludes the app itself and system/internal packages with zero usage.
     *
     * @return List of [AppUsageInfo] for apps used today.
     */
    fun getTodayUsage(context: Context): List<AppUsageInfo> {
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

        return statsMap.values.asSequence()
            .filter { it.totalTimeInForeground > 0 }
            .filter { it.packageName != context.packageName }
            .mapNotNull { stats ->
                val appName = try {
                    val appInfo = packageManager.getApplicationInfo(stats.packageName, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (_: PackageManager.NameNotFoundException) {
                    null // Skip packages that can't be resolved
                }
                appName?.let {
                    AppUsageInfo(
                        packageName = stats.packageName,
                        appName = it,
                        totalTimeMillis = stats.totalTimeInForeground,
                    )
                }
            }
            .sortedByDescending { it.totalTimeMillis }
            .toList()
    }

    /**
     * Returns the total screen time today in milliseconds.
     */
    fun getTotalScreenTimeMillis(context: Context): Long {
        return getTodayUsage(context).sumOf { it.totalTimeMillis }
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
        get() {
            val minutes = totalTimeMinutes
            val hours = minutes / 60
            val mins = minutes % 60
            return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
        }
}
