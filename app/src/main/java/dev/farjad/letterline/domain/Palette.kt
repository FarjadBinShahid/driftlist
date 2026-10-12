package dev.farjad.letterline.domain

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** The four accent roles the theme needs, as ARGB. */
data class AccentSet(val accent: Int, val onAccent: Int, val container: Int, val onContainer: Int)

/**
 * Turns one seed colour (the wallpaper's, or one the user picked) into accent colours that keep
 * WCAG AA contrast (4.5:1) against the app's surfaces. Hue is kept; lightness is walked until
 * every pair passes. Android-free so it runs as a unit test.
 */
object Palette {
    private const val AA = 4.5
    private const val STEP = 0.01f

    fun light(seed: Int, background: Int, surface: Int): AccentSet {
        val (h, s) = hueAndSaturation(seed)
        val accent = walk(h, s, 0.42f, darker = true) { c -> contrast(c, WHITE) >= AA && contrast(c, background) >= AA && contrast(c, surface) >= AA }
        val container = fromHsl(h, min(s, 0.7f), 0.92f)
        val onContainer = walk(h, s, 0.25f, darker = true) { c -> contrast(c, container) >= AA }
        return AccentSet(accent, WHITE, container, onContainer)
    }

    fun dark(seed: Int, background: Int, surface: Int): AccentSet {
        val (h, s) = hueAndSaturation(seed)
        val accent = walk(h, s, 0.75f, darker = false) { c -> contrast(c, background) >= AA && contrast(c, surface) >= AA }
        val onAccent = walk(h, s, 0.15f, darker = true) { c -> contrast(c, accent) >= AA }
        val container = fromHsl(h, s * 0.8f, 0.28f)
        val onContainer = walk(h, min(s, 0.6f), 0.9f, darker = false) { c -> contrast(c, container) >= AA }
        return AccentSet(accent, onAccent, container, onContainer)
    }

    /** WCAG 2.x contrast ratio between two opaque colours, 1 to 21. */
    fun contrast(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    fun luminance(argb: Int): Double {
        fun channel(v: Int): Double {
            val c = v / 255.0
            return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(argb shr 16 and 0xFF) + 0.7152 * channel(argb shr 8 and 0xFF) + 0.0722 * channel(argb and 0xFF)
    }

    /** [hue 0-360, saturation 0-1, lightness 0-1]. */
    fun hsl(argb: Int): FloatArray {
        val r = (argb shr 16 and 0xFF) / 255f
        val g = (argb shr 8 and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        val mx = max(r, max(g, b))
        val mn = min(r, min(g, b))
        val l = (mx + mn) / 2f
        val d = mx - mn
        if (d == 0f) return floatArrayOf(0f, 0f, l)
        val s = d / (1f - abs(2f * l - 1f))
        val h = when (mx) {
            r -> 60f * (((g - b) / d) % 6f)
            g -> 60f * ((b - r) / d + 2f)
            else -> 60f * ((r - g) / d + 4f)
        }
        return floatArrayOf((h + 360f) % 360f, s.coerceIn(0f, 1f), l)
    }

    fun fromHsl(h: Float, s: Float, l: Float): Int {
        val c = (1f - abs(2f * l - 1f)) * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f
        val (r, g, b) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        fun byte(v: Float) = ((v + m) * 255f).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (byte(r) shl 16) or (byte(g) shl 8) or byte(b)
    }

    /** Near-grey seeds stay grey; everything else gets a usable, not neon, saturation. */
    private fun hueAndSaturation(seed: Int): Pair<Float, Float> {
        val (h, s, _) = hsl(seed)
        return h to if (s < 0.08f) 0.06f else s.coerceIn(0.35f, 0.85f)
    }

    private inline fun walk(h: Float, s: Float, start: Float, darker: Boolean, ok: (Int) -> Boolean): Int {
        var l = start
        while (l in 0f..1f) {
            val c = fromHsl(h, s, l)
            if (ok(c)) return c
            l += if (darker) -STEP else STEP
        }
        return if (darker) BLACK else WHITE
    }

    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val BLACK = 0xFF000000.toInt()
}
