package dev.farjad.driftlist

import dev.farjad.driftlist.domain.Wave
import org.junit.Assert.assertEquals
import org.junit.Test

class WaveTest {
    @Test
    fun intensityIsFullAtFingerAndZeroAtInfluenceEdge() {
        assertEquals(1f, Wave.intensity(0f, 4), 1e-6f)
        assertEquals(0.5f, Wave.intensity(2f, 4), 1e-6f)
        assertEquals(0f, Wave.intensity(4f, 4), 1e-6f)
        assertEquals(0f, Wave.intensity(9f, 4), 1e-6f)
        assertEquals(Wave.intensity(-1.5f, 4), Wave.intensity(1.5f, 4), 1e-6f)
    }

    @Test
    fun slotAtClampsToStrip() {
        assertEquals(0, Wave.slotAt(-10f, 270f, 27))
        assertEquals(0, Wave.slotAt(5f, 270f, 27))
        assertEquals(13, Wave.slotAt(135f, 270f, 27))
        assertEquals(26, Wave.slotAt(999f, 270f, 27))
    }

    @Test
    fun nearestEnabledSkipsEmptyLetters() {
        val enabled = listOf(true, false, false, true, false)
        assertEquals(0, Wave.nearestEnabled(0, enabled))
        assertEquals(0, Wave.nearestEnabled(1, enabled))
        assertEquals(3, Wave.nearestEnabled(2, enabled))
        assertEquals(3, Wave.nearestEnabled(4, enabled))
        assertEquals(-1, Wave.nearestEnabled(0, listOf(false, false)))
    }
}
