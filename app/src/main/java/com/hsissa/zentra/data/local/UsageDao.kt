package com.hsissa.zentra.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UsageDao {
    @Query("SELECT * FROM usage_records WHERE dayTimestamp = :dayTimestamp")
    suspend fun getUsageRecord(dayTimestamp: Long): UsageRecord?

    @Query("SELECT * FROM usage_records WHERE dayTimestamp >= :startTime ORDER BY dayTimestamp ASC")
    suspend fun getUsageRecords(startTime: Long): List<UsageRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageRecord(record: UsageRecord)

    // Focus Streaks
    @Query("SELECT * FROM focus_streaks WHERE id = 1")
    suspend fun getFocusStreak(): FocusStreakEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusStreak(streak: FocusStreakEntity)

    // App Limits
    @Query("SELECT * FROM app_limits WHERE isEnabled = 1")
    suspend fun getAllAppLimits(): List<AppLimitEntity>

    @Query("SELECT * FROM app_limits WHERE packageName = :packageName AND isEnabled = 1")
    suspend fun getAppLimit(packageName: String): AppLimitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppLimit(limit: AppLimitEntity)

    @Query("DELETE FROM app_limits WHERE packageName = :packageName")
    suspend fun deleteAppLimit(packageName: String)

    // Focus Sessions
    @Query("SELECT * FROM focus_sessions ORDER BY timestamp DESC")
    suspend fun getAllFocusSessions(): List<FocusSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusSession(session: FocusSessionEntity)
}
