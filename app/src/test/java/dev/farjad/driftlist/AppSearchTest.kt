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
