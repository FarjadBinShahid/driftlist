package dev.farjad.driftlist

import dev.farjad.driftlist.domain.Calculator
import dev.farjad.driftlist.domain.SearchEngine
import dev.farjad.driftlist.domain.WebSuggestions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchExtrasTest {
    private fun calc(s: String) = Calculator.evaluate(s)?.let(Calculator::format)

    @Test
    fun calculatesWithPrecedenceAndBrackets() {
        assertEquals("14", calc("2+3*4"))
        assertEquals("20", calc("(2+3)*4"))
        assertEquals("512", calc("2^3^2"))
        assertEquals("-1", calc("-3+2"))
        assertEquals("2.5", calc("5/2"))
        assertEquals("0.3333333333", calc("1/3"))
        assertEquals("12", calc("15% * 80"))
        assertEquals("3", calc("sqrt(9)"))
        assertEquals("6.283185307", calc("2*pi"))
    }

    @Test
    fun acceptsPhoneKeyboardSymbols() {
        assertEquals("12", calc("3 × 4"))
        assertEquals("2", calc("8 ÷ 4"))
        assertEquals("4", calc("7 − 3"))
        assertEquals("3", calc("1,5 * 2"))
    }

    @Test
    fun staysQuietForAppNamesAndBrokenInput() {
        assertFalse(Calculator.looksLikeMath("maps"))
        assertFalse(Calculator.looksLikeMath("7"))
        assertFalse(Calculator.looksLikeMath("e"))
        assertTrue(Calculator.looksLikeMath("1+1"))
        assertNull(calc("2+"))
        assertNull(calc("(2+3"))
        assertNull(calc("1/0"))
        assertNull(calc("sqrt(-1)"))
        assertNull(calc("2048 hello"))
    }

    @Test
    fun formatsLargeAndTinyNumbers() {
        assertEquals("1e21", Calculator.format(1e21))
        assertEquals("1000000", Calculator.format(1e6))
        assertEquals("0", Calculator.format(0.0))
    }

    @Test
    fun engineUrlsEncodeTheQuery() {
        assertEquals("https://duckduckgo.com/?q=caf%C3%A9+%26+bar", SearchEngine.DuckDuckGo.searchUrl("café & bar"))
        assertNull(SearchEngine.System.searchUrl("x"))
        assertFalse(SearchEngine.System.hasSuggestions)
        assertEquals(SearchEngine.System, SearchEngine.fromName("nope"))
    }

    @Test
    fun parsesOpenSearchSuggestions() {
        val body = """["weath",["weather","weather tomorrow","Weath",""," weather radar "]]"""
        assertEquals(listOf("weather", "weather tomorrow", "weather radar"), WebSuggestions.parse(body, "weath"))
        assertEquals(emptyList<String>(), WebSuggestions.parse("<html>", "x"))
    }
}
