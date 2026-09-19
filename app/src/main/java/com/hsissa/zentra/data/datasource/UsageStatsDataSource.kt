package com.hsissa.zentra.data.datasource

import android.content.Context
import com.hsissa.zentra.service.DailyUsageSummary
import com.hsissa.zentra.service.TodayUsageResult
import com.hsissa.zentra.service.UsageStatsHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class UsageStatsDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getTodayUsage(): TodayUsageResult =
        UsageStatsHelper.getTodaySummaryResult(context)

    fun getSummaryForDay(dayStart: Long, dayEnd: Long): DailyUsageSummary =
        UsageStatsHelper.getSummaryForDay(context, dayStart, dayEnd)
}
