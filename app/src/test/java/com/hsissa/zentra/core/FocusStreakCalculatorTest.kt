package com.hsissa.zentra.core

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class FocusStreakCalculatorTest {
    private val today = LocalDate.of(2026, 11, 7)

    @Test fun sameDayDoesNotIncrement() = assertEquals(3, FocusStreakCalculator.next(3, today, today))
    @Test fun yesterdayIncrements() = assertEquals(4, FocusStreakCalculator.next(3, today.minusDays(1), today))
    @Test fun missedDayRestarts() = assertEquals(1, FocusStreakCalculator.next(3, today.minusDays(2), today))
    @Test fun firstSessionStartsStreak() = assertEquals(1, FocusStreakCalculator.next(0, null, today))
}
