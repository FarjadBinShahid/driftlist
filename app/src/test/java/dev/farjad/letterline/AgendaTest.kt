package dev.farjad.letterline

import dev.farjad.letterline.domain.Agenda
import dev.farjad.letterline.domain.CalEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Locale

class AgendaTest {
    private val zone = ZoneId.of("Asia/Karachi") // UTC+5, so all-day UTC midnights fall mid-morning locally
    private val locale = Locale.US
    private val today = LocalDate.of(2026, 10, 12)

    private fun at(day: LocalDate, h: Int, m: Int = 0) = ZonedDateTime.of(day.atTime(h, m), zone).toInstant().toEpochMilli()
    private fun utcMidnight(day: LocalDate) = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private var nextId = 1L
    private fun timed(title: String, begin: Long, end: Long) = CalEvent(nextId++, nextId, title, begin, end, false, 0, 1)
    private fun allDay(title: String, first: LocalDate, daysLong: Long = 1) =
        CalEvent(nextId++, nextId, title, utcMidnight(first), utcMidnight(first.plusDays(daysLong)), true, 0, 1)

    @Test
    fun nextUpPrefersTheSoonestTimedEventOverAllDay() {
        val now = at(today, 9)
        val events = listOf(
            allDay("Birthday", today),
            timed("Lunch", at(today, 13), at(today, 14)),
            timed("Standup", at(today, 9, 30), at(today, 9, 45)),
        )
        assertEquals("Standup", Agenda.nextUp(events, now, zone)?.title)
    }

    @Test
    fun nextUpIgnoresLongOngoingEventsAndFallsBackToAllDay() {
        val now = at(today, 12)
        val conference = timed("Conference", at(today.minusDays(1), 8), at(today.plusDays(1), 18))
        val birthday = allDay("Birthday", today)
        assertEquals("Birthday", Agenda.nextUp(listOf(conference, birthday), now, zone)?.title)
        assertNull(Agenda.nextUp(listOf(conference, timed("Done", at(today, 8), at(today, 9))), now, zone))
    }

    @Test
    fun headerLineWording() {
        val now = at(today, 9)
        assertEquals("In 25 min · Standup", Agenda.headerLine(timed("Standup", at(today, 9, 25), at(today, 9, 40)), now, zone, true, locale))
        assertEquals("Until 09:30 · Call", Agenda.headerLine(timed("Call", at(today, 8, 45), at(today, 9, 30)), now, zone, true, locale))
        assertEquals("14:30 · Lunch", Agenda.headerLine(timed("Lunch", at(today, 14, 30), at(today, 15)), now, zone, true, locale))
        assertEquals("Tomorrow 7:00 AM · Gym", Agenda.headerLine(timed("Gym", at(today.plusDays(1), 7), at(today.plusDays(1), 8)), now, zone, false, locale))
        assertEquals("Today · (No title)", Agenda.headerLine(allDay(" ", today), now, zone, true, locale))
    }

    @Test
    fun allDayEventsUseTheirUtcDateNotTheLocalOne() {
        val (first, last) = Agenda.daysOf(allDay("Trip", today, daysLong = 3), zone)
        assertEquals(today, first)
        assertEquals(today.plusDays(2), last)
    }

    @Test
    fun groupByDaySpreadsMultiDayEventsAndPutsAllDayFirst() {
        val events = listOf(
            timed("Late flight", at(today, 23), at(today.plusDays(1), 2)),
            allDay("Holiday", today.plusDays(1)),
            timed("Old", at(today.minusDays(3), 9), at(today.minusDays(3), 10)),
            timed("Far away", at(today.plusDays(9), 9), at(today.plusDays(9), 10)),
        )
        val days = Agenda.groupByDay(events, today, 7, zone)
        assertEquals(listOf(today, today.plusDays(1)), days.map { it.first })
        assertEquals(listOf("Late flight"), days[0].second.map { it.title })
        assertEquals(listOf("Holiday", "Late flight"), days[1].second.map { it.title })
        val flight = days[1].second[1]
        assertEquals("From 23:00", Agenda.timeRange(flight, today, zone, true, locale))
        assertEquals("Until 02:00", Agenda.timeRange(flight, today.plusDays(1), zone, true, locale))
    }

    @Test
    fun dayLabels() {
        assertEquals("Today", Agenda.dayLabel(today, today, locale))
        assertEquals("Tomorrow", Agenda.dayLabel(today.plusDays(1), today, locale))
        assertEquals("Wednesday, October 14", Agenda.dayLabel(today.plusDays(2), today, locale))
    }

    @Test
    fun sameEventInTwoCalendarsShowsOnce() {
        val holiday = allDay("Durga Puja", today)
        val copy = holiday.copy(instanceId = 99, eventId = 99, calendarId = 2, title = "durga puja ")
        val other = allDay("Durga Puja", today.plusDays(1))
        val meeting = timed("Standup", at(today, 9), at(today, 10))
        val moved = meeting.copy(instanceId = 98, eventId = 98, calendarId = 2, begin = at(today, 11), end = at(today, 12))
        assertEquals(listOf(holiday, other, meeting, moved), Agenda.distinct(listOf(holiday, copy, other, meeting, moved)))
    }
}
