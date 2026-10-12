package dev.farjad.letterline

import dev.farjad.letterline.domain.SummarySchedule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class SummaryScheduleTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private fun at(h: Int, m: Int, day: Int = 15, month: Int = 6) = ZonedDateTime.of(2026, month, day, h, m, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun roundsUpToTheHourAfterFiveAndAHalfHours() {
        // opened 09:10 -> earliest 14:40 -> delivered 15:00
        assertEquals(at(15, 0), SummarySchedule.nextDelivery(at(9, 10), zone))
        // opened 09:30 -> earliest exactly 15:00 -> stays 15:00
        assertEquals(at(15, 0), SummarySchedule.nextDelivery(at(9, 30), zone))
        // opened 09:31 -> earliest 15:01 -> 16:00
        assertEquals(at(16, 0), SummarySchedule.nextDelivery(at(9, 31), zone))
    }

    @Test
    fun landsOnTheLocalHourAcrossDaylightSaving() {
        // 2026-10-25 03:00 CEST -> 02:00 CET in Berlin. Opened 22:45 on the 24th:
        // earliest = 04:15 local (5.5 h of real time later, one hour repeated) -> 05:00 local.
        val opened = ZonedDateTime.of(2026, 10, 24, 22, 45, 0, 0, zone).toInstant().toEpochMilli()
        val delivered = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(SummarySchedule.nextDelivery(opened, zone)), zone)
        assertEquals(0, delivered.minute)
        assertEquals(0, delivered.second)
        assertEquals(true, delivered.toInstant().toEpochMilli() - opened >= SummarySchedule.MIN_DELAY_MS)
        assertEquals(true, delivered.toInstant().toEpochMilli() - opened < SummarySchedule.MIN_DELAY_MS + 60 * 60 * 1000)
    }
}
