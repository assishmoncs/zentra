package com.hsissa.zentra.data.datasource

import android.content.Context
import com.hsissa.zentra.service.DailyUsageSummary
import com.hsissa.zentra.service.TodayUsageResult
import com.hsissa.zentra.service.UsageStatsHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

open class UsageStatsDataSource @Inject constructor(
    @ApplicationContext private val context: Context?
) {
    open fun getTodayUsage(): TodayUsageResult =
        context?.let { UsageStatsHelper.getTodaySummaryResult(it) } ?: TodayUsageResult.Error

    open fun getSummaryForDay(dayStart: Long, dayEnd: Long): DailyUsageSummary =
        context?.let { UsageStatsHelper.getSummaryForDay(it, dayStart, dayEnd) } ?: DailyUsageSummary.EMPTY
}
