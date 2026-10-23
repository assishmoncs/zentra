package com.hsissa.zentra.service

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import android.util.Log
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.hsissa.zentra.core.SettingsManager
import com.hsissa.zentra.util.TimeFormatter
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles all UsageStatsManager interactions.
 * Fetches today's app usage and checks permission status.
 */
object UsageStatsHelper {

    private const val TAG = "UsageStatsHelper"
    private val appNameCache = ConcurrentHashMap<String, String>()
    private val systemAppCache = ConcurrentHashMap<String, Boolean>()

    /**
     * Returns true if the PACKAGE_USAGE_STATS permission has been granted.
     * Avoids synchronous UsageStatsManager IPC queries when called on the UI thread.
     */
    fun hasUsagePermission(context: Context): Boolean {
        val appOps = (context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager) ?: return false
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

        return mode == AppOpsManager.MODE_ALLOWED
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
                context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                    ?: return TodayUsageResult.Error

            val zoneId = ZoneId.systemDefault()
            val startLocalDate = LocalDate.now(zoneId).minusDays((daysCount - 1).coerceAtLeast(0).toLong())
            val startTime = startLocalDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val now = System.currentTimeMillis()

            val packageManager = context.packageManager
            val launcherPackage = getLauncherPackageName(packageManager)

            val aggregatedStats = mutableMapOf<String, Long>()

            if (daysCount == 1) {
                // For today, rely on Events for real-time accuracy and local midnight alignment.
                aggregatedStats.putAll(getUsageFromEvents(usageStatsManager, startTime, now))
            } else {
                // For multiple days, use queryAndAggregate which is efficient for historical data.
                usageStatsManager.queryAndAggregateUsageStats(startTime, now).forEach { (pkg, stats) ->
                    aggregatedStats[pkg] = stats.totalTimeInForeground
                }
            }

            val usageList = aggregatedStats.asSequence()
                .map { (packageName, totalTime) ->
                    val appName = resolveAppName(packageManager, packageName)
                    val isSystemOrLauncher = isSystemOrLauncherPackage(
                        packageManager,
                        packageName,
                        launcherPackage
                    )

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
                totalScreenTimeMillis = usageList.sumOf { it.totalTimeMillis },
                weightedScreenTimeMillis = nonSystemUsage.sumOf { (it.totalTimeMillis * it.category.scoreWeight).toLong() },
                topApps = nonSystemUsage.take(MAX_TOP_APPS),
                fullUsageList = nonSystemUsage,
                dayTimestamp = startTime
            )

            if (summary.totalScreenTimeMillis > 0L || summary.topApps.isNotEmpty()) {
                TodayUsageResult.Success(summary)
            } else {
                val isUnexpected = (now - startTime) > UNEXPECTED_EMPTY_THRESHOLD_MILLIS
                TodayUsageResult.Empty(summary, isUnexpected)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException querying usage stats", e)
            TodayUsageResult.Error
        } catch (e: RuntimeException) {
            Log.e(TAG, "RuntimeException querying usage stats", e)
            TodayUsageResult.Error
        }
    }

