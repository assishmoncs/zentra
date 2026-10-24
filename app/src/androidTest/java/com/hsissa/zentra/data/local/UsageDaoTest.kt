package com.hsissa.zentra.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UsageDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var usageDao: UsageDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        usageDao = database.usageDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetUsageRecord_replacesOnConflictAndOrdersByTimestamp() = runTest {
        val day1 = UsageRecord(1_700_000_000_000L, 60_000L, 60_000L, "[]")
        val day1Updated = UsageRecord(1_700_000_000_000L, 120_000L, 90_000L, "[]")
        val day2 = UsageRecord(1_700_086_400_000L, 180_000L, 180_000L, "[]")

        usageDao.insertUsageRecord(day1)
        usageDao.insertUsageRecord(day1Updated)
        usageDao.insertUsageRecord(day2)

        val fetchedDay1 = usageDao.getUsageRecord(1_700_000_000_000L)
        assertNotNull(fetchedDay1)
        assertEquals(120_000L, fetchedDay1?.totalScreenTimeMillis)

        val allRecords = usageDao.getUsageRecords(1_700_000_000_000L)
        assertEquals(2, allRecords.size)
        assertEquals(1_700_000_000_000L, allRecords[0].dayTimestamp)
        assertEquals(1_700_086_400_000L, allRecords[1].dayTimestamp)
    }

    @Test
    fun insertAndDeleteAppLimit_managesEnabledLimits() = runTest {
        val limit = AppLimitEntity("com.instagram.android", 45, isEnabled = true)
        usageDao.insertAppLimit(limit)

        assertEquals(limit, usageDao.getAppLimit("com.instagram.android"))
        assertEquals(1, usageDao.getAllAppLimits().size)

        usageDao.deleteAppLimit("com.instagram.android")
        assertNull(usageDao.getAppLimit("com.instagram.android"))
    }

    @Test
    fun insertAndGetFocusStreakAndSession_persistsFocusHistory() = runTest {
        val session = FocusSessionEntity(
            durationMinutes = 25,
            timestamp = 1_700_000_000_000L,
            completed = true,
            sessionType = "Pomodoro"
        )
        val streak = FocusStreakEntity(
            id = 1,
            currentStreak = 3,
            longestStreak = 5,
            totalFocusMinutes = 75,
            lastGoalMetTimestamp = 1_700_000_000_000L
        )

        usageDao.insertFocusSession(session)
        usageDao.insertFocusStreak(streak)

        val sessions = usageDao.getAllFocusSessions()
        assertEquals(1, sessions.size)
        assertEquals(25, sessions.first().durationMinutes)

        val savedStreak = usageDao.getFocusStreak()
        assertEquals(3, savedStreak?.currentStreak)
        assertEquals(75L, savedStreak?.totalFocusMinutes)
    }
}
