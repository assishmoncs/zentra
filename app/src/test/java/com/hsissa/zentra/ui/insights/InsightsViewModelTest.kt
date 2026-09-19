package com.hsissa.zentra.ui.insights

import com.hsissa.zentra.domain.repository.UsageRepositoryContract
import com.hsissa.zentra.domain.usecase.GetTodayUsageUseCase
import com.hsissa.zentra.domain.usecase.GetWeeklyTrendUseCase
import com.hsissa.zentra.service.DailyUsageSummary
import com.hsissa.zentra.service.TodayUsageResult
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

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
    fun loadInsights_success_publishesDataAndClearsError() = runTest {
        val trend = listOf(summary(30L), summary(60L))
        val today = TodayUsageResult.Success(summary(90L))
        val repository = FakeRepository(today = today, trend = trend)
        val viewModel = InsightsViewModel(
            GetTodayUsageUseCase(repository),
            GetWeeklyTrendUseCase(repository)
        )

        viewModel.loadInsights()
        advanceUntilIdle()

        assertFalse(viewModel.isLoading.value == true)
        assertFalse(viewModel.hasLoadError.value == true)
        assertEquals(trend, viewModel.weeklyTrend.value)
        assertEquals(today, viewModel.todayUsage.value)
    }

    @Test
    fun loadInsights_failure_publishesErrorAndStopsLoading() = runTest {
        val repository = FakeRepository(throwOnRead = true)
        val viewModel = InsightsViewModel(
            GetTodayUsageUseCase(repository),
            GetWeeklyTrendUseCase(repository)
        )

        viewModel.loadInsights()
        advanceUntilIdle()

        assertFalse(viewModel.isLoading.value == true)
        assertTrue(viewModel.hasLoadError.value == true)
    }

    @Test
    fun loadInsights_emptyHistory_keepsEmptyTrendWithoutError() = runTest {
        val repository = FakeRepository(
            today = TodayUsageResult.Empty(DailyUsageSummary.EMPTY, isUnexpected = false),
            trend = emptyList()
        )
        val viewModel = InsightsViewModel(
            GetTodayUsageUseCase(repository),
            GetWeeklyTrendUseCase(repository)
        )

        viewModel.loadInsights()
        advanceUntilIdle()

        assertFalse(viewModel.hasLoadError.value == true)
        assertTrue(viewModel.weeklyTrend.value.orEmpty().isEmpty())
        assertTrue(viewModel.todayUsage.value is TodayUsageResult.Empty)
    }

    private fun summary(time: Long): DailyUsageSummary = DailyUsageSummary(
        totalScreenTimeMillis = time,
        weightedScreenTimeMillis = time,
        topApps = emptyList()
    )

    private class FakeRepository(
        private val today: TodayUsageResult = TodayUsageResult.Success(DailyUsageSummary.EMPTY),
        private val trend: List<DailyUsageSummary> = emptyList(),
        private val throwOnRead: Boolean = false
    ) : UsageRepositoryContract {

        override suspend fun getTodayUsage(): TodayUsageResult {
            check(!throwOnRead) { "test failure" }
            return today
        }

        override suspend fun getWeeklyTrend(): List<DailyUsageSummary> {
            check(!throwOnRead) { "test failure" }
            return trend
        }
    }
}
