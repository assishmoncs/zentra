package com.hsissa.zentra.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val durationMinutes: Int,
    val timestamp: Long,
    val completed: Boolean,
    val sessionType: String = "Pomodoro"
)
