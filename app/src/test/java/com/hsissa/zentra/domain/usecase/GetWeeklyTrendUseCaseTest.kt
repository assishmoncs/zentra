package com.hsissa.zentra.domain.usecase

import com.hsissa.zentra.domain.repository.UsageRepositoryContract
import com.hsissa.zentra.service.DailyUsageSummary
import com.hsissa.zentra.service.TodayUsageResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetWeeklyTrendUseCaseTest {

    @Test
    fun invoke_returnsRepositoryTrend() = runBlocking {
        val expected = listOf(
            DailyUsageSummary(10L, 8L, emptyList(), dayTimestamp = 1L),
            DailyUsageSummary(20L, 16L, emptyList(), dayTimestamp = 2L)
        )
        val repository = object : UsageRepositoryContract {
            override suspend fun getTodayUsage(): TodayUsageResult = TodayUsageResult.Error
            override suspend fun getWeeklyTrend(): List<DailyUsageSummary> = expected
        }

        val result = GetWeeklyTrendUseCase(repository)()

        assertEquals(expected, result)
    }
}
