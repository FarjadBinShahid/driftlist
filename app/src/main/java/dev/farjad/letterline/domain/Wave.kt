package dev.farjad.letterline.domain

import kotlin.math.abs

/**
 * Pure maths for the wave alphabet, kept out of Compose so it can be unit tested.
 *
 * The strip is a list of entries: the favorites star, the letters that have apps, and the
 * settings circle. Letters slide sideways in a V centred on the finger.
 */
object Wave {
    /**
     * How far a letter moves toward the finger: 1 at the finger, falling off linearly to 0 at
     * [influence] slots away (a V shape, as measured on the reference app).
     */
    fun intensity(distanceInSlots: Float, influence: Float): Float {
        if (influence <= 0f) return 0f
        val d = abs(distanceInSlots)
        return (1f - d / influence).coerceIn(0f, 1f)
    }

    /** Index of the slot under [y] for a strip of [height] split into [count] equal slots. */
    fun slotAt(y: Float, height: Float, count: Int): Int {
        if (count <= 0 || height <= 0f) return 0
        return (y / (height / count)).toInt().coerceIn(0, count - 1)
    }

    /**
     * The nearest slot to [slot] whose letter is enabled, preferring the one below on a tie,
     * or -1 if none are enabled.
     */
    fun nearestEnabled(slot: Int, enabled: List<Boolean>): Int {
        if (enabled.isEmpty()) return -1
        if (enabled.getOrElse(slot) { false }) return slot
        for (step in 1 until enabled.size) {
            val down = slot + step
            val up = slot - step
            if (down in enabled.indices && enabled[down]) return down
            if (up in enabled.indices && enabled[up]) return up
        }
        return -1
    }
}
