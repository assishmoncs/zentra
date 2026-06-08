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
}
