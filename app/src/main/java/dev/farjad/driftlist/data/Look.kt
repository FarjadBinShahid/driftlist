package dev.farjad.driftlist.data

import org.json.JSONObject

/** Clock designs for the home header. Our own, drawn from type alone. */
enum class ClockStyle(val label: String, val description: String) {
    Thin("Thin", "Large and light"),
    Bold("Bold", "Large and heavy"),
    Stacked("Stacked", "Hours above minutes"),
    Compact("Compact", "Small, date beside it"),
    Mono("Mono", "Fixed-width digits"),
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
) {
    fun toJson(): String = JSONObject()
        .put("clock", clockStyle.name)
        .put("font", font)
        .put("font_scale", fontScale.toDouble())
        .put("accent", accent ?: JSONObject.NULL)
        .put("mode", mode.name)
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
    )
}

/**
 * Fonts: the families every Android phone already has (no downloads, nothing to license), plus
 * .ttf/.otf files the user imports. Ids are "system:<family>" or "file:<name>".
 */
object Fonts {
    const val DEFAULT = "system:sans-serif"
    const val FILE_PREFIX = "file:"

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
        system.firstOrNull { it.first == id }?.second
            ?: id.removePrefix(FILE_PREFIX).substringBeforeLast('.').takeIf { id.startsWith(FILE_PREFIX) }
            ?: "System"
}

/** Themes that ship with Driftlist. Applying one replaces the look; the wallpaper and icons stay. */
data class ReadyTheme(val id: String, val name: String, val description: String, val look: Look)

object ReadyThemes {
    val all: List<ReadyTheme> = listOf(
        ReadyTheme("driftlist", "Driftlist", "Thin clock, colours from your wallpaper", Look()),
        ReadyTheme("ink", "Ink", "Serif type, graphite, always light", Look(ClockStyle.Thin, "system:serif", 1f, Accents.presets[7].second, ThemeMode.Light)),
        ReadyTheme("night-shift", "Night shift", "Bold clock, warm amber, always dark", Look(ClockStyle.Bold, "system:sans-serif-condensed", 1f, Accents.presets[3].second, ThemeMode.Dark)),
        ReadyTheme("terminal", "Terminal", "Fixed-width everything, teal", Look(ClockStyle.Mono, "system:monospace", 1f, Accents.presets[1].second, ThemeMode.Dark)),
        ReadyTheme("postcard", "Postcard", "Stacked clock, casual type, coral", Look(ClockStyle.Stacked, "system:casual", 1.05f, Accents.presets[4].second, ThemeMode.Auto)),
        ReadyTheme("quiet", "Quiet", "Small clock, light type, sage", Look(ClockStyle.Compact, "system:sans-serif-light", 1f, Accents.presets[2].second, ThemeMode.Auto)),
    )
}
