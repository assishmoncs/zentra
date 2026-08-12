package com.hsissa.zentra.core

import com.hsissa.zentra.data.local.AppLimitEntity
import com.hsissa.zentra.data.local.UsageDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLimitManager @Inject constructor(
    private val usageDao: UsageDao
) {
    suspend fun getLimits(): List<AppLimitEntity> {
        return usageDao.getAllAppLimits()
    }

    suspend fun setLimit(packageName: String, limitMinutes: Int) {
        val entity = AppLimitEntity(packageName = packageName, limitMinutes = limitMinutes, isEnabled = true)
        usageDao.insertAppLimit(entity)
    }

    suspend fun removeLimit(packageName: String) {
        usageDao.deleteAppLimit(packageName)
    }

    suspend fun isLimitExceeded(packageName: String, currentUsageMillis: Long): Boolean {
        val limit = usageDao.getAppLimit(packageName) ?: return false
        val currentMinutes = currentUsageMillis / 1000 / 60
        return currentMinutes >= limit.limitMinutes
    }
}
