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

    suspend fun getAllLimits(): List<AppLimitEntity> = usageDao.getAllAppLimitsIncludingDisabled()

    suspend fun setEnabled(limit: AppLimitEntity, enabled: Boolean) {
        usageDao.insertAppLimit(limit.copy(isEnabled = enabled))
    }

    suspend fun setLimit(packageName: String, limitMinutes: Int, isEnabled: Boolean = true) {
        require(packageName.isNotBlank() && limitMinutes in 1..1440)
        val entity = AppLimitEntity(packageName = packageName, limitMinutes = limitMinutes, isEnabled = isEnabled)
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
