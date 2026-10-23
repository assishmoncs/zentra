package com.hsissa.zentra.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Thread-safe utility for formatting time durations and dates using java.time.
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

    fun formatDate(timestamp: Long): String {
        val formatter = DateTimeFormatter.ofPattern("MMM dd", Locale.getDefault())
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }
}
