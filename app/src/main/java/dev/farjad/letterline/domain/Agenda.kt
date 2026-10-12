package dev.farjad.letterline.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** One occurrence of a calendar event, as read from the system calendar (never stored). */
data class CalEvent(
    val instanceId: Long,
    val eventId: Long,
    val title: String,
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
    val color: Int,
    val calendarId: Long,
    val location: String? = null,
)

/** Pure rules for the header line and the agenda sheet. Android-free so they run as unit tests. */
object Agenda {
    private const val HOUR = 60 * 60 * 1000L

    /** Ongoing events this long (a conference, a trip) never hide what's coming up next. */
    private const val LONG_EVENT = 12 * HOUR

    val CalEvent.displayTitle: String get() = title.ifBlank { "(No title)" }

    /**
     * One row per event when it sits in several calendars (a holiday in both the phone's and the
     * Google calendar, a meeting invite copied across accounts): same title, ignoring case and
     * spaces, and the same start and end. The first copy wins.
     */
    fun distinct(events: List<CalEvent>): List<CalEvent> =
        events.distinctBy { listOf(it.title.trim().lowercase(), it.begin, it.end, it.allDay) }

    /** First and last day an event covers. All-day events are stored as UTC midnights, end exclusive. */
    fun daysOf(e: CalEvent, zone: ZoneId): Pair<LocalDate, LocalDate> = if (e.allDay) {
        val first = Instant.ofEpochMilli(e.begin).atZone(ZoneOffset.UTC).toLocalDate()
        val last = Instant.ofEpochMilli(maxOf(e.begin, e.end - 1)).atZone(ZoneOffset.UTC).toLocalDate()
        first to last
    } else {
        val first = Instant.ofEpochMilli(e.begin).atZone(zone).toLocalDate()
        val last = Instant.ofEpochMilli(maxOf(e.begin, e.end - 1)).atZone(zone).toLocalDate()
        first to last
    }

    /**
     * The event for the header: the soonest timed event that is on now or starts in the next 24 hours
     * (long ongoing ones excluded), else one of today's all-day events, else nothing.
     */
    fun nextUp(events: List<CalEvent>, nowMs: Long, zone: ZoneId): CalEvent? {
        val timed = events
            .filter { !it.allDay && it.end > nowMs && it.begin < nowMs + 24 * HOUR }
            .filterNot { it.begin <= nowMs && it.end - it.begin >= LONG_EVENT }
            .minByOrNull { it.begin }
        if (timed != null) return timed
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        return events.firstOrNull { e -> e.allDay && daysOf(e, zone).let { (a, b) -> today in a..b } }
    }

    fun time(ms: Long, zone: ZoneId, is24h: Boolean, locale: Locale = Locale.getDefault()): String =
        Instant.ofEpochMilli(ms).atZone(zone).format(DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "h:mm a", locale))

    /** "In 25 min · Standup", "Until 10:30 · Standup", "14:30 · Lunch", "Tomorrow 09:00 · Gym", "Today · Birthday". */
    fun headerLine(e: CalEvent, nowMs: Long, zone: ZoneId, is24h: Boolean, locale: Locale = Locale.getDefault()): String {
        val title = e.displayTitle
        if (e.allDay) return "Today · $title"
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        val day = Instant.ofEpochMilli(e.begin).atZone(zone).toLocalDate()
        val minutes = ((e.begin - nowMs) / 60_000L).toInt()
        val whenText = when {
            e.begin <= nowMs -> "Until ${time(e.end, zone, is24h, locale)}"
            minutes < 1 -> "Now"
            minutes < 60 -> "In $minutes min"
            day == today -> time(e.begin, zone, is24h, locale)
            else -> "Tomorrow ${time(e.begin, zone, is24h, locale)}"
        }
        return "$whenText · $title"
    }

    /** "09:00 – 09:30", "All day", or for an event running over several days, where it starts or ends. */
    fun timeRange(e: CalEvent, day: LocalDate, zone: ZoneId, is24h: Boolean, locale: Locale = Locale.getDefault()): String {
        if (e.allDay) return "All day"
        val (first, last) = daysOf(e, zone)
        return when {
            first == last -> "${time(e.begin, zone, is24h, locale)} – ${time(e.end, zone, is24h, locale)}"
            day == first -> "From ${time(e.begin, zone, is24h, locale)}"
            day == last -> "Until ${time(e.end, zone, is24h, locale)}"
            else -> "All day"
        }
    }

    /** Events grouped under each day they touch, from [today] for [days] days. Empty days are left out. */
    fun groupByDay(events: List<CalEvent>, today: LocalDate, days: Int, zone: ZoneId): List<Pair<LocalDate, List<CalEvent>>> {
        val lastDay = today.plusDays(days.toLong() - 1)
        val byDay = sortedMapOf<LocalDate, MutableList<CalEvent>>()
        for (e in events) {
            val (first, last) = daysOf(e, zone)
            var d = maxOf(first, today)
            val stop = minOf(last, lastDay)
            while (!d.isAfter(stop)) {
                byDay.getOrPut(d) { mutableListOf() } += e
                d = d.plusDays(1)
            }
        }
        // All-day first, then by start time.
        return byDay.map { (day, list) -> day to list.sortedWith(compareBy<CalEvent>({ !it.allDay }, { it.begin }, { it.displayTitle })) }
    }

    fun dayLabel(day: LocalDate, today: LocalDate, locale: Locale = Locale.getDefault()): String = when (day) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))
            .substringBeforeLast(day.year.toString()).trim().trimEnd(',')
    }
}
