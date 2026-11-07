package com.hsissa.zentra.core

import java.time.LocalDate

object FocusStreakCalculator {
    fun next(current: Int, previousDay: LocalDate?, completedDay: LocalDate): Int = when (previousDay) {
        completedDay -> current.coerceAtLeast(1)
        completedDay.minusDays(1) -> current.coerceAtLeast(0) + 1
        else -> 1
    }
}
