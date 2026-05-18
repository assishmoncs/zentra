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
        return getSummaryResultForDays(context, 1)
    }

    /**
     * Fetches usage summary for the last N days.
     */
    fun getSummaryResultForDays(context: Context, daysCount: Int): TodayUsageResult {
        if (!hasUsagePermission(context)) return TodayUsageResult.Error

        return try {
            val usageStatsManager =
                context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (daysCount > 1) {
                    add(Calendar.DAY_OF_YEAR, -(daysCount - 1))
                }
            }
            val startTime = calendar.timeInMillis
            val now = System.currentTimeMillis()

            val statsMap: Map<String, UsageStats> = usageStatsManager.queryAndAggregateUsageStats(
                startTime,
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
                            category = getCategory(context, stats.packageName)
                        )
                    }
                }
                .sortedByDescending { it.totalTimeMillis }
                .toList()

            val summary = DailyUsageSummary(
                totalScreenTimeMillis = usageList.sumOf { it.totalTimeMillis },
                weightedScreenTimeMillis = usageList.sumOf { (it.totalTimeMillis * it.category.scoreWeight).toLong() },
                topApps = usageList.take(MAX_TOP_APPS),
            )

            if (summary.totalScreenTimeMillis > 0L || summary.topApps.isNotEmpty()) {
                TodayUsageResult.Success(summary)
            } else {
                val isUnexpected = (now - startTime) > UNEXPECTED_EMPTY_THRESHOLD_MILLIS
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

    private fun getCategory(context: Context, packageName: String): AppCategory {
        val custom = com.hsissa.zentra.core.SettingsManager(context).getAppCategory(packageName)
        if (custom != null) return custom

        return when {
            PRODUCTIVE_PACKAGES.any { packageName.contains(it) } -> AppCategory.PRODUCTIVE
            DISTRACTING_PACKAGES.any { packageName.contains(it) } -> AppCategory.DISTRACTING
            else -> AppCategory.NEUTRAL
        }
    }

    private val PRODUCTIVE_PACKAGES = setOf(
        "com.google.android.apps.docs",
        "com.google.android.apps.sheets",
        "com.google.android.apps.slides",
        "com.microsoft.office",
        "com.slack",
        "com.todoist",
        "com.notion.id",
        "com.evernote",
        "org.coursera.android",
        "com.duolingo",
        "com.google.android.calendar",
        "com.google.android.gm"
    )

    private val DISTRACTING_PACKAGES = setOf(
        "com.instagram.android",
        "com.facebook.katana",
        "com.twitter.android",
        "com.zhiliaoapp.musically", // TikTok
        "com.snapchat.android",
        "com.google.android.youtube",
        "com.netflix.mediaclient",
        "com.reddit.frontpage",
        "com.valvesoftware.android.steam.community",
        "com.discord"
    )

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
    val weightedScreenTimeMillis: Long,
    val topApps: List<AppUsageInfo>,
) {
    companion object {
        val EMPTY = DailyUsageSummary(
            totalScreenTimeMillis = 0L,
            weightedScreenTimeMillis = 0L,
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
    val totalTimeMillis: Long,
    val category: AppCategory = AppCategory.NEUTRAL
) {
    /** Convenience property: total usage time in minutes. */
    val totalTimeMinutes: Long get() = totalTimeMillis / 1000 / 60

    /** Formatted string like "1h 23m" or "45m". */
    val formattedTime: String
        get() = TimeFormatter.formatMillis(totalTimeMillis)
}

enum class AppCategory(val scoreWeight: Double) {
    PRODUCTIVE(0.2), // Only 20% of time counts towards penalty
    NEUTRAL(1.0),    // 100% of time counts
    DISTRACTING(2.0) // 200% of time counts (double penalty)
}
