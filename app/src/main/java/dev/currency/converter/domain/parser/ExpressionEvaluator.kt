package dev.currency.converter.domain.parser

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Outcome of evaluating the text in the amount field.
 *
 * Empty input is deliberately distinct from an error: the UI shows a friendly
 * hint instead of a red validation message while the user is just getting started.
 */
sealed interface ExpressionResult {
    data class Success(val value: BigDecimal) : ExpressionResult
    data object Empty : ExpressionResult
    data class Failure(val reason: ExpressionFailure) : ExpressionResult
}

/**
 * Machine-readable failures. Keeping the parser free of Android string resources
 * lets it be unit tested on the JVM and localised by the UI layer.
 */
enum class ExpressionFailure {
    INCOMPLETE_EXPRESSION,
    UNBALANCED_PARENTHESES,
    DIVISION_BY_ZERO,
    INVALID_NUMBER,
    UNEXPECTED_CHARACTER,
    NUMBER_TOO_LARGE,
    TOO_COMPLEX,
    UNKNOWN,
}

/**
 * A tiny, dependency-free arithmetic evaluator for the converter input.
 *
 * Supported grammar (recursive descent):
 *
 *     expression := term (('+' | '-') term)*
 *     term       := factor (('*' | '/') factor)*
 *     factor     := ('+' | '-') factor | '(' expression ')' | number
 *     number     := digits ('.' digits*)? | '.' digits+
 *
 * Notes:
 *  - Precedence and parentheses fall naturally out of the grammar.
 *  - BigDecimal keeps currency-sized numbers accurate (no 0.1 + 0.2 artefacts).
 *  - A comma is accepted as a decimal separator because many numeric mobile
 *    keyboards emit one. Thousands separators are intentionally not supported.
 *  - Implicit multiplication (e.g. `2(3 + 4)`) is not supported; use `2 * (...)`.
 */
object ExpressionEvaluator {

    /** Input is capped so a paste cannot make parsing or formatting expensive. */
    const val MAX_INPUT_LENGTH = 120

    private const val MAX_NUMBER_DIGITS = 24
    private const val MAX_PARENTHESIS_DEPTH = 32
    private val CALCULATION_CONTEXT = MathContext(16, RoundingMode.HALF_UP)

    fun evaluate(expression: String): ExpressionResult {
        val normalized = normalize(expression)
        if (normalized.isEmpty()) return ExpressionResult.Empty
        if (normalized.length > MAX_INPUT_LENGTH) {
            return ExpressionResult.Failure(ExpressionFailure.TOO_COMPLEX)
        }

        return try {
            ExpressionResult.Success(Parser(normalized).parse())
        } catch (parseFailure: ExpressionParseException) {
            ExpressionResult.Failure(parseFailure.reason)
        } catch (_: ArithmeticException) {
            // Defensive fallback; divide-by-zero is caught explicitly in the parser.
            ExpressionResult.Failure(ExpressionFailure.UNKNOWN)
        }
    }

    /**
     * Accepts the typographic operators people get from keyboards or pasted text,
     * and treats comma as a decimal separator. Whitespace is left for the parser.
     */
    private fun normalize(raw: String): String = raw
        .replace('\u00A0', ' ') // non-breaking space
        .replace('×', '*')
        .replace('÷', '/')
        .replace('−', '-') // U+2212 minus sign
        .replace('–', '-') // U+2013 en dash
        .replace(',', '.')
        .trim()

    private class Parser(private val source: String) {
        private var position = 0
        private var parenthesisDepth = 0

        fun parse(): BigDecimal {
            val value = parseExpression()
            skipWhitespace()
            // Anything left over is outside the grammar. A stray ')' is specifically
            // reported as an unbalanced parenthesis rather than a generic character error.
            if (position < source.length) {
                throw fail(
                    if (peek() == ')') {
                        ExpressionFailure.UNBALANCED_PARENTHESES
                    } else {
                        ExpressionFailure.UNEXPECTED_CHARACTER
                    },
                )
            }
            return value
        }

        private fun parseExpression(): BigDecimal {
            var value = parseTerm()
            while (true) {
                skipWhitespace()
                val operator = peek() ?: return value
                if (operator != '+' && operator != '-') return value

                position++ // consume the operator
                val right = parseTerm()
                value = if (operator == '+') {
                    value.add(right, CALCULATION_CONTEXT)
                } else {
                    value.subtract(right, CALCULATION_CONTEXT)
                }
            }
        }

        private fun parseTerm(): BigDecimal {
            var value = parseFactor()
            while (true) {
                skipWhitespace()
                when (peek()) {
                    '*' -> {
                        position++
                        value = value.multiply(parseFactor(), CALCULATION_CONTEXT)
                    }

                    '/' -> {
                        position++
                        val divisor = parseFactor()
                        if (divisor.signum() == 0) throw fail(ExpressionFailure.DIVISION_BY_ZERO)
                        value = value.divide(divisor, CALCULATION_CONTEXT)
                    }

                    else -> return value
                }
            }
        }

        private fun parseFactor(): BigDecimal {
            skipWhitespace()
            return when (peek()) {
                null -> throw fail(ExpressionFailure.INCOMPLETE_EXPRESSION)
                '+' -> {
                    position++
                    parseFactor()
                }

                '-' -> {
                    position++
                    parseFactor().negate(CALCULATION_CONTEXT)
                }

                '(' -> parseParenthesized()
                ')' -> throw fail(ExpressionFailure.UNBALANCED_PARENTHESES)
                else -> parseNumber()
            }
        }

        private fun parseParenthesized(): BigDecimal {
            if (parenthesisDepth >= MAX_PARENTHESIS_DEPTH) {
                throw fail(ExpressionFailure.TOO_COMPLEX)
            }
            parenthesisDepth++
            position++ // consume '('

            val value = parseExpression()
            skipWhitespace()
            if (peek() != ')') throw fail(ExpressionFailure.UNBALANCED_PARENTHESES)
            position++ // consume ')'
            parenthesisDepth--
            return value
        }

        private fun parseNumber(): BigDecimal {
            val start = position
            var digitCount = 0

            while (peek()?.isDigit() == true) {
                position++
                digitCount++
            }
            if (peek() == '.') {
                position++
                while (peek()?.isDigit() == true) {
                    position++
                    digitCount++
                }
            }

            if (digitCount == 0) {
                throw fail(
                    if (peek() == null) {
                        ExpressionFailure.INCOMPLETE_EXPRESSION
                    } else {
                        ExpressionFailure.UNEXPECTED_CHARACTER
                    },
                )
            }
            if (digitCount > MAX_NUMBER_DIGITS) throw fail(ExpressionFailure.NUMBER_TOO_LARGE)

            val token = source.substring(start, position)
            return try {
                BigDecimal(token)
            } catch (_: NumberFormatException) {
                throw fail(ExpressionFailure.INVALID_NUMBER)
            }
        }

        private fun skipWhitespace() {
            while (peek()?.isWhitespace() == true) position++
        }

        private fun peek(): Char? = source.getOrNull(position)

        private fun fail(reason: ExpressionFailure) = ExpressionParseException(reason)
    }

    private class ExpressionParseException(val reason: ExpressionFailure) : Exception()
}
