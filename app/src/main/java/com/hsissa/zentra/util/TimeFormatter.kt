package com.hsissa.zentra.util

/**
 * Utility for formatting time durations.
 */
object TimeFormatter {

    /**
     * Formats milliseconds into a human-readable string.
     * Examples: "2h 15m", "45m", "0m"
     */
    fun formatMillis(millis: Long): String {
        val safeMillis = millis.coerceAtLeast(0L)
        val totalMinutes = safeMillis / 1000 / 60
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }
}
