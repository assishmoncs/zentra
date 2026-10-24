package com.hsissa.zentra.data.datasource

import com.google.gson.Gson
import com.hsissa.zentra.data.local.AppLimitEntity
import com.hsissa.zentra.data.local.FocusSessionEntity
import com.hsissa.zentra.data.local.FocusStreakEntity
import com.hsissa.zentra.data.local.UsageDao
import com.hsissa.zentra.data.local.UsageRecord
import com.hsissa.zentra.service.AppCategory
import com.hsissa.zentra.service.AppUsageInfo
import com.hsissa.zentra.service.DailyUsageSummary
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UsageLocalDataSourceTest {

    private lateinit var fakeDao: InMemoryUsageDao
    private lateinit var dataSource: UsageLocalDataSource

    @Before
    fun setUp() {
        fakeDao = InMemoryUsageDao()
        dataSource = UsageLocalDataSource(fakeDao, Gson())
    }

    @Test
    fun save_skipsSummaryWhenDayTimestampIsZero() = runTest {
        val summary = DailyUsageSummary(
            totalScreenTimeMillis = 60_000L,
            weightedScreenTimeMillis = 60_000L,
            topApps = emptyList(),
            dayTimestamp = 0L
        )
        dataSource.save(summary)
        assertNull(dataSource.get(0L))
    }

    @Test
    fun saveAndDecodeTopApps_roundTripsAppUsageList() = runTest {
        val apps = listOf(
            AppUsageInfo("com.slack", "Slack", 120_000L, AppCategory.PRODUCTIVE),
            AppUsageInfo("com.instagram.android", "Instagram", 300_000L, AppCategory.DISTRACTING)
        )
        val summary = DailyUsageSummary(
            totalScreenTimeMillis = 420_000L,
            weightedScreenTimeMillis = 624_000L,
            topApps = apps,
            dayTimestamp = 1_700_000_000_000L
        )

        dataSource.save(summary)
        val record = dataSource.get(1_700_000_000_000L)!!
        val decoded = dataSource.decodeTopApps(record)

        assertEquals(apps, decoded)
    }

    @Test
    fun decodeTopApps_malformedJson_returnsEmptyListSafely() {
        val corruptedRecord = UsageRecord(
            dayTimestamp = 1_700_000_000_000L,
            totalScreenTimeMillis = 100L,
            weightedScreenTimeMillis = 100L,
            topAppsJson = "{not-valid-json-array"
        )

        val decoded = dataSource.decodeTopApps(corruptedRecord)
        assertTrue(decoded.isEmpty())
    }

    private class InMemoryUsageDao : UsageDao {
        private val records = mutableMapOf<Long, UsageRecord>()

        override suspend fun getUsageRecord(dayTimestamp: Long): UsageRecord? = records[dayTimestamp]
        override suspend fun getUsageRecords(startTime: Long): List<UsageRecord> =
            records.values.filter { it.dayTimestamp >= startTime }.sortedBy { it.dayTimestamp }
        override suspend fun insertUsageRecord(record: UsageRecord) {
            records[record.dayTimestamp] = record
        }
        override suspend fun getFocusStreak(): FocusStreakEntity? = null
        override suspend fun insertFocusStreak(streak: FocusStreakEntity) = Unit
        override suspend fun getAllAppLimits(): List<AppLimitEntity> = emptyList()
        override suspend fun getAppLimit(packageName: String): AppLimitEntity? = null
        override suspend fun insertAppLimit(limit: AppLimitEntity) = Unit
        override suspend fun deleteAppLimit(packageName: String) = Unit
        override suspend fun getAllFocusSessions(): List<FocusSessionEntity> = emptyList()
        override suspend fun insertFocusSession(session: FocusSessionEntity) = Unit
    }
}
