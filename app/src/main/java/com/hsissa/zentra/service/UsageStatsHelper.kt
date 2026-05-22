package com.hsissa.zentra.service

import android.app.AppOpsManager
import android.app.usage.UsageEvents
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
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
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

            val packageManager = context.packageManager
            val launcherPackage = getLauncherPackageName(packageManager)

            // Accuracy fix: queryUsageStats/queryAndAggregateUsageStats for today is often stale 
            // or misattributed. queryEvents provides the most accurate real-time data for "today".
            val aggregatedStats = mutableMapOf<String, Long>()
            
            if (daysCount == 1) {
                // For today, rely heavily on Events for real-time accuracy.
                aggregatedStats.putAll(getUsageFromEvents(usageStatsManager, startTime, now))
                
                // Supplement with queryUsageStats for apps that were already open at startTime.
                val dailyStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, now)
                dailyStats?.forEach { stats ->
                    val pkg = stats.packageName
                    val time = stats.totalTimeInForeground
                    if (time > (aggregatedStats[pkg] ?: 0L)) {
                        aggregatedStats[pkg] = time
                    }
                }
            } else {
                // For multiple days, use queryAndAggregate which is efficient for historical data.
                usageStatsManager.queryAndAggregateUsageStats(startTime, now).forEach { (pkg, stats) ->
                    aggregatedStats[pkg] = stats.totalTimeInForeground
                }
            }

            val usageList = aggregatedStats.asSequence()
                .map { (packageName, totalTime) ->
                    val appName = resolveAppName(packageManager, packageName)
                    val isSystemOrLauncher = isSystemApp(packageManager, packageName) ||
                                           packageName == launcherPackage || 
                                           packageName == "com.android.systemui" ||
                                           packageName == "android" ||
                                           packageName.lowercase().contains("launcher") ||
                                           packageName == "com.google.android.googlequicksearchbox"
                    
                    AppUsageInfo(
                        packageName = packageName,
                        appName = appName,
                        totalTimeMillis = totalTime,
                        category = if (isSystemOrLauncher) AppCategory.SYSTEM else getCategory(context, packageName)
                    )
                }
                .filter { it.packageName != context.packageName }
                .filter { it.totalTimeMillis > 0 }
                .sortedByDescending { it.totalTimeMillis }
                .toList()

            val nonSystemUsage = usageList.filter { it.category != AppCategory.SYSTEM }

            val summary = DailyUsageSummary(
                totalScreenTimeMillis = nonSystemUsage.sumOf { it.totalTimeMillis },
                weightedScreenTimeMillis = nonSystemUsage.sumOf { (it.totalTimeMillis * it.category.scoreWeight).toLong() },
                topApps = nonSystemUsage.take(MAX_TOP_APPS),
                fullUsageList = nonSystemUsage
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

    /**
     * Fetches daily usage summary for the last N days, returned as a list per day.
     */
    fun getWeeklyTrend(context: Context): List<DailyUsageSummary> {
        if (!hasUsagePermission(context)) return emptyList()

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val results = mutableListOf<DailyUsageSummary>()
        
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        for (i in 0 until 7) {
            val dayStart = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val dayEnd = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, -1) // reset for next iteration

            // For historical days, queryUsageStats is usually fine
            val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, dayStart, dayEnd)
            val nonSystemUsage = stats.filter { 
                !isSystemApp(context.packageManager, it.packageName) && 
                it.packageName != context.packageName &&
                it.totalTimeInForeground > 0
            }

            results.add(DailyUsageSummary(
                totalScreenTimeMillis = nonSystemUsage.sumOf { it.totalTimeInForeground },
                weightedScreenTimeMillis = nonSystemUsage.sumOf { 
                    (it.totalTimeInForeground * getCategory(context, it.packageName).scoreWeight).toLong() 
                },
                topApps = emptyList() // We don't need top apps for the trend chart
            ))

            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        return results.reversed()
    }

    private fun getUsageFromEvents(usm: UsageStatsManager, startTime: Long, endTime: Long): Map<String, Long> {
        val events = usm.queryEvents(startTime, endTime)
        val stats = mutableMapOf<String, Long>()
        val startTimes = mutableMapOf<String, Long>()

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val packageName = event.packageName
            // Use both MOVE_TO (old) and ACTIVITY_ (new) events for maximum compatibility
            when (event.eventType) {
                1, 10 -> startTimes[packageName] = event.timeStamp // MOVE_TO_FOREGROUND, ACTIVITY_RESUMED
                2, 11 -> { // MOVE_TO_BACKGROUND, ACTIVITY_PAUSED
                    val start = startTimes.remove(packageName)
                    if (start != null) {
                        val duration = event.timeStamp - start
                        if (duration > 0) {
                            stats[packageName] = (stats[packageName] ?: 0L) + duration
                        }
                    }
                }
            }
        }
        
        // App currently in foreground
        for ((packageName, start) in startTimes) {
            val duration = endTime - start
            if (duration > 0) {
                stats[packageName] = (stats[packageName] ?: 0L) + duration
            }
        }
        
        return stats
    }

    private fun isSystemApp(pm: PackageManager, packageName: String): Boolean {
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (_: Exception) {
            false
        }
    }

    private fun resolveAppName(packageManager: PackageManager, packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    private fun getLauncherPackageName(packageManager: PackageManager): String? {
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_HOME)
        }
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName
    }

    private fun getCategory(context: Context, packageName: String): AppCategory {
        return com.hsissa.zentra.core.SettingsManager(context).getAppCategory(packageName)
            ?: when {
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
        "com.discord",
        "com.whatsapp"
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
    val fullUsageList: List<AppUsageInfo> = emptyList()
) {
    companion object {
        val EMPTY = DailyUsageSummary(
            totalScreenTimeMillis = 0L,
            weightedScreenTimeMillis = 0L,
            topApps = emptyList(),
            fullUsageList = emptyList()
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

enum class AppCategory(val displayName: String, val scoreWeight: Double) {
    PRODUCTIVE("Productive", 0.2), // Only 20% of time counts towards penalty
    NEUTRAL("Neutral", 1.0),       // 100% of time counts
    DISTRACTING("Distracting", 2.0), // 200% of time counts (double penalty)
    SYSTEM("System", 0.0)         // System apps don't count towards score
}
