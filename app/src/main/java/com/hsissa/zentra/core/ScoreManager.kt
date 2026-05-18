package com.hsissa.zentra.core

import com.hsissa.zentra.R
import kotlin.math.roundToInt

/**
 * Computes the life score based on total screen time.
 *
 * Uses a threshold-based penalty model:
 * - No penalty under a healthy daily target.
 * - Moderate penalty in the caution range.
 * - Steeper penalty for excessive usage.
 */
object ScoreManager {

    /**
     * Computes the life score from total screen time in milliseconds.
     *
     * @param totalScreenTimeMillis Total foreground usage today in milliseconds.
     * @return Score in range [0, 100].
     */
    fun computeScore(totalScreenTimeMillis: Long): Int {
        val minutes = totalScreenTimeMillis / 1000.0 / 60.0

        val penalty = when {
            minutes <= HEALTHY_LIMIT_MINUTES -> 0.0
            minutes <= CAUTION_LIMIT_MINUTES ->
                (minutes - HEALTHY_LIMIT_MINUTES) * CAUTION_PENALTY_PER_MINUTE
            else -> {
                val cautionPenalty =
                    (CAUTION_LIMIT_MINUTES - HEALTHY_LIMIT_MINUTES) * CAUTION_PENALTY_PER_MINUTE
                val excessPenalty =
                    (minutes - CAUTION_LIMIT_MINUTES) * EXCESS_PENALTY_PER_MINUTE
                cautionPenalty + excessPenalty
            }
        }

        return (MAX_SCORE - penalty).roundToInt().coerceIn(MIN_SCORE, MAX_SCORE)
    }

    /**
     * Returns a feedback message string resource ID based on the computed score.
     */
    fun getFeedbackResId(score: Int): Int {
        return when {
            isHighScore(score) -> R.string.feedback_high
            isMidScore(score) -> R.string.feedback_mid
            else       -> R.string.feedback_low
        }
    }

    fun isHighScore(score: Int): Boolean = score >= HIGH_SCORE_THRESHOLD

    fun isMidScore(score: Int): Boolean = score >= MID_SCORE_THRESHOLD

    private const val MIN_SCORE = 0
    private const val MAX_SCORE = 100
    private const val HEALTHY_LIMIT_MINUTES = 90.0
    private const val CAUTION_LIMIT_MINUTES = 240.0
    private const val CAUTION_PENALTY_PER_MINUTE = 0.25
    private const val EXCESS_PENALTY_PER_MINUTE = 0.35
    private const val HIGH_SCORE_THRESHOLD = 80
    private const val MID_SCORE_THRESHOLD = 50
}
