package com.hsissa.zentra.ui.dashboard

import com.hsissa.zentra.domain.repository.UsageRepositoryContract
import com.hsissa.zentra.domain.usecase.GetTodayUsageUseCase
import com.hsissa.zentra.domain.usecase.GetWeeklyTrendUseCase
import com.hsissa.zentra.service.DailyUsageSummary
import com.hsissa.zentra.service.TodayUsageResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadData_success_publishesTodayAndWeeklyData() = runTest {
        val trend = listOf(summary(10L), summary(20L))
        val today = TodayUsageResult.Success(summary(30L))
        val repository = FakeRepository(today, trend)
        val viewModel = DashboardViewModel(
            GetTodayUsageUseCase(repository),
            GetWeeklyTrendUseCase(repository)
        )

        viewModel.loadData()
        advanceUntilIdle()

        assertFalse(viewModel.isLoading.value == true)
        assertEquals(today, viewModel.todayUsage.value)
        assertEquals(trend, viewModel.weeklyTrend.value)
    }

    private fun summary(time: Long) = DailyUsageSummary(
        totalScreenTimeMillis = time,
        weightedScreenTimeMillis = time,
        topApps = emptyList()
    )

    private class FakeRepository(
        private val today: TodayUsageResult,
        private val trend: List<DailyUsageSummary>
    ) : UsageRepositoryContract {

        override suspend fun getTodayUsage(): TodayUsageResult = today

        override suspend fun getWeeklyTrend(): List<DailyUsageSummary> = trend
    }
}
