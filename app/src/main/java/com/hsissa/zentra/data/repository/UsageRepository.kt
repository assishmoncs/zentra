package com.hsissa.zentra.data.repository

import android.content.Context
import com.hsissa.zentra.data.local.UsageDao
import com.hsissa.zentra.data.local.UsageRecord
import com.hsissa.zentra.service.DailyUsageSummary
import com.hsissa.zentra.service.TodayUsageResult
import com.hsissa.zentra.service.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hsissa.zentra.service.AppUsageInfo

class UsageRepository(
    private val context: Context,
    private val usageDao: UsageDao
) {
    suspend fun getTodayUsage(): TodayUsageResult = withContext(Dispatchers.IO) {
        val result = UsageStatsHelper.getTodaySummaryResult(context)
        if (result is TodayUsageResult.Success) {
            // Cache today's data (optional, maybe better at end of day)
            saveToCache(result.summary)
        }
        result
    }

    suspend fun getWeeklyTrend(): List<DailyUsageSummary> = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val gson = Gson()
        val results = mutableListOf<DailyUsageSummary>()

        // Look back 7 days
        for (i in 0 until 7) {
            val dayStart = calendar.timeInMillis
            val endCal = calendar.clone() as Calendar
            endCal.add(Calendar.DAY_OF_YEAR, 1)
            val dayEnd = if (i == 0) System.currentTimeMillis() else endCal.timeInMillis - 1

            val cachedRecord = usageDao.getUsageRecord(dayStart)

            if (i > 0 && cachedRecord != null) {
                // For past days, if cached, use the cached record directly.
                val type = object : TypeToken<List<AppUsageInfo>>() {}.type
                val topApps: List<AppUsageInfo> = try {
                    gson.fromJson(cachedRecord.topAppsJson, type) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }

                results.add(DailyUsageSummary(
                    totalScreenTimeMillis = cachedRecord.totalScreenTimeMillis,
                    weightedScreenTimeMillis = cachedRecord.weightedScreenTimeMillis,
                    topApps = topApps,
                    dayTimestamp = cachedRecord.dayTimestamp
                ))
            } else {
                // It's today, or a missing past day. Fetch from OS.
                val systemSummary = UsageStatsHelper.getSummaryForDay(context, dayStart, dayEnd)
                
                // Cache it if it's a past day (meaning it's complete)
                if (i > 0 && systemSummary.totalScreenTimeMillis > 0) {
                    saveToCache(systemSummary)
                }
                
                results.add(systemSummary)
            }

            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        
        results.reversed()
    }

    private suspend fun saveToCache(summary: DailyUsageSummary) {
        if (summary.dayTimestamp == 0L) return
        val record = UsageRecord(
            dayTimestamp = summary.dayTimestamp,
            totalScreenTimeMillis = summary.totalScreenTimeMillis,
            weightedScreenTimeMillis = summary.weightedScreenTimeMillis,
            topAppsJson = Gson().toJson(summary.topApps)
        )
        usageDao.insertUsageRecord(record)
    }
}