    /**
     * Fetches daily usage summary for a specific day.
     */
    fun getSummaryForDay(context: Context, dayStart: Long, dayEnd: Long): DailyUsageSummary {
        if (!hasUsagePermission(context)) return DailyUsageSummary.EMPTY

        return try {
            val usageStatsManager =
                context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                    ?: return DailyUsageSummary.EMPTY
            val packageManager = context.packageManager
            val launcherPackage = getLauncherPackageName(packageManager)

            val aggregatedStats = getUsageFromEvents(usageStatsManager, dayStart, dayEnd)

            val nonSystemUsage = aggregatedStats.filter { (pkg, time) ->
                time > 0 &&
                    pkg != context.packageName &&
                    !isSystemOrLauncherPackage(packageManager, pkg, launcherPackage)
            }

            val totalScreenTime = aggregatedStats.entries.asSequence()
                .filter { it.key != context.packageName && it.value > 0 }
                .sumOf { it.value }

            val topAppsList = nonSystemUsage.entries
                .map { (pkg, time) ->
                    val appName = resolveAppName(packageManager, pkg)
                    AppUsageInfo(
                        packageName = pkg,
                        appName = appName,
                        totalTimeMillis = time,
                        category = getCategory(context, pkg)
                    )
                }
                .sortedByDescending { it.totalTimeMillis }
                .take(MAX_TOP_APPS)

            DailyUsageSummary(
                totalScreenTimeMillis = totalScreenTime,
                weightedScreenTimeMillis = nonSystemUsage.entries.sumOf { (pkg, time) ->
                    (time * getCategory(context, pkg).scoreWeight).toLong()
                },
                topApps = topAppsList,
                dayTimestamp = dayStart
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compute summary for dayStart=$dayStart", e)
            DailyUsageSummary.EMPTY
        }
    }

    private fun isSystemOrLauncherPackage(
        packageManager: PackageManager,
        packageName: String,
        launcherPackage: String?
    ): Boolean {
        return isSystemApp(packageManager, packageName) ||
            packageName == launcherPackage ||
            packageName == "com.android.systemui" ||
            packageName == "android" ||
            packageName.lowercase().contains("launcher") ||
            packageName == "com.google.android.googlequicksearchbox"
    }

    private fun getUsageFromEvents(usm: UsageStatsManager, startTime: Long, endTime: Long): Map<String, Long> {
        val stats = mutableMapOf<String, Long>()
        val event = UsageEvents.Event()

        var currentApp: String? = null
        var lastTime = startTime

        // 1. Determine the active app at startTime by looking back up to 24h
        val preEvents = usm.queryEvents(startTime - 24 * 60 * 60 * 1000L, startTime)
        while (preEvents.hasNextEvent()) {
            preEvents.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> currentApp = event.packageName
                UsageEvents.Event.ACTIVITY_PAUSED -> if (currentApp == event.packageName) currentApp = null
                16, // SCREEN_NON_INTERACTIVE (API 28+)
                26, // DEVICE_SHUTDOWN
                27 -> currentApp = null
            }
        }

        // 2. Process events in range using a single-foreground-app model to prevent double counting
        val events = usm.queryEvents(startTime, endTime)
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val now = event.timeStamp

            // Abrupt shutdown recovery (DEVICE_STARTUP = 27)
            if (event.eventType == 27) {
                currentApp = null
                lastTime = maxOf(lastTime, now)
                continue
            }

            if (currentApp != null) {
                val duration = now - lastTime
                if (duration > 0) {
                    val app = currentApp
                    stats[app] = (stats[app] ?: 0L) + duration
                }
            }

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    currentApp = event.packageName
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    if (currentApp == event.packageName) {
                        currentApp = null
                    }
                }
                16, // SCREEN_NON_INTERACTIVE
                26 -> { // DEVICE_SHUTDOWN
                    currentApp = null
                }
            }
            lastTime = maxOf(lastTime, now)
        }

        // 3. Final session until the end of the requested period
        if (currentApp != null) {
            val duration = endTime - lastTime
            if (duration > 0) {
                val app = currentApp
                stats[app] = (stats[app] ?: 0L) + duration
            }
        }

        return stats
    }

    private fun isSystemApp(pm: PackageManager, packageName: String): Boolean {
        return systemAppCache.getOrPut(packageName) {
            try {
                if (packageName == "com.android.settings") {
                    false
                } else {
                    pm.getLaunchIntentForPackage(packageName) == null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to check launcher intent for $packageName", e)
                true
            }
        }
    }

    private fun resolveAppName(packageManager: PackageManager, packageName: String): String {
        return appNameCache.getOrPut(packageName) {
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (_: PackageManager.NameNotFoundException) {
                packageName
            }
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
        return SettingsManager.getInstance(context).getAppCategory(packageName)
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
    private const val UNEXPECTED_EMPTY_THRESHOLD_MILLIS = 15 * 60 * 1000L
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
    val fullUsageList: List<AppUsageInfo> = emptyList(),
    val dayTimestamp: Long = 0L
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
 * Annotated with @Keep and @SerializedName to remain safe under R8 obfuscation.
 */
@Keep
data class AppUsageInfo(
    @SerializedName("packageName") val packageName: String,
    @SerializedName("appName") val appName: String,
    @SerializedName("totalTimeMillis") val totalTimeMillis: Long,
    @SerializedName("category") val category: AppCategory = AppCategory.NEUTRAL
) {
    /** Convenience property: total usage time in minutes. */
    val totalTimeMinutes: Long get() = totalTimeMillis / 1000 / 60

    /** Formatted string like "1h 23m" or "45m". */
    val formattedTime: String
        get() = TimeFormatter.formatMillis(totalTimeMillis)
}

@Keep
enum class AppCategory(val displayName: String, val scoreWeight: Double) {
    @SerializedName("PRODUCTIVE") PRODUCTIVE("Productive", 0.2),
    @SerializedName("NEUTRAL") NEUTRAL("Neutral", 1.0),
    @SerializedName("DISTRACTING") DISTRACTING("Distracting", 2.0),
    @SerializedName("SYSTEM") SYSTEM("System", 0.0)
}
