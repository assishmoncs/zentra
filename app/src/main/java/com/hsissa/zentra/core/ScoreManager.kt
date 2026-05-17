package com.hsissa.zentra.core

import com.hsissa.zentra.R

/**
 * Computes the life score based on total screen time.
 *
 * Formula: score = 100 - (wasted_minutes / 2)
 * Score is clamped between 0 and 100.
 */
object ScoreManager {

    /**
     * Computes the life score from total screen time in milliseconds.
     *
     * @param totalScreenTimeMillis Total foreground usage today in milliseconds.
     * @return Score in range [0, 100].
     */
    fun computeScore(totalScreenTimeMillis: Long): Int {
        val wastedMinutes = totalScreenTimeMillis / 1000 / 60
        val raw = 100 - (wastedMinutes / 2)
        return raw.toInt().coerceIn(0, 100)
    }

    /**
     * Returns a feedback message string resource ID based on the computed score.
     */
    fun getFeedbackResId(score: Int): Int {
        return when {
            score > 80 -> R.string.feedback_high
            score > 50 -> R.string.feedback_mid
            else       -> R.string.feedback_low
        }
    }
}
