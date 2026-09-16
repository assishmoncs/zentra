package com.hsissa.zentra.domain.usecase

import com.hsissa.zentra.domain.repository.UsageRepositoryContract
import com.hsissa.zentra.service.DailyUsageSummary
import com.hsissa.zentra.service.TodayUsageResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Test

class GetTodayUsageUseCaseTest {

    @Test
    fun invoke_returnsRepositoryResult() = runTest {
        val expected = TodayUsageResult.Success(DailyUsageSummary.EMPTY)
        val repository = object : UsageRepositoryContract {
            override suspend fun getTodayUsage(): TodayUsageResult = expected
            override suspend fun getWeeklyTrend(): List<DailyUsageSummary> = emptyList()
        }

        val result = GetTodayUsageUseCase(repository)()

        assertSame(expected, result)
    }
}
