package dev.farjad.driftlist

import dev.farjad.driftlist.data.ClockStyle
import dev.farjad.driftlist.data.Fonts
import dev.farjad.driftlist.data.Look
import dev.farjad.driftlist.data.ReadyThemes
import dev.farjad.driftlist.data.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class LookTest {
    @Test
    fun roundTripsThroughJson() {
        val look = Look(ClockStyle.Stacked, "file:Inter.ttf", 1.15f, 0xFFC2185B.toInt(), ThemeMode.Dark)
        assertEquals(look, Look.fromJson(look.toJson()))
        assertEquals(Look(), Look.fromJson(Look().toJson()))
    }

    @Test
    fun badOrMissingJsonFallsBackToDefaults() {
        assertEquals(Look(), Look.fromJson(null))
        assertEquals(Look(), Look.fromJson("not json"))
        assertEquals(ClockStyle.Thin, Look.fromJson("""{"clock":"Neon","mode":"Sepia"}""").clockStyle)
        assertEquals(ThemeMode.Auto, Look.fromJson("""{"clock":"Neon","mode":"Sepia"}""").mode)
    }

    @Test
    fun fontLabels() {
        assertEquals("Serif", Fonts.label("system:serif"))
        assertEquals("Inter Display", Fonts.label("file:Inter Display.otf"))
        assertEquals("System", Fonts.label("system:unknown"))
    }

    @Test
    fun readyThemeIdsAreUnique() {
        assertEquals(ReadyThemes.all.size, ReadyThemes.all.map { it.id }.toSet().size)
    }
}
