package dev.farjad.driftlist.domain

import android.icu.text.AlphabeticIndex
import android.icu.text.Transliterator
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Locale-aware alphabet buckets for the app list and the wave alphabet.
 *
 * Uses ICU's AlphabeticIndex for the device locales (so Swedish gets Å Ä Ö after Z, Spanish gets Ñ),
 * transliterates non-Latin names to Latin where the OS allows (API 29+), and files anything
 * that still does not fit under [OTHER].
 */
class AlphabetIndexer private constructor(locales: List<Locale>) {
    private val primary: Locale = locales.firstOrNull() ?: Locale.getDefault()
    private val index: AlphabeticIndex.ImmutableIndex<Any>
    private val transliterator: Transliterator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { Transliterator.getInstance("Any-Latin") }.getOrNull()
        } else {
            null
        }

    /** Letters in display order, always ending with [OTHER]. */
    val letters: List<String>

    init {
        val builder = AlphabeticIndex<Any>(primary)
        locales.drop(1).forEach { builder.addLabels(it) }
        builder.addLabels(Locale.ENGLISH)
        index = builder.buildImmutableIndex()
        letters = (0 until index.bucketCount)
            .map { index.getBucket(it) }
            .filter { it.labelType == AlphabeticIndex.Bucket.LabelType.NORMAL }
            .map { it.label } + OTHER
    }

    fun sortKey(label: String): String = latin(label).trim().lowercase(primary)

    fun bucketOf(label: String): String {
        val key = latin(label).trim()
        if (key.isEmpty()) return OTHER
        val bucket = index.getBucket(index.getBucketIndex(key))
        return if (bucket.labelType == AlphabeticIndex.Bucket.LabelType.NORMAL) bucket.label else OTHER
    }

    private fun latin(label: String): String {
        val t = transliterator ?: return label
        val needsIt = label.any { ch ->
            when (Character.UnicodeScript.of(ch.code)) {
                Character.UnicodeScript.LATIN,
                Character.UnicodeScript.COMMON,
                Character.UnicodeScript.INHERITED -> false
                else -> true
            }
        }
        return if (needsIt) t.transliterate(label) else label
    }

    companion object {
        const val OTHER = "#"

        fun forCurrentLocales(): AlphabetIndexer {
            val list = LocaleList.getDefault()
            return AlphabetIndexer((0 until list.size()).map { list[it] })
        }
    }
}
