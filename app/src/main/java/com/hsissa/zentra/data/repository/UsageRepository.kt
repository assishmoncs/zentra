package com.hsissa.zentra.data.repository

import com.hsissa.zentra.data.datasource.UsageLocalDataSource
import com.hsissa.zentra.data.datasource.UsageStatsDataSource
import com.hsissa.zentra.domain.repository.UsageRepositoryContract
import com.hsissa.zentra.service.DailyUsageSummary
import com.hsissa.zentra.service.TodayUsageResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

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
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val results = mutableListOf<DailyUsageSummary>()

        for (i in 0 until 7) {
            val dayStart = calendar.timeInMillis
            val endCal = calendar.clone() as Calendar
            endCal.add(Calendar.DAY_OF_YEAR, 1)
            val dayEnd = if (i == 0) System.currentTimeMillis() else endCal.timeInMillis - 1

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

            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        results.reversed()
    }
}
