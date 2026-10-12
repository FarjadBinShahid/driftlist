package dev.farjad.letterline.domain

import org.json.JSONArray
import java.net.URLEncoder

/**
 * Search engines for "Search the web". [suggestUrl] is the engine's public autocomplete endpoint
 * in the OpenSearch suggestions format (["query", ["s1", "s2", ...]]); used only when the user
 * turns on live suggestions.
 */
enum class SearchEngine(val label: String, private val search: String?, private val suggest: String?) {
    System("Your browser's default", null, null),
    Google("Google", "https://www.google.com/search?q=%s", "https://suggestqueries.google.com/complete/search?client=firefox&q=%s"),
    DuckDuckGo("DuckDuckGo", "https://duckduckgo.com/?q=%s", "https://duckduckgo.com/ac/?type=list&q=%s"),
    Bing("Bing", "https://www.bing.com/search?q=%s", "https://api.bing.com/osjson.aspx?query=%s"),
    Brave("Brave Search", "https://search.brave.com/search?q=%s", "https://search.brave.com/api/suggest?q=%s"),
    Ecosia("Ecosia", "https://www.ecosia.org/search?q=%s", "https://ac.ecosia.org/autocomplete?type=list&q=%s"),
    Startpage("Startpage", "https://www.startpage.com/do/search?q=%s", "https://www.startpage.com/suggestions?format=opensearch&q=%s"),
    ;

    /** Null for [System]: hand the query to the system's web search instead. */
    fun searchUrl(query: String): String? = search?.format(encode(query))

    fun suggestUrl(query: String): String? = suggest?.format(encode(query))

    val hasSuggestions: Boolean get() = suggest != null

    companion object {
        fun fromName(name: String?): SearchEngine = entries.firstOrNull { it.name == name } ?: System

        private fun encode(q: String) = URLEncoder.encode(q.trim(), "UTF-8")
    }
}

object WebSuggestions {
    /** Parses an OpenSearch suggestions response; drops the query itself and blanks. */
    fun parse(body: String, query: String, limit: Int = 5): List<String> = runCatching {
        val arr = JSONArray(body).getJSONArray(1)
        (0 until arr.length())
            .map { arr.getString(it).trim() }
            .filter { it.isNotEmpty() && !it.equals(query.trim(), ignoreCase = true) }
            .distinct()
            .take(limit)
    }.getOrDefault(emptyList())
}
