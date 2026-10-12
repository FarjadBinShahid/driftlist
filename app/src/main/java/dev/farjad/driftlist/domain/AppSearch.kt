package dev.farjad.driftlist.domain

import dev.farjad.driftlist.data.db.AppEntity
import java.text.Normalizer

/**
 * App search ranking: whole-label prefix, then word prefix, then initials ("gm" -> Google Maps),
 * then substring. Accents are ignored ("cafe" finds "Café"), and small typos still find the app, ranked last. Pure, so it is unit tested.
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
            near(q, l, words) -> 5
            else -> null
        }
    }

    /**
     * Typo tolerance: the query is within one edit (4-6 letters) or two (7+) of the start of the
     * label or one of its words. Short queries never guess. Swapped letters count as one edit.
     */
    private fun near(q: String, label: String, words: List<String>): Boolean {
        if (q.length < 4) return false
        val max = if (q.length <= 6) 1 else 2
        return (words + label.replace(" ", "")).any { w ->
            (q.length - 1..q.length + 1).any { n -> n <= w.length && n > 0 && distance(q, w.substring(0, n)) <= max }
        }
    }

    /** Optimal string alignment distance (Levenshtein plus adjacent swaps). */
    private fun distance(a: String, b: String): Int {
        val d = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) d[i][0] = i
        for (j in 0..b.length) d[0][j] = j
        for (i in 1..a.length) for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            d[i][j] = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
            if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) d[i][j] = minOf(d[i][j], d[i - 2][j - 2] + 1)
        }
        return d[a.length][b.length]
    }

    fun rank(query: String, apps: List<AppEntity>, limit: Int = 40): List<AppEntity> =
        apps.mapNotNull { app -> score(query, app.label, app.sortKey)?.let { app to it } }
            .sortedWith(compareBy<Pair<AppEntity, Int>> { it.second }.thenByDescending { it.first.launchCount }.thenBy { normalize(it.first.label) })
            .take(limit)
            .map { it.first }
}
