package dev.farjad.driftlist.domain

/** Compact "how long ago" for notification rows: now, 5m, 3h, 2d. */
object RelativeTime {
    fun short(ageMillis: Long): String {
        val minutes = ageMillis / 60_000L
        return when {
            minutes < 1 -> "now"
            minutes < 60 -> "${minutes}m"
            minutes < 24 * 60 -> "${minutes / 60}h"
            else -> "${minutes / (24 * 60)}d"
        }
    }
}
