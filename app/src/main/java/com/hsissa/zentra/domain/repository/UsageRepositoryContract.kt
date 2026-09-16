package com.hsissa.zentra.domain.repository

import com.hsissa.zentra.service.DailyUsageSummary
import com.hsissa.zentra.service.TodayUsageResult

interface UsageRepositoryContract {
    suspend fun getTodayUsage(): TodayUsageResult
    suspend fun getWeeklyTrend(): List<DailyUsageSummary>
}
