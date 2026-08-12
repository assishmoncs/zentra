package com.hsissa.zentra.core

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.hsissa.zentra.service.AppCategory

/**
 * Manages user preferences including custom app categories and goals.
 * Uses EncryptedSharedPreferences for secure storage of user settings.
 */
class SettingsManager(context: Context) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun setAppCategory(packageName: String, category: AppCategory) {
        prefs.edit().putString(KEY_PREFIX_CATEGORY + packageName, category.name).apply()
    }

    fun getAppCategory(packageName: String): AppCategory? {
        val name = prefs.getString(KEY_PREFIX_CATEGORY + packageName, null) ?: return null
        return try {
            AppCategory.valueOf(name)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun setDailyGoal(goal: Int) {
        prefs.edit().putInt(KEY_DAILY_GOAL, goal).apply()
    }

    fun getDailyGoal(): Int {
        return prefs.getInt(KEY_DAILY_GOAL, 80)
    }

    fun setQuietHoursEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_QUIET_HOURS_ENABLED, enabled).apply()
    }

    fun isQuietHoursEnabled(): Boolean {
        return prefs.getBoolean(KEY_QUIET_HOURS_ENABLED, false)
    }

    fun setQuietHoursStart(hour: Int, minute: Int) {
        prefs.edit().putInt(KEY_QUIET_HOURS_START_HOUR, hour)
            .putInt(KEY_QUIET_HOURS_START_MIN, minute).apply()
    }

    fun getQuietHoursStart(): Pair<Int, Int> {
        return Pair(
            prefs.getInt(KEY_QUIET_HOURS_START_HOUR, 22),
            prefs.getInt(KEY_QUIET_HOURS_START_MIN, 0)
        )
    }

    fun setQuietHoursEnd(hour: Int, minute: Int) {
        prefs.edit().putInt(KEY_QUIET_HOURS_END_HOUR, hour)
            .putInt(KEY_QUIET_HOURS_END_MIN, minute).apply()
    }

    fun getQuietHoursEnd(): Pair<Int, Int> {
        return Pair(
            prefs.getInt(KEY_QUIET_HOURS_END_HOUR, 7),
            prefs.getInt(KEY_QUIET_HOURS_END_MIN, 0)
        )
    }

    companion object {
        private const val PREFS_NAME = "zentra_settings"
        private const val KEY_PREFIX_CATEGORY = "cat_"
        private const val KEY_DAILY_GOAL = "daily_goal"
        private const val KEY_QUIET_HOURS_ENABLED = "quiet_hours_enabled"
        private const val KEY_QUIET_HOURS_START_HOUR = "quiet_hours_start_h"
        private const val KEY_QUIET_HOURS_START_MIN = "quiet_hours_start_m"
        private const val KEY_QUIET_HOURS_END_HOUR = "quiet_hours_end_h"
        private const val KEY_QUIET_HOURS_END_MIN = "quiet_hours_end_m"
    }
}
