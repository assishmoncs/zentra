package com.hsissa.zentra.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreManagerTest {

    @Test
    fun `computeScore returns 100 at healthy limit`() {
        val millis = minutesToMillis(90)
        assertEquals(100, ScoreManager.computeScore(millis))
    }

    @Test
    fun `computeScore decreases in caution range`() {
        val millis = minutesToMillis(180)
        assertEquals(78, ScoreManager.computeScore(millis))
    }

    @Test
    fun `computeScore decreases more in excess range`() {
        val millis = minutesToMillis(300)
        assertEquals(42, ScoreManager.computeScore(millis))
    }

    @Test
    fun `computeScore is clamped to zero`() {
        val millis = minutesToMillis(1_000)
        assertEquals(0, ScoreManager.computeScore(millis))
    }

    @Test
    fun `feedback bucket boundaries are stable`() {
        assertEquals(true, ScoreManager.isHighScore(80))
        assertEquals(false, ScoreManager.isHighScore(79))

        assertEquals(true, ScoreManager.isMidScore(50))
        assertEquals(false, ScoreManager.isMidScore(49))
    }

    private fun minutesToMillis(minutes: Long): Long = minutes * 60_000L
}
