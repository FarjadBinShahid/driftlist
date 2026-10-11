package dev.farjad.driftlist.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

/**
 * Pure maths for the wave alphabet, kept out of Compose so it can be unit tested.
 */
object Wave {
    /**
     * How strongly a letter reacts to the finger: 1 at the finger, easing to 0 at [influence]
     * slots away on a raised-cosine curve.
     */
    fun intensity(distanceInSlots: Float, influence: Int): Float {
        val d = abs(distanceInSlots)
        if (d >= influence) return 0f
        return ((cos(PI * d / influence) + 1.0) / 2.0).toFloat()
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
