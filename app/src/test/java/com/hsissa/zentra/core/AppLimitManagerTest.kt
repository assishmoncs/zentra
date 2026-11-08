package com.hsissa.zentra.core

import com.hsissa.zentra.data.local.AppLimitEntity
import com.hsissa.zentra.data.local.FocusSessionEntity
import com.hsissa.zentra.data.local.FocusStreakEntity
import com.hsissa.zentra.data.local.UsageDao
import com.hsissa.zentra.data.local.UsageRecord
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppLimitManagerTest {

    @Test
    fun pausedLimitIsNotExceededAndCanBeResumed() = runTest {
        val dao = FakeUsageDao()
        val manager = AppLimitManager(dao)
        manager.setLimit("example.app", 30)
        val limit = manager.getAllLimits().single()
        manager.setEnabled(limit, false)
        manager.setLimit("example.app", 45, isEnabled = false)
        assertFalse(manager.isLimitExceeded("example.app", 60 * 60_000L))
        manager.setEnabled(manager.getAllLimits().single(), true)
        assertTrue(manager.isLimitExceeded("example.app", 60 * 60_000L))
    }

    private lateinit var fakeDao: FakeUsageDao
    private lateinit var manager: AppLimitManager

    @Before
    fun setUp() {
        fakeDao = FakeUsageDao()
        manager = AppLimitManager(fakeDao)
    }

    @Test
    fun setAndGetLimits_persistsAndReturnsEnabledLimits() = runTest {
        manager.setLimit("com.instagram.android", 30)
        val limits = manager.getLimits()

        assertEquals(1, limits.size)
        assertEquals("com.instagram.android", limits.first().packageName)
        assertEquals(30, limits.first().limitMinutes)
    }

    @Test
    fun isLimitExceeded_returnsTrueOnlyWhenThresholdMetOrExceeded() = runTest {
        manager.setLimit("com.instagram.android", 30)

        assertFalse(manager.isLimitExceeded("com.instagram.android", 29 * 60 * 1000L))
        assertTrue(manager.isLimitExceeded("com.instagram.android", 30 * 60 * 1000L))
        assertTrue(manager.isLimitExceeded("com.instagram.android", 45 * 60 * 1000L))
        assertFalse(manager.isLimitExceeded("com.unknown.app", 120 * 60 * 1000L))
    }

    @Test
    fun removeLimit_deletesConfiguredLimit() = runTest {
        manager.setLimit("com.instagram.android", 30)
        manager.removeLimit("com.instagram.android")

        assertTrue(manager.getLimits().isEmpty())
    }

    private class FakeUsageDao : UsageDao {
        private val limits = mutableMapOf<String, AppLimitEntity>()

        override suspend fun getUsageRecord(dayTimestamp: Long): UsageRecord? = null
        override suspend fun getUsageRecords(startTime: Long): List<UsageRecord> = emptyList()
        override suspend fun insertUsageRecord(record: UsageRecord) = Unit
        override suspend fun getFocusStreak(): FocusStreakEntity? = null
        override suspend fun deleteUsageHistory() = Unit
        override suspend fun deleteFocusSessions() = Unit
        override suspend fun deleteFocusStreaks() = Unit
        override suspend fun insertFocusStreak(streak: FocusStreakEntity) = Unit
        override suspend fun getAllAppLimits(): List<AppLimitEntity> = limits.values.filter { it.isEnabled }
        override suspend fun getAllAppLimitsIncludingDisabled(): List<AppLimitEntity> = limits.values.toList()
        override suspend fun getAppLimit(packageName: String): AppLimitEntity? = limits[packageName]?.takeIf { it.isEnabled }
        override suspend fun insertAppLimit(limit: AppLimitEntity) {
            limits[limit.packageName] = limit
        }
        override suspend fun deleteAppLimit(packageName: String) {
            limits.remove(packageName)
        }
        override suspend fun getAllFocusSessions(): List<FocusSessionEntity> = emptyList()
        override suspend fun getFocusSession(id: Long): FocusSessionEntity? = null
        override suspend fun insertFocusSession(session: FocusSessionEntity) = Unit
    }
}
