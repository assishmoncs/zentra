package com.hsissa.zentra.data.datasource

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hsissa.zentra.data.local.UsageDao
import com.hsissa.zentra.data.local.UsageRecord
import com.hsissa.zentra.service.AppUsageInfo
import com.hsissa.zentra.service.DailyUsageSummary
import javax.inject.Inject

class UsageLocalDataSource @Inject constructor(
    private val usageDao: UsageDao,
    private val gson: Gson
) {
    suspend fun get(dayTimestamp: Long): UsageRecord? =
        usageDao.getUsageRecord(dayTimestamp)

    suspend fun save(summary: DailyUsageSummary) {
        if (summary.dayTimestamp == 0L) return

        usageDao.insertUsageRecord(
            UsageRecord(
                dayTimestamp = summary.dayTimestamp,
                totalScreenTimeMillis = summary.totalScreenTimeMillis,
                weightedScreenTimeMillis = summary.weightedScreenTimeMillis,
                topAppsJson = gson.toJson(summary.topApps)
            )
        )
    }

    fun decodeTopApps(record: UsageRecord): List<AppUsageInfo> {
        val type = object : TypeToken<List<AppUsageInfo>>() {}.type
        return try {
            gson.fromJson<List<AppUsageInfo>>(record.topAppsJson, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
