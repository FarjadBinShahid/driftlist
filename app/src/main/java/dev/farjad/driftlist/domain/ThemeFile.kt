package dev.farjad.driftlist.domain

import dev.farjad.driftlist.data.Fonts
import dev.farjad.driftlist.data.Look
import org.json.JSONObject

/**
 * Theme files you can share: a small JSON document (".driftlist-theme"). It carries the look,
 * the icon pack name and one of our own wallpapers, never files from the phone: an imported font
 * can't travel, so it falls back to the system font. Pure, so it is unit tested.
 */
object ThemeFile {
    const val FORMAT = "driftlist-theme"
    const val VERSION = 1
    const val MAX_BYTES = 64 * 1024
    const val MIME = "application/json"
    const val EXTENSION = ".driftlist-theme"
    private const val MAX_NAME = 40

    data class Parsed(val name: String, val look: Look, val iconPack: String?, val droppedFont: Boolean)

    fun write(name: String, look: Look, iconPack: String?): String {
        val portable = if (look.font.startsWith(Fonts.FILE_PREFIX)) look.copy(font = Fonts.DEFAULT) else look
        return JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("name", name.take(MAX_NAME))
            .put("look", JSONObject(portable.toJson()))
            .put("icon_pack", iconPack ?: JSONObject.NULL)
            .toString(2)
    }

    fun read(text: String): Result<Parsed> = runCatching {
        require(text.length <= MAX_BYTES) { "That file is too large to be a theme" }
        val root = runCatching { JSONObject(text) }.getOrElse { throw IllegalArgumentException("That file isn't a Driftlist theme") }
        require(root.optString("format") == FORMAT) { "That file isn't a Driftlist theme" }
        require(root.optInt("version", 0) in 1..VERSION) { "That theme is from a newer version of Driftlist" }
        val name = root.optString("name").trim().take(MAX_NAME).ifEmpty { "Shared theme" }
        val raw = Look.fromJson(root.optJSONObject("look")?.toString())
        val known = raw.font in Fonts.bundled.map { it.id } || raw.font in Fonts.system.map { it.first }
        val look = if (known) raw else raw.copy(font = Fonts.DEFAULT)
        val pack = if (root.isNull("icon_pack")) null else root.optString("icon_pack").takeIf { it.matches(PACKAGE) }
        Parsed(name, look, pack, droppedFont = !known)
    }

    private val PACKAGE = Regex("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+")
}
