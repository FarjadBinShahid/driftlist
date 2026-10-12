package dev.farjad.letterline.system

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import dev.farjad.letterline.data.Fonts
import java.io.File

/** Fonts the user imports (S20 "Add font"): copied into app storage, checked, listed. */
object FontStore {
    private const val MAX_BYTES = 8L * 1024 * 1024

    fun dir(context: Context): File = File(context.filesDir, "fonts").apply { mkdirs() }

    /** Font ids of imported files, sorted by name. */
    fun imported(context: Context): List<String> =
        dir(context).listFiles().orEmpty().filter { it.isFile }.sortedBy { it.name.lowercase() }.map { Fonts.FILE_PREFIX + it.name }

    /** Copies a .ttf/.otf from the picker. Returns the new font id, or an error message for the user. */
    fun import(context: Context, uri: Uri): Result<String> = runCatching {
        val name = displayName(context, uri)?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "Font.ttf"
        val ext = name.substringAfterLast('.', "").lowercase()
        require(ext == "ttf" || ext == "otf") { "Pick a .ttf or .otf font file" }
        val safe = name.replace(Regex("[^A-Za-z0-9 ._-]"), "_")
        val target = File(dir(context), safe)
        val tmp = File(dir(context), "$safe.part")
        context.contentResolver.openInputStream(uri)!!.use { input ->
            tmp.outputStream().use { out ->
                val copied = input.copyTo(out)
                require(copied in 1..MAX_BYTES) { "That file is too large for a font" }
            }
        }
        // Typeface falls back silently on garbage; an empty-glyph check is the cheapest real test.
        val ok = runCatching { Typeface.createFromFile(tmp) }.getOrNull()?.let { it != Typeface.DEFAULT } ?: false
        if (!ok) {
            tmp.delete()
            error("That file couldn't be read as a font")
        }
        tmp.renameTo(target)
        Fonts.FILE_PREFIX + safe
    }

    fun delete(context: Context, id: String) {
        if (id.startsWith(Fonts.FILE_PREFIX)) File(dir(context), id.removePrefix(Fonts.FILE_PREFIX)).delete()
    }

    private fun displayName(context: Context, uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
}
