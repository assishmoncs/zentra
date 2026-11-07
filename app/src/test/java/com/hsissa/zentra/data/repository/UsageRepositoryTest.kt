package com.hsissa.zentra.data.repository

import com.google.gson.Gson
import com.hsissa.zentra.data.datasource.UsageLocalDataSource
import com.hsissa.zentra.data.datasource.UsageStatsDataSource
import com.hsissa.zentra.data.local.AppLimitEntity
import com.hsissa.zentra.data.local.FocusSessionEntity
import com.hsissa.zentra.data.local.FocusStreakEntity
import com.hsissa.zentra.data.local.UsageDao
import com.hsissa.zentra.data.local.UsageRecord
import com.hsissa.zentra.service.DailyUsageSummary
import com.hsissa.zentra.service.TodayUsageResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class UsageRepositoryTest {

    @Test
    fun getWeeklyTrend_reconcilesPartialCachedYesterday() = runTest {
        val dao = FakeUsageDao()
        val local = UsageLocalDataSource(dao, Gson())
        val yesterday = LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        local.save(DailyUsageSummary(60_000L, 60_000L, emptyList(), dayTimestamp = yesterday))
        val source = object : UsageStatsDataSource(null) {
            override fun getSummaryForDay(dayStart: Long, dayEnd: Long) =
                DailyUsageSummary(180_000L, 180_000L, emptyList(), dayTimestamp = dayStart)
        }
        val trend = UsageRepository(source, local).getWeeklyTrend()
        assertEquals(180_000L, trend.first { it.dayTimestamp == yesterday }.totalScreenTimeMillis)
        assertEquals(180_000L, dao.getUsageRecord(yesterday)?.totalScreenTimeMillis)
    }

    @Test
    fun getWeeklyTrend_fallsBackOnlyToFinalizedHistory() = runTest {
        val dao = FakeUsageDao()
        val local = UsageLocalDataSource(dao, Gson())
        val zone = ZoneId.systemDefault()
        val yesterday = LocalDate.now().minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val twoDaysAgo = LocalDate.now().minusDays(2).atStartOfDay(zone).toInstant().toEpochMilli()
        local.save(DailyUsageSummary(60_000L, 60_000L, emptyList(), dayTimestamp = yesterday))
        local.save(DailyUsageSummary(120_000L, 120_000L, emptyList(), dayTimestamp = twoDaysAgo), isFinalized = true)
        val trend = UsageRepository(UsageStatsDataSource(null), local).getWeeklyTrend()
        assertEquals(false, trend.first { it.dayTimestamp == yesterday }.isAvailable)
        assertEquals(120_000L, trend.first { it.dayTimestamp == twoDaysAgo }.totalScreenTimeMillis)
    }

    @Test
    fun getTodayUsage_savesToLocalCacheOnlyOnSuccess() = runTest {
        val dao = FakeUsageDao()
        val localDataSource = UsageLocalDataSource(dao, Gson())
        val summary = DailyUsageSummary(
            totalScreenTimeMillis = 120_000L,
            weightedScreenTimeMillis = 120_000L,
            topApps = emptyList(),
            dayTimestamp = 1_700_000_000_000L
        )
        val statsDataSource = object : UsageStatsDataSource(null) {
            override fun getTodayUsage(): TodayUsageResult = TodayUsageResult.Success(summary)
        }

        val repository = UsageRepository(statsDataSource, localDataSource)
        val result = repository.getTodayUsage()

        assertEquals(TodayUsageResult.Success(summary), result)
        assertNotNull(dao.getUsageRecord(1_700_000_000_000L))
    }

    @Test
    fun getTodayUsage_doesNotCacheOnError() = runTest {
        val dao = FakeUsageDao()
        val localDataSource = UsageLocalDataSource(dao, Gson())
        val statsDataSource = object : UsageStatsDataSource(null) {
            override fun getTodayUsage(): TodayUsageResult = TodayUsageResult.Error
        }

        val repository = UsageRepository(statsDataSource, localDataSource)
        val result = repository.getTodayUsage()

        assertEquals(TodayUsageResult.Error, result)
        assertNull(dao.getUsageRecord(1_700_000_000_000L))
    }

    @Test
    fun getWeeklyTrend_returnsSevenChronologicalSummaries() = runTest {
        val dao = FakeUsageDao()
        val localDataSource = UsageLocalDataSource(dao, Gson())
        val statsDataSource = object : UsageStatsDataSource(null) {
            override fun getSummaryForDay(dayStart: Long, dayEnd: Long): DailyUsageSummary {
                return DailyUsageSummary(
                    totalScreenTimeMillis = 60_000L,
                    weightedScreenTimeMillis = 60_000L,
                    topApps = emptyList(),
                    dayTimestamp = dayStart
                )
            }
        }

        val repository = UsageRepository(statsDataSource, localDataSource)
        val trend = repository.getWeeklyTrend()

        assertEquals(7, trend.size)
        for (i in 0 until trend.size - 1) {
            assert(trend[i].dayTimestamp < trend[i + 1].dayTimestamp)
        }
    }

    private class FakeUsageDao : UsageDao {
        private val records = mutableMapOf<Long, UsageRecord>()

        override suspend fun getUsageRecord(dayTimestamp: Long): UsageRecord? = records[dayTimestamp]
        override suspend fun getUsageRecords(startTime: Long): List<UsageRecord> =
            records.values.filter { it.dayTimestamp >= startTime }
        override suspend fun insertUsageRecord(record: UsageRecord) {
            records[record.dayTimestamp] = record
        }
        override suspend fun getFocusStreak(): FocusStreakEntity? = null
        override suspend fun deleteUsageHistory() = Unit
        override suspend fun deleteFocusSessions() = Unit
        override suspend fun deleteFocusStreaks() = Unit
        override suspend fun insertFocusStreak(streak: FocusStreakEntity) = Unit
        override suspend fun getAllAppLimits(): List<AppLimitEntity> = emptyList()
        override suspend fun getAllAppLimitsIncludingDisabled(): List<AppLimitEntity> = emptyList()
        override suspend fun getAppLimit(packageName: String): AppLimitEntity? = null
        override suspend fun insertAppLimit(limit: AppLimitEntity) = Unit
        override suspend fun deleteAppLimit(packageName: String) = Unit
        override suspend fun getAllFocusSessions(): List<FocusSessionEntity> = emptyList()
        override suspend fun getFocusSession(id: Long): FocusSessionEntity? = null
        override suspend fun insertFocusSession(session: FocusSessionEntity) = Unit
    }
}
