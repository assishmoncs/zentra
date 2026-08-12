package com.hsissa.zentra.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_streaks")
data class FocusStreakEntity(
    @PrimaryKey val id: Int = 1,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalFocusMinutes: Long = 0,
    val lastGoalMetTimestamp: Long = 0
)
