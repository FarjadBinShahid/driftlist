package dev.farjad.driftlist.domain

import dev.farjad.driftlist.data.db.AppEntity
import java.text.Normalizer

/**
 * App search ranking: whole-label prefix, then word prefix, then initials ("gm" -> Google Maps),
 * then substring. Accents are ignored ("cafe" finds "Café"). Pure, so it is unit tested.
 */
object AppSearch {
    private val marks = Regex("\\p{Mn}+")
    private val separators = Regex("[\\s\\-_.:/]+")

    fun normalize(s: String): String =
        marks.replace(Normalizer.normalize(s, Normalizer.Form.NFD), "").lowercase().trim()

    /** Lower is better; null means no match. */
    fun score(query: String, label: String, sortKey: String = label): Int? {
        val q = normalize(query)
        if (q.isEmpty()) return null
        val l = normalize(label)
        val k = normalize(sortKey)
        val words = l.split(separators).filter { it.isNotEmpty() }
        return when {
            l == q -> 0
            l.startsWith(q) || k.startsWith(q) -> 1
            words.any { it.startsWith(q) } -> 2
            q.length >= 2 && words.joinToString("") { it.take(1) }.startsWith(q) -> 3
            l.contains(q) || k.contains(q) -> 4
            else -> null
        }
    }

    fun rank(query: String, apps: List<AppEntity>, limit: Int = 40): List<AppEntity> =
        apps.mapNotNull { app -> score(query, app.label, app.sortKey)?.let { app to it } }
            .sortedWith(compareBy<Pair<AppEntity, Int>> { it.second }.thenByDescending { it.first.launchCount }.thenBy { normalize(it.first.label) })
            .take(limit)
            .map { it.first }
}
