package dev.farjad.driftlist

import dev.farjad.driftlist.data.db.AppEntity
import dev.farjad.driftlist.domain.AppSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppSearchTest {
    private fun app(label: String, launches: Int = 0) = AppEntity(
        id = label, packageName = "p.$label", activityClass = "A", userSerial = 0,
        systemLabel = label, sortKey = label.lowercase(), bucket = label.take(1).uppercase(),
        installedAt = 0, launchCount = launches, createdAt = 0, updatedAt = 0,
    )

    @Test
    fun scoresByMatchQuality() {
        assertEquals(0, AppSearch.score("maps", "Maps"))
        assertEquals(1, AppSearch.score("ma", "Maps"))
        assertEquals(2, AppSearch.score("map", "Google Maps"))
        assertEquals(3, AppSearch.score("gm", "Google Maps"))
        assertEquals(4, AppSearch.score("oogl", "Google"))
        assertNull(AppSearch.score("zz", "Google"))
        assertNull(AppSearch.score("  ", "Google"))
    }

    @Test
    fun forgivesSmallTyposAfterRealMatches() {
        // One slip in a 4-6 letter query, two in a longer one; always ranked after real matches.
        assertEquals(5, AppSearch.score("spotfy", "Spotify"))
        assertEquals(5, AppSearch.score("calender", "Calendar"))
        assertEquals(5, AppSearch.score("whtasapp", "WhatsApp"))
        assertEquals(5, AppSearch.score("mpas", "Google Maps"))
        assertEquals(5, AppSearch.score("telgeram", "Telegram"))
        assertNull(AppSearch.score("map", "Mail"))      // too short to guess
        assertNull(AppSearch.score("spotfiy", "Slack"))
        val apps = listOf(app("Calendar"), app("Calculator"))
        assertEquals(listOf("Calendar"), AppSearch.rank("calender", apps).map { it.label })
    }

    @Test
    fun ignoresAccentsAndCase() {
        assertEquals(0, AppSearch.score("CAFE", "Café"))
    }

    @Test
    fun ranksPrefixBeforeSubstringAndThenByUse() {
        val apps = listOf(app("Clock"), app("Calculator"), app("Calendar", launches = 5), app("Tic Tac Toe"))
        assertEquals(listOf("Calendar", "Calculator"), AppSearch.rank("cal", apps).map { it.label })
        assertEquals(listOf("Clock", "Tic Tac Toe"), AppSearch.rank("c", apps).map { it.label }.filter { it == "Clock" || it == "Tic Tac Toe" })
    }
}
