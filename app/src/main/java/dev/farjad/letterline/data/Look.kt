package dev.farjad.letterline.data

import org.json.JSONObject

/** Clock designs for the home header. Our own, drawn from type alone. */
enum class ClockStyle(val label: String, val description: String) {
    Thin("Thin", "Large and light"),
    Bold("Bold", "Large and heavy"),
    Stacked("Stacked", "Hours above minutes"),
    Compact("Compact", "Small, date beside it"),
    Mono("Mono", "Fixed-width digits"),
}

/** How apps are drawn on home: their icons, a small coloured dot, or text only. */
enum class IconStyle(val label: String) {
    Icons("Icons"),
    Dots("Dots"),
    None("Text only"),
}

enum class ThemeMode(val label: String) {
    Auto("Follow system"),
    Light("Light"),
    Dark("Dark"),
}

/**
 * The current look: everything Theme Studio edits. Applied live and saved on every change.
 * [accent] null means "take it from the wallpaper".
 */
data class Look(
    val clockStyle: ClockStyle = ClockStyle.Thin,
    val font: String = Fonts.DEFAULT,
    val fontScale: Float = 1f,
    val accent: Int? = null,
    val mode: ThemeMode = ThemeMode.Auto,
    val iconStyle: IconStyle = IconStyle.Icons,
    /** One of Letterline's own wallpapers ("art:<name>"), or null to leave the wallpaper alone. */
    val wallpaper: String? = null,
) {
    /** Same look apart from the wallpaper (a theme applied without its wallpaper still counts). */
    fun sameStyle(other: Look) = copy(wallpaper = null) == other.copy(wallpaper = null)

    fun toJson(): String = JSONObject()
        .put("clock", clockStyle.name)
        .put("font", font)
        .put("font_scale", fontScale.toDouble())
        .put("accent", accent ?: JSONObject.NULL)
        .put("mode", mode.name)
        .put("icons", iconStyle.name)
        .put("wallpaper", wallpaper ?: JSONObject.NULL)
        .toString()

    /** One line for lists: "Bold clock · Serif · Coral". */
    fun summary(fontLabel: (String) -> String): String = listOf(
        "${clockStyle.label} clock",
        fontLabel(font),
        accent?.let { a -> Accents.presets.firstOrNull { it.second == a }?.first ?: "Custom colour" } ?: "Wallpaper colours",
    ).joinToString(" · ")

    companion object {
        fun fromJson(json: String?): Look {
            if (json.isNullOrBlank()) return Look()
            return runCatching {
                val o = JSONObject(json)
                Look(
                    clockStyle = enumOr(o.optString("clock"), ClockStyle.Thin),
                    font = o.optString("font").ifBlank { Fonts.DEFAULT },
                    fontScale = o.optDouble("font_scale", 1.0).toFloat().coerceIn(0.5f, 2f),
                    accent = if (o.isNull("accent") || !o.has("accent")) null else o.getInt("accent"),
                    mode = enumOr(o.optString("mode"), ThemeMode.Auto),
                    iconStyle = enumOr(o.optString("icons"), IconStyle.Icons),
                    wallpaper = if (o.isNull("wallpaper") || !o.has("wallpaper")) null else o.getString("wallpaper").takeIf { it.startsWith("art:") },
                )
            }.getOrDefault(Look())
        }

        private inline fun <reified E : Enum<E>> enumOr(name: String, default: E): E =
            enumValues<E>().firstOrNull { it.name == name } ?: default
    }
}

/** Accent seeds offered in Theme Studio, next to "from wallpaper". The palette makes each one readable. */
object Accents {
    val presets: List<Pair<String, Int>> = listOf(
        "Ocean" to 0xFF3B5BCC.toInt(),
        "Teal" to 0xFF00897B.toInt(),
        "Sage" to 0xFF6B8F71.toInt(),
        "Amber" to 0xFFC77700.toInt(),
        "Coral" to 0xFFE0604F.toInt(),
        "Rose" to 0xFFC2185B.toInt(),
        "Violet" to 0xFF7E57C2.toInt(),
        "Graphite" to 0xFF5F6670.toInt(),
        "Moss" to 0xFF2F6A4F.toInt(),
    )
}

