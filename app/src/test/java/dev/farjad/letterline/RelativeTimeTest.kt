package dev.farjad.letterline

import dev.farjad.letterline.domain.RelativeTime
import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeTimeTest {
    @Test
    fun formatsShortAges() {
        assertEquals("now", RelativeTime.short(0))
        assertEquals("now", RelativeTime.short(59_000))
        assertEquals("1m", RelativeTime.short(60_000))
        assertEquals("59m", RelativeTime.short(59 * 60_000L))
        assertEquals("1h", RelativeTime.short(60 * 60_000L))
        assertEquals("23h", RelativeTime.short(23 * 60 * 60_000L + 59 * 60_000L))
        assertEquals("2d", RelativeTime.short(2 * 24 * 60 * 60_000L))
    }
}
