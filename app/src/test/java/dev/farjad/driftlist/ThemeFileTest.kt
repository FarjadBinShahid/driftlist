package dev.farjad.driftlist

import dev.farjad.driftlist.data.ClockStyle
import dev.farjad.driftlist.data.Fonts
import dev.farjad.driftlist.data.IconStyle
import dev.farjad.driftlist.data.Look
import dev.farjad.driftlist.data.ThemeMode
import dev.farjad.driftlist.domain.ThemeFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeFileTest {
    private val look = Look(ClockStyle.Bold, "font:lora", 1.1f, 0xFFE0604F.toInt(), ThemeMode.Dark, IconStyle.Dots, "art:tide")

    @Test
    fun roundTrips() {
        val parsed = ThemeFile.read(ThemeFile.write("Evening", look, "com.example.pack")).getOrThrow()
        assertEquals("Evening", parsed.name)
        assertEquals(look, parsed.look)
        assertEquals("com.example.pack", parsed.iconPack)
        assertFalse(parsed.droppedFont)
    }

    @Test
    fun importedFontsDoNotTravel() {
        val text = ThemeFile.write("Mine", look.copy(font = "file:Secret.ttf"), null)
        assertFalse(text.contains("Secret"))
        assertEquals(Fonts.DEFAULT, ThemeFile.read(text).getOrThrow().look.font)
    }

    @Test
    fun unknownFontInAFileFallsBackAndSaysSo() {
        val text = """{"format":"driftlist-theme","version":1,"name":"X","look":{"font":"file:../../etc/passwd"},"icon_pack":"not a package"}"""
        val parsed = ThemeFile.read(text).getOrThrow()
        assertEquals(Fonts.DEFAULT, parsed.look.font)
        assertTrue(parsed.droppedFont)
        assertNull(parsed.iconPack)
    }

    @Test
    fun rejectsOtherFiles() {
        assertTrue(ThemeFile.read("hello").isFailure)
        assertTrue(ThemeFile.read("""{"format":"driftlist-backup","version":1}""").isFailure)
        assertTrue(ThemeFile.read("""{"format":"driftlist-theme","version":9}""").isFailure)
        assertTrue(ThemeFile.read("x".repeat(ThemeFile.MAX_BYTES + 1)).isFailure)
    }
}
