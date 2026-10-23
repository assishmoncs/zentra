package com.hsissa.zentra.core

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.hsissa.zentra.service.AppCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.GeneralSecurityException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages user preferences including custom app categories and goals.
 * Uses EncryptedSharedPreferences with automatic recovery from Keystore corruption
 * and an in-memory cache to avoid repeated cryptographic/disk lookups inside loops.
 */
@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val appContext: Context = context.applicationContext
    private val categoryCache = ConcurrentHashMap<String, AppCategory>()

    private val prefs: SharedPreferences by lazy {
        createEncryptedPrefsWithRecovery(appContext)
    }

    init {
        synchronized(LOCK) {
            if (INSTANCE == null) {
                INSTANCE = this
            }
        }
    }

    private fun createEncryptedPrefsWithRecovery(context: Context): SharedPreferences {
        return try {
            createEncryptedPrefs(context)
        } catch (e: GeneralSecurityException) {
            Log.e(TAG, "EncryptedSharedPreferences corrupted after restore/key invalidation; resetting", e)
            recoverCorruptedPrefs(context)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error initializing EncryptedSharedPreferences; resetting", e)
            recoverCorruptedPrefs(context)
        }
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun recoverCorruptedPrefs(context: Context): SharedPreferences {
        return try {
            context.deleteSharedPreferences(PREFS_NAME)
            createEncryptedPrefs(context)
        } catch (fallbackError: Exception) {
            Log.e(TAG, "Fallback to private SharedPreferences after Keystore failure", fallbackError)
            context.getSharedPreferences("${PREFS_NAME}_fallback", Context.MODE_PRIVATE)
        }
    }

    fun setAppCategory(packageName: String, category: AppCategory) {
        categoryCache[packageName] = category
        prefs.edit().putString(KEY_PREFIX_CATEGORY + packageName, category.name).apply()
    }

    fun getAppCategory(packageName: String): AppCategory? {
        categoryCache[packageName]?.let { return it }
        val name = prefs.getString(KEY_PREFIX_CATEGORY + packageName, null) ?: return null
        return try {
            AppCategory.valueOf(name).also { categoryCache[packageName] = it }
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Invalid stored AppCategory '$name' for $packageName", e)
            null
        }
    }

    fun setDailyGoal(goal: Int) {
        prefs.edit().putInt(KEY_DAILY_GOAL, goal.coerceIn(0, 100)).apply()
    }

    fun getDailyGoal(): Int {
        return prefs.getInt(KEY_DAILY_GOAL, DEFAULT_DAILY_GOAL)
    }

    fun setQuietHoursEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_QUIET_HOURS_ENABLED, enabled).apply()
    }

    fun isQuietHoursEnabled(): Boolean {
        return prefs.getBoolean(KEY_QUIET_HOURS_ENABLED, false)
    }

    fun setQuietHoursStart(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_QUIET_HOURS_START_HOUR, hour.coerceIn(0, 23))
            .putInt(KEY_QUIET_HOURS_START_MIN, minute.coerceIn(0, 59))
            .apply()
    }

    fun getQuietHoursStart(): Pair<Int, Int> {
        return Pair(
            prefs.getInt(KEY_QUIET_HOURS_START_HOUR, 22),
            prefs.getInt(KEY_QUIET_HOURS_START_MIN, 0)
        )
    }

    fun setQuietHoursEnd(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_QUIET_HOURS_END_HOUR, hour.coerceIn(0, 23))
            .putInt(KEY_QUIET_HOURS_END_MIN, minute.coerceIn(0, 59))
            .apply()
    }

    fun getQuietHoursEnd(): Pair<Int, Int> {
        return Pair(
            prefs.getInt(KEY_QUIET_HOURS_END_HOUR, 7),
            prefs.getInt(KEY_QUIET_HOURS_END_MIN, 0)
        )
    }

    companion object {
        private const val TAG = "SettingsManager"
        private const val PREFS_NAME = "zentra_settings"
        private const val KEY_PREFIX_CATEGORY = "cat_"
        private const val KEY_DAILY_GOAL = "daily_goal"
        private const val KEY_QUIET_HOURS_ENABLED = "quiet_hours_enabled"
        private const val KEY_QUIET_HOURS_START_HOUR = "quiet_hours_start_h"
        private const val KEY_QUIET_HOURS_START_MIN = "quiet_hours_start_m"
        private const val KEY_QUIET_HOURS_END_HOUR = "quiet_hours_end_h"
        private const val KEY_QUIET_HOURS_END_MIN = "quiet_hours_end_m"
        private const val DEFAULT_DAILY_GOAL = 80

        private val LOCK = Any()

        @Volatile
        private var INSTANCE: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(LOCK) {
                INSTANCE ?: SettingsManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
