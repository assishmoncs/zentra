package com.hsissa.zentra.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.hsissa.zentra.service.DailyUsageSummary

class ScoreManagerTest {

    @Test
    fun averageDailyScore_averagesScoresAndIgnoresUnavailableDays() {
        val low = DailyUsageSummary(0, 300 * 60_000L, emptyList())
        val high = DailyUsageSummary(0, 0, emptyList())
        val unavailable = DailyUsageSummary.EMPTY
        assertEquals(71, ScoreManager.averageDailyScore(listOf(low, high, unavailable)))
        assertEquals(null, ScoreManager.averageDailyScore(listOf(unavailable)))
    }

    @Test
    fun computeScore_zeroUsage_returnsMaxScore() {
        val score = ScoreManager.computeScore(0L)
        assertEquals(100, score)
    }

    @Test
    fun computeScore_healthyLimit_returnsMaxScore() {
        // 90 minutes = 90 * 60 * 1000 ms
        val score = ScoreManager.computeScore(90 * 60 * 1000L)
        assertEquals(100, score)
    }

    @Test
    fun computeScore_cautionLimit_appliesCautionPenalty() {
        // 120 minutes (30 mins excess) -> penalty = 30 * 0.25 = 7.5 -> 92
        val score = ScoreManager.computeScore(120 * 60 * 1000L)
        assertEquals(93, score)
    }

    @Test
    fun computeScore_excessiveUsage_appliesSteepPenalty() {
        // 300 minutes -> 150m caution penalty + 60m excess penalty = 37.5 + 21 = 58.5 -> score 42
        val score = ScoreManager.computeScore(300 * 60 * 1000L)
        assertTrue(score < 50)
    }
}
