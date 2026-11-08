package com.hsissa.zentra.core

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.hsissa.zentra.service.AppCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.GeneralSecurityException
import java.time.LocalTime
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages user preferences including custom app categories, goals, quiet hours, and cached widget score.
 * Uses EncryptedSharedPreferences and preserves the encrypted file on Keystore failures
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
            Log.e(TAG, "Secure preferences unavailable; preserving encrypted settings", e)
            fallbackPrefs(context)
        } catch (e: Exception) {
            Log.e(TAG, "Secure preferences unavailable; preserving encrypted settings", e)
            fallbackPrefs(context)
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

    @Volatile var usingFallbackPreferences: Boolean = false
        private set

    private fun fallbackPrefs(context: Context): SharedPreferences {
        // Preserve the encrypted file for later recovery instead of deleting user data.
        usingFallbackPreferences = true
        return context.getSharedPreferences("${PREFS_NAME}_fallback", Context.MODE_PRIVATE)
    }

    fun areDefaultRemindersEnabled(): Boolean = prefs.getBoolean("default_reminders", true)

    fun notificationPermissionRequested(): Boolean = prefs.getBoolean("notification_permission_requested", false)
    fun markNotificationPermissionRequested() { prefs.edit().putBoolean("notification_permission_requested", true).apply() }

    fun getDefaultReminderMinutes(): Int = prefs.getInt("default_reminder_minutes", 30).coerceIn(1, 1440)

    fun setDefaultReminderMinutes(minutes: Int) {
        prefs.edit().putInt("default_reminder_minutes", minutes.coerceIn(1, 1440)).apply()
    }

    fun setDefaultRemindersEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("default_reminders", enabled).apply()
    }

    fun wasRemindedToday(packageName: String, date: String): Boolean =
        prefs.getString("reminded_$packageName", null) == date

    fun markRemindedToday(packageName: String, date: String) {
        prefs.edit().putString("reminded_$packageName", date).apply()
    }

    fun setAppCategory(packageName: String, category: AppCategory) {
        categoryCache[packageName] = category
        prefs.edit().putString(KEY_PREFIX_CATEGORY + packageName, category.name).apply()
    }

    fun resetAppCategory(packageName: String) {
        categoryCache.remove(packageName)
        prefs.edit().remove(KEY_PREFIX_CATEGORY + packageName).apply()
    }

    fun clearLastFocusScore() { prefs.edit().remove(KEY_LAST_FOCUS_SCORE).apply() }

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

    fun setLastFocusScore(score: Int) {
        prefs.edit().putInt(KEY_LAST_FOCUS_SCORE, score.coerceIn(0, 100)).apply()
    }

    fun getLastFocusScore(): Int? {
        val value = prefs.getInt(KEY_LAST_FOCUS_SCORE, -1)
        return value.takeIf { it in 0..100 }
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

    /**
     * Checks whether the given LocalTime falls within the user's configured Quiet Hours window.
     * Supports overnight windows (e.g. 22:00 to 07:00) as well as same-day windows.
     */
    fun isWithinQuietHours(now: LocalTime = LocalTime.now()): Boolean {
        if (!isQuietHoursEnabled()) return false
        val (startH, startM) = getQuietHoursStart()
        val (endH, endM) = getQuietHoursEnd()
        val start = LocalTime.of(startH.coerceIn(0, 23), startM.coerceIn(0, 59))
        val end = LocalTime.of(endH.coerceIn(0, 23), endM.coerceIn(0, 59))

        return if (start == end) {
            true
        } else if (start < end) {
            !now.isBefore(start) && now.isBefore(end)
        } else {
            !now.isBefore(start) || now.isBefore(end)
        }
    }

    companion object {
        private const val TAG = "SettingsManager"
        private const val PREFS_NAME = "zentra_settings"
        private const val KEY_PREFIX_CATEGORY = "cat_"
        private const val KEY_DAILY_GOAL = "daily_goal"
        private const val KEY_LAST_FOCUS_SCORE = "last_focus_score"
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
