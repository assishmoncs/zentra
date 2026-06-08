package com.hsissa.zentra.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_records")
data class UsageRecord(
    @PrimaryKey val dayTimestamp: Long, // Midnight of the day
    val totalScreenTimeMillis: Long,
    val weightedScreenTimeMillis: Long,
    val topAppsJson: String // Simple JSON storage for top apps for now
)
