package dev.farjad.letterline

import dev.farjad.letterline.data.ClockStyle
import dev.farjad.letterline.data.Fonts
import dev.farjad.letterline.data.IconStyle
import dev.farjad.letterline.data.Look
import dev.farjad.letterline.data.ReadyThemes
import dev.farjad.letterline.data.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LookTest {
    @Test
    fun roundTripsThroughJson() {
        val look = Look(ClockStyle.Stacked, "file:Inter.ttf", 1.15f, 0xFFC2185B.toInt(), ThemeMode.Dark, IconStyle.Dots, "art:dunes")
        assertEquals(look, Look.fromJson(look.toJson()))
        assertEquals(Look(), Look.fromJson(Look().toJson()))
    }

    @Test
    fun onlyOurOwnWallpapersAreAccepted() {
        assertEquals(null, Look.fromJson("""{"wallpaper":"/sdcard/x.png"}""").wallpaper)
        assertTrue(Look(wallpaper = "art:ink").sameStyle(Look()))
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
    fun everyReadyThemeUsesAFontWeShip() {
        val known = Fonts.bundled.map { it.id } + Fonts.system.map { it.first }
        ReadyThemes.all.forEach { assertTrue("${it.id} uses unknown font ${it.look.font}", it.look.font in known) }
        assertEquals("Lora", Fonts.label("font:lora"))
        assertEquals(Fonts.bundled.size, Fonts.bundled.map { it.id }.toSet().size)
    }

    @Test
    fun readyThemeIdsAreUnique() {
        assertEquals(ReadyThemes.all.size, ReadyThemes.all.map { it.id }.toSet().size)
    }
}
