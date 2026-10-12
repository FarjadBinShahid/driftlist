package dev.farjad.letterline.system

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Build
import android.view.WindowManager
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Letterline's own wallpapers, drawn in code (no image files, nothing licensed). Ids are
 * "art:<name>", stable across devices, so themes and backups can carry them.
 */
object WallpaperArt {
    data class Design(val id: String, val name: String, val draw: (Canvas, Int, Int) -> Unit)

    private fun argb(hex: Long) = hex.toInt()

    val designs: List<Design> = listOf(
        Design("art:dusk", "Dusk") { c, w, h -> gradient(c, w, h, argb(0xFF1B1F3B), argb(0xFF53346D), argb(0xFFB35D6C)) },
        Design("art:dunes", "Dunes") { c, w, h ->
            gradient(c, w, h, argb(0xFF2B1E16), argb(0xFF6E4A2F), argb(0xFFD9A066))
            waves(c, w, h, listOf(argb(0x33F2C48D), argb(0x55A86B3C), argb(0x88502E1A)), amplitude = 0.05f)
        },
        Design("art:tide", "Tide") { c, w, h ->
            gradient(c, w, h, argb(0xFF04202B), argb(0xFF0B4F5C), argb(0xFF3FA7A3))
            waves(c, w, h, listOf(argb(0x3364D2C8), argb(0x55107A7C), argb(0x8803343F)), amplitude = 0.035f)
        },
        Design("art:meadow", "Meadow") { c, w, h ->
            gradient(c, w, h, argb(0xFF0F2416), argb(0xFF2F5D3A), argb(0xFF9BC27E))
            waves(c, w, h, listOf(argb(0x33C9E3A1), argb(0x553E7D45), argb(0x88142A18)), amplitude = 0.045f)
        },
        Design("art:aurora", "Aurora") { c, w, h ->
            c.drawColor(argb(0xFF070B1A))
            blob(c, w * 0.2f, h * 0.25f, w * 0.9f, argb(0x9930C9A5))
            blob(c, w * 0.85f, h * 0.45f, w * 0.8f, argb(0x887A4CE0))
            blob(c, w * 0.4f, h * 0.8f, w * 0.9f, argb(0x66305FE0))
        },
        Design("art:ember", "Ember") { c, w, h ->
            c.drawColor(argb(0xFF120806))
            blob(c, w * 0.5f, h * 0.95f, w * 1.3f, argb(0xCCE0602F))
            blob(c, w * 0.15f, h * 0.7f, w * 0.7f, argb(0x77C2185B))
        },
        Design("art:ink", "Ink") { c, w, h ->
            gradient(c, w, h, argb(0xFF0D0E10), argb(0xFF16181C), argb(0xFF0A0B0C))
            grain(c, w, h, argb(0x14FFFFFF))
        },
        Design("art:fog", "Fog") { c, w, h ->
            gradient(c, w, h, argb(0xFFF3F1EE), argb(0xFFDCDFE3), argb(0xFFB9C2CC))
            grain(c, w, h, argb(0x10000000))
        },
    )

    fun design(id: String?): Design? = designs.firstOrNull { it.id == id }

    fun render(id: String, width: Int, height: Int): Bitmap? {
        val d = design(id) ?: return null
        return createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1)).also { d.draw(Canvas(it), width, height) }
    }

    /** Where to set it: the home screen, the lock screen, or both. */
    enum class Target(val label: String, val flags: Int) {
        Home("Home screen", WallpaperManager.FLAG_SYSTEM),
        Lock("Lock screen", WallpaperManager.FLAG_LOCK),
        Both("Both", WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK),
    }

    /** Draws [id] at the screen's full size and sets it. Needs SET_WALLPAPER (a normal permission). */
    suspend fun apply(context: Context, id: String, target: Target): Boolean = withContext(Dispatchers.Default) {
        val (w, h) = screenSize(context)
        val bmp = render(id, w, h) ?: return@withContext false
        runCatching {
            WallpaperManager.getInstance(context).setBitmap(bmp, null, true, target.flags)
            true
        }.getOrDefault(false).also { bmp.recycle() }
    }

    private fun screenSize(context: Context): Pair<Int, Int> {
        val wm = context.getSystemService(WindowManager::class.java)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val b = wm.maximumWindowMetrics.bounds
            minOf(b.width(), b.height()) to maxOf(b.width(), b.height())
        } else {
            val m = context.resources.displayMetrics
            minOf(m.widthPixels, m.heightPixels) to maxOf(m.widthPixels, m.heightPixels)
        }
    }

    // --- drawing helpers ----------------------------------------------------------------------

    private fun gradient(c: Canvas, w: Int, h: Int, top: Int, mid: Int, bottom: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, w * 0.3f, h.toFloat(), intArrayOf(top, mid, bottom), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
    }

    /** Layered sine bands across the lower half, back to front. */
    private fun waves(c: Canvas, w: Int, h: Int, colors: List<Int>, amplitude: Float) {
        colors.forEachIndexed { i, color ->
            val base = h * (0.55f + i * 0.13f)
            val amp = h * amplitude * (1f + i * 0.3f)
            val phase = i * 1.7f
            val path = Path().apply {
                moveTo(0f, h.toFloat())
                var x = 0
                while (x <= w) {
                    val t = x / w.toFloat()
                    lineTo(x.toFloat(), base + amp * sin(2 * PI * (t * (1.2 + i * 0.4)) + phase).toFloat())
                    x += 8
                }
                lineTo(w.toFloat(), h.toFloat())
                close()
            }
            c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
        }
    }

    private fun blob(c: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, cy, r, intArrayOf(color, color and 0x00FFFFFF), null, Shader.TileMode.CLAMP)
        }
        c.drawCircle(cx, cy, r, p)
    }

    /** Fixed-seed speckle so the image is the same every time. */
    private fun grain(c: Canvas, w: Int, h: Int, color: Int) {
        val rnd = Random(7)
        val p = Paint().apply { this.color = color }
        repeat((w * h) / 900) { c.drawPoint(rnd.nextFloat() * w, rnd.nextFloat() * h, p) }
    }
}
