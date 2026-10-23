package com.hsissa.zentra.data.datasource

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hsissa.zentra.data.local.UsageDao
import com.hsissa.zentra.data.local.UsageRecord
import com.hsissa.zentra.service.AppUsageInfo
import com.hsissa.zentra.service.DailyUsageSummary
import javax.inject.Inject

open class UsageLocalDataSource @Inject constructor(
    private val usageDao: UsageDao,
    private val gson: Gson
) {
    open suspend fun get(dayTimestamp: Long): UsageRecord? =
        usageDao.getUsageRecord(dayTimestamp)

    open suspend fun save(summary: DailyUsageSummary) {
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

    open fun decodeTopApps(record: UsageRecord): List<AppUsageInfo> {
        val type = object : TypeToken<List<AppUsageInfo>>() {}.type
        return try {
            gson.fromJson<List<AppUsageInfo>>(record.topAppsJson, type) ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode topAppsJson for dayTimestamp=${record.dayTimestamp}", e)
            emptyList()
        }
    }

    companion object {
        private const val TAG = "UsageLocalDataSource"
    }
}
