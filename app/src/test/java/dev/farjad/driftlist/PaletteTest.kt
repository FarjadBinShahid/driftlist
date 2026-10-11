package dev.farjad.driftlist

import dev.farjad.driftlist.domain.AccentSet
import dev.farjad.driftlist.domain.Palette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaletteTest {
    // The surfaces the accent sits on (tokens.json light/dark bg and surface-raised).
    private val lightBg = 0xFFFBFBFC.toInt()
    private val lightSurface = 0xFFFFFFFF.toInt()
    private val darkBg = 0xFF0F1012.toInt()
    private val darkSurface = 0xFF232529.toInt()

    private val seeds = listOf(
        0xFF3B5BCC, 0xFFFFEB3B, 0xFF00FFFF, 0xFF808080, 0xFFFFFFFF, 0xFF000000,
        0xFFE91E63, 0xFF00C853, 0xFFFF6D00, 0xFF6200EA, 0xFF795548, 0xFFF5F5DC,
    ).map { it.toInt() }

    private fun AccentSet.assertReadable(bg: Int, surface: Int, seed: Int) {
        val name = "seed %08X".format(seed)
        assertTrue("$name: text on accent", Palette.contrast(onAccent, accent) >= 4.5)
        assertTrue("$name: accent on background", Palette.contrast(accent, bg) >= 4.5)
        assertTrue("$name: accent on surface", Palette.contrast(accent, surface) >= 4.5)
        assertTrue("$name: text on container", Palette.contrast(onContainer, container) >= 4.5)
    }

    @Test
    fun everySeedGivesReadableLightAndDarkAccents() {
        for (seed in seeds) {
            Palette.light(seed, lightBg, lightSurface).assertReadable(lightBg, lightSurface, seed)
            Palette.dark(seed, darkBg, darkSurface).assertReadable(darkBg, darkSurface, seed)
        }
    }

    @Test
    fun saturatedSeedsKeepTheirHue() {
        for (seed in listOf(0xFF3B5BCC, 0xFFE91E63, 0xFF00C853, 0xFFFF6D00).map { it.toInt() }) {
            val hue = Palette.hsl(seed)[0]
            assertEquals(hue, Palette.hsl(Palette.light(seed, lightBg, lightSurface).accent)[0], 3f)
            assertEquals(hue, Palette.hsl(Palette.dark(seed, darkBg, darkSurface).accent)[0], 3f)
        }
    }

    @Test
    fun contrastMatchesWcag() {
        assertEquals(21.0, Palette.contrast(0xFF000000.toInt(), 0xFFFFFFFF.toInt()), 0.01)
        assertEquals(1.0, Palette.contrast(0xFF777777.toInt(), 0xFF777777.toInt()), 0.001)
        assertEquals(4.48, Palette.contrast(0xFF777777.toInt(), 0xFFFFFFFF.toInt()), 0.01)
    }
}
