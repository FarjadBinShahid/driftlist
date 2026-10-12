package dev.farjad.letterline.domain

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * When a held batch of notifications is delivered: at least 5.5 hours after it opened, rounded
 * up to the next full hour in local time (so somewhere between 5.5 and 6.5 hours, as described
 * in the reference's help). Computed in local time so a daylight-saving change still lands on
 * the hour.
 */
object SummarySchedule {
    const val MIN_DELAY_MS = 5L * 60 * 60 * 1000 + 30L * 60 * 1000

    fun nextDelivery(openedAtMs: Long, zone: ZoneId): Long {
        val earliest = Instant.ofEpochMilli(openedAtMs + MIN_DELAY_MS).atZone(zone)
        val floored = earliest.truncatedTo(ChronoUnit.HOURS)
        val onTheHour = if (floored == earliest) floored else floored.plusHours(1)
        return onTheHour.toInstant().toEpochMilli()
    }
}
