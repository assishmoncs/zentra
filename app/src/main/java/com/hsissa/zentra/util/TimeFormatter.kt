package com.hsissa.zentra.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility for formatting time durations.
 */
object TimeFormatter {

    private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

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
        return dateFormat.format(Date(timestamp))
    }
}
