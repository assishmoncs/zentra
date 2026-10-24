package com.hsissa.zentra.data.repository

import com.hsissa.zentra.data.datasource.UsageLocalDataSource
import com.hsissa.zentra.data.datasource.UsageStatsDataSource
import com.hsissa.zentra.domain.repository.UsageRepositoryContract
import com.hsissa.zentra.service.DailyUsageSummary
import com.hsissa.zentra.service.TodayUsageResult
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class UsageRepository @Inject constructor(
    private val usageStatsDataSource: UsageStatsDataSource,
    private val usageLocalDataSource: UsageLocalDataSource
) : UsageRepositoryContract {

    override suspend fun getTodayUsage(): TodayUsageResult = withContext(Dispatchers.IO) {
        val result = usageStatsDataSource.getTodayUsage()
        if (result is TodayUsageResult.Success) {
            usageLocalDataSource.save(result.summary)
        }
        result
    }

    override suspend fun getWeeklyTrend(): List<DailyUsageSummary> = withContext(Dispatchers.IO) {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val results = mutableListOf<DailyUsageSummary>()

        for (i in 0 until 7) {
            val targetDate = today.minusDays(i.toLong())
            val dayStart = targetDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val nextDayStart = targetDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val dayEnd = if (i == 0) System.currentTimeMillis() else nextDayStart - 1

            val cachedRecord = usageLocalDataSource.get(dayStart)

            if (i > 0 && cachedRecord != null) {
                val topApps = usageLocalDataSource.decodeTopApps(cachedRecord)
                results.add(
                    DailyUsageSummary(
                        totalScreenTimeMillis = cachedRecord.totalScreenTimeMillis,
                        weightedScreenTimeMillis = cachedRecord.weightedScreenTimeMillis,
                        topApps = topApps,
                        dayTimestamp = cachedRecord.dayTimestamp
                    )
                )
            } else {
                val systemSummary = usageStatsDataSource.getSummaryForDay(dayStart, dayEnd)
                if (i > 0 && systemSummary.totalScreenTimeMillis > 0) {
                    usageLocalDataSource.save(systemSummary)
                }
                results.add(systemSummary)
            }
        }

        results.reversed()
    }
}
