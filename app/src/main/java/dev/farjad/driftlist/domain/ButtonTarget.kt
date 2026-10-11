package dev.farjad.driftlist.domain

/**
 * What the round button opens: "app:<appId>", "folder:<folderId>" or
 * "shortcut:<userSerial>|<package>|<shortcutId>|<label>". Pure, so it is unit tested.
 */
sealed interface ButtonTarget {
    data class App(val appId: String) : ButtonTarget
    data class Folder(val folderId: String) : ButtonTarget
    data class Shortcut(val userSerial: Long, val packageName: String, val shortcutId: String, val label: String) : ButtonTarget

    fun encode(): String = when (this) {
        is App -> "app:$appId"
        is Folder -> "folder:$folderId"
        is Shortcut -> "shortcut:$userSerial|$packageName|$shortcutId|$label"
    }

    companion object {
        fun parse(value: String?): ButtonTarget? = when {
            value == null -> null
            value.startsWith("app:") -> App(value.removePrefix("app:"))
            value.startsWith("folder:") -> Folder(value.removePrefix("folder:"))
            value.startsWith("shortcut:") -> value.removePrefix("shortcut:").split("|", limit = 4).let { p ->
                val serial = p.getOrNull(0)?.toLongOrNull()
                if (p.size == 4 && serial != null && p[1].isNotEmpty() && p[2].isNotEmpty()) Shortcut(serial, p[1], p[2], p[3]) else null
            }
            else -> null
        }
    }
}
