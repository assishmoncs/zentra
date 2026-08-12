package com.hsissa.zentra.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_limits")
data class AppLimitEntity(
    @PrimaryKey val packageName: String,
    val limitMinutes: Int,
    val isEnabled: Boolean = true
)
