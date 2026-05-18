package com.hsissa.zentra.core

import android.content.Context
import android.content.SharedPreferences
import com.hsissa.zentra.service.AppCategory

/**
 * Manages user preferences including custom app categories and goals.
 */
class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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

    companion object {
        private const val PREFS_NAME = "zentra_settings"
        private const val KEY_PREFIX_CATEGORY = "cat_"
        private const val KEY_DAILY_GOAL = "daily_goal"
    }
}
