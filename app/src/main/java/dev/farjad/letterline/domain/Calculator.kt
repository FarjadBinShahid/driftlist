package dev.farjad.letterline.domain

import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.PI
import kotlin.math.E
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * The calculator in search: + - * / ^ %, brackets, unary minus, sqrt(), pi and e. Accepts the
 * symbols people type on phones (× ÷ − and a decimal comma). Pure, so it is unit tested.
 */
object Calculator {
    /** Looks like a sum worth answering: has a digit and an operator, function or constant. */
    fun looksLikeMath(input: String): Boolean {
        val s = normalize(input)
        if (s.isEmpty() || s.none { it.isDigit() || it == 'π' }) return false
        if (s.length == 1) return false
        return s.any { it in "+-*/^%()" } || s.contains("sqrt") || s.contains("pi") || s.contains('π')
    }

    /** The result, or null when the input isn't a finished, valid expression (or divides by zero). */
    fun evaluate(input: String): Double? {
        if (!looksLikeMath(input)) return null
        return runCatching {
            val p = Parser(normalize(input))
            val v = p.expression()
            require(p.done()) { "trailing input" }
            v.takeIf { it.isFinite() }
        }.getOrNull()
    }

    /** "42", "0.3333333333", "1.5e21": up to ten significant digits, no trailing zeros. */
    fun format(value: Double): String {
        if (value == 0.0) return "0"
        val bd = BigDecimal(value).round(MathContext(10))
        val plain = bd.stripTrailingZeros()
        return if (plain.abs() >= BigDecimal("1e15") || (plain.abs() < BigDecimal("1e-6"))) {
            "%.6e".format(java.util.Locale.ROOT, value).replace(Regex("0+e"), "e").replace(".e", "e").replace("e+", "e")
        } else {
            plain.toPlainString()
        }
    }

    private fun normalize(input: String): String = input.trim()
        .replace('×', '*').replace('x', '*').replace('X', '*').replace('÷', '/').replace('−', '-')
        .replace(Regex("(?<=\\d),(?=\\d)"), ".")
        .replace(" ", "")
        .lowercase()

    private class Parser(private val s: String) {
        private var i = 0

        fun done() = i >= s.length

        // expression := term (('+' | '-') term)*
        fun expression(): Double {
            var v = term()
            while (i < s.length && (s[i] == '+' || s[i] == '-')) {
                val op = s[i++]
                val r = term()
                v = if (op == '+') v + r else v - r
            }
            return v
        }

        // term := power (('*' | '/') power)*
        private fun term(): Double {
            var v = power()
            while (i < s.length && (s[i] == '*' || s[i] == '/')) {
                val op = s[i++]
                val r = power()
                v = if (op == '*') {
                    v * r
                } else {
                    require(r != 0.0) { "divide by zero" }
                    v / r
                }
            }
            return v
        }

        // power := unary ('^' power)?   (right-associative)
        private fun power(): Double {
            val base = unary()
            if (i < s.length && s[i] == '^') {
                i++
                return base.pow(power())
            }
            return base
        }

        // unary := '-' unary | '+' unary | postfix
        private fun unary(): Double = when {
            i < s.length && s[i] == '-' -> { i++; -unary() }
            i < s.length && s[i] == '+' -> { i++; unary() }
            else -> postfix()
        }

        // postfix := atom '%'*
        private fun postfix(): Double {
            var v = atom()
            while (i < s.length && s[i] == '%') {
                i++
                v /= 100.0
            }
            return v
        }

        private fun atom(): Double {
            require(i < s.length) { "unexpected end" }
            return when {
                s[i] == '(' -> {
                    i++
                    val v = expression()
                    require(i < s.length && s[i] == ')') { "missing )" }
                    i++
                    v
                }
                s.startsWith("sqrt", i) -> {
                    i += 4
                    val v = atom()
                    require(v >= 0) { "sqrt of negative" }
                    sqrt(v)
                }
                s.startsWith("pi", i) -> { i += 2; PI }
                s[i] == 'π' -> { i++; PI }
                s[i] == 'e' && (i + 1 >= s.length || !s[i + 1].isLetter()) -> { i++; E }
                s[i].isDigit() || s[i] == '.' -> number()
                else -> error("unexpected ${s[i]}")
            }
        }

        private fun number(): Double {
            val start = i
            while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
            return s.substring(start, i).toDouble()
        }
    }
}
