package com.hsissa.zentra.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatterTest {

    @Test
    fun `formatMillis returns minutes for values below one hour`() {
        assertEquals("45m", TimeFormatter.formatMillis(45 * 60_000L))
    }

    @Test
    fun `formatMillis returns hours and minutes for one hour or more`() {
        assertEquals("2h 15m", TimeFormatter.formatMillis(135 * 60_000L))
    }

    @Test
    fun `formatMillis handles zero and negative values safely`() {
        assertEquals("0m", TimeFormatter.formatMillis(0L))
        assertEquals("0m", TimeFormatter.formatMillis(-10_000L))
    }
}