/**
 * Fonts: families bundled with Letterline (SIL Open Font License, from github.com/google/fonts,
 * licences in assets/licenses/fonts), the families every Android phone already has, and .ttf/.otf
 * files the user imports. Ids are "font:<name>", "system:<family>" or "file:<name>".
 */
object Fonts {
    const val DEFAULT = "system:sans-serif"
    const val FILE_PREFIX = "file:"
    const val BUNDLED_PREFIX = "font:"

    /** Shipped with the app; [licence] is the file under assets/licenses/fonts. */
    data class Bundled(val id: String, val label: String, val licence: String, val variable: Boolean)

    val bundled: List<Bundled> = listOf(
        Bundled("font:manrope", "Manrope", "manrope", variable = true),
        Bundled("font:outfit", "Outfit", "outfit", variable = true),
        Bundled("font:space_grotesk", "Space Grotesk", "spacegrotesk", variable = true),
        Bundled("font:comfortaa", "Comfortaa", "comfortaa", variable = true),
        Bundled("font:lora", "Lora", "lora", variable = true),
        Bundled("font:playfair_display", "Playfair Display", "playfairdisplay", variable = true),
        Bundled("font:fraunces", "Fraunces", "fraunces", variable = true),
        Bundled("font:dm_serif_display", "DM Serif Display", "dmserifdisplay", variable = false),
        Bundled("font:jetbrains_mono", "JetBrains Mono", "jetbrainsmono", variable = true),
        Bundled("font:bebas_neue", "Bebas Neue", "bebasneue", variable = false),
        Bundled("font:caveat", "Caveat", "caveat", variable = true),
    )

    /** Families on every Android phone. */
    val system: List<Pair<String, String>> = listOf(
        DEFAULT to "System",
        "system:sans-serif-light" to "Light",
        "system:sans-serif-condensed" to "Condensed",
        "system:serif" to "Serif",
        "system:monospace" to "Mono",
        "system:serif-monospace" to "Typewriter",
        "system:casual" to "Casual",
        "system:cursive" to "Handwritten",
        "system:sans-serif-smallcaps" to "Small caps",
    )

    fun label(id: String): String =
        bundled.firstOrNull { it.id == id }?.label
            ?: system.firstOrNull { it.first == id }?.second
            ?: id.removePrefix(FILE_PREFIX).substringBeforeLast('.').takeIf { id.startsWith(FILE_PREFIX) }
            ?: "System"
}

/** Themes that ship with Letterline. Applying one replaces the look; the wallpaper and icons stay. */
data class ReadyTheme(val id: String, val name: String, val description: String, val look: Look)

object ReadyThemes {
    val all: List<ReadyTheme> = listOf(
        ReadyTheme("letterline", "Letterline", "Thin clock, colours from your wallpaper", Look()),
        ReadyTheme("ink", "Ink", "Lora serif, graphite, foggy wallpaper", Look(ClockStyle.Thin, "font:lora", 1f, Accents.presets[7].second, ThemeMode.Light, wallpaper = "art:fog")),
        ReadyTheme("night-shift", "Night shift", "Bebas Neue, warm amber, ember wallpaper", Look(ClockStyle.Bold, "font:bebas_neue", 1.05f, Accents.presets[3].second, ThemeMode.Dark, wallpaper = "art:ember")),
        ReadyTheme("terminal", "Terminal", "JetBrains Mono, teal, dots for icons", Look(ClockStyle.Mono, "font:jetbrains_mono", 1f, Accents.presets[1].second, ThemeMode.Dark, IconStyle.Dots, "art:ink")),
        ReadyTheme("postcard", "Postcard", "Stacked clock, handwritten Caveat, dunes", Look(ClockStyle.Stacked, "font:caveat", 1.1f, Accents.presets[4].second, ThemeMode.Auto, wallpaper = "art:dunes")),
        ReadyTheme("quiet", "Quiet", "Small clock, Manrope, text only", Look(ClockStyle.Compact, "font:manrope", 1f, Accents.presets[2].second, ThemeMode.Auto, IconStyle.None, "art:meadow")),
        ReadyTheme("gallery", "Gallery", "Playfair Display, rose, dusk wallpaper", Look(ClockStyle.Thin, "font:playfair_display", 1f, Accents.presets[5].second, ThemeMode.Light, wallpaper = "art:dusk")),
    )
}
