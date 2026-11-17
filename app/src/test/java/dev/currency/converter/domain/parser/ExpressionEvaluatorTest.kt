package dev.currency.converter.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class ExpressionEvaluatorTest {

    @Test
    fun `empty input is treated as empty, not an error`() {
        assertEquals(ExpressionResult.Empty, ExpressionEvaluator.evaluate(""))
        assertEquals(ExpressionResult.Empty, ExpressionEvaluator.evaluate("   "))
    }

    @Test
    fun `evaluates plain numbers and decimals`() {
        assertValue("42", "42")
        assertValue("3.75", "3.75")
        assertValue("0.5", ".5")
        assertValue("-5", "-5")
    }

    @Test
    fun `respects multiplication and division precedence`() {
        assertValue("14", "2 + 3 * 4")
        assertValue("5", "10 - 20 / 4")
        assertValue("21", "6 × 7 ÷ 2")
    }

    @Test
    fun `supports nested parentheses`() {
        assertValue("20", "(2 + 3) * 4")
        assertValue("3", "((2 + 3) * (4 - 1)) / 5")
        assertValue("-14", "-2 * (3 + 4)")
        assertValue("2", "((((2))))")
    }

    @Test
    fun `supports decimal and unary edge cases`() {
        assertValue("2.5", "10 / 4")
        assertValue("5.5", "1.5 + 4")
        assertValue("6", "--2 * 3")
        assertValue("7.5", "1,5 + 6") // comma decimal from numeric keyboards
    }

    @Test
    fun `reports division by zero`() {
        assertFailure(ExpressionFailure.DIVISION_BY_ZERO, "1 / 0")
        assertFailure(ExpressionFailure.DIVISION_BY_ZERO, "5 / (2 - 2)")
    }

    @Test
    fun `reports incomplete expressions`() {
        assertFailure(ExpressionFailure.INCOMPLETE_EXPRESSION, "12 *")
        assertFailure(ExpressionFailure.INCOMPLETE_EXPRESSION, "5 +")
        assertFailure(ExpressionFailure.INCOMPLETE_EXPRESSION, ".")
        assertFailure(ExpressionFailure.INCOMPLETE_EXPRESSION, "(")
    }

    @Test
    fun `reports unbalanced parentheses`() {
        assertFailure(ExpressionFailure.UNBALANCED_PARENTHESES, "(1 + 2")
        assertFailure(ExpressionFailure.UNBALANCED_PARENTHESES, ") + 2")
        assertFailure(ExpressionFailure.UNBALANCED_PARENTHESES, "(1 + 2))")
    }

    @Test
    fun `reports unsupported characters and malformed numbers`() {
        assertFailure(ExpressionFailure.UNEXPECTED_CHARACTER, "2 + x")
        assertFailure(ExpressionFailure.UNEXPECTED_CHARACTER, "1.2.3")
        assertFailure(ExpressionFailure.UNEXPECTED_CHARACTER, "2(3 + 4)")
        assertFailure(ExpressionFailure.UNEXPECTED_CHARACTER, "1 2")
    }

    @Test
    fun `reports oversized input`() {
        assertFailure(
            ExpressionFailure.NUMBER_TOO_LARGE,
            "1234567890123456789012345",
        )
        assertFailure(
            ExpressionFailure.TOO_COMPLEX,
            "(".repeat(40) + "1" + ")".repeat(40),
        )
        assertFailure(
            ExpressionFailure.TOO_COMPLEX,
            "1+".repeat(ExpressionEvaluator.MAX_INPUT_LENGTH / 2) + "1",
        )
    }

    private fun assertValue(expected: String, expression: String) {
        val result = ExpressionEvaluator.evaluate(expression)
        assertTrue("Expected success for '$expression' but got $result", result is ExpressionResult.Success)
        val actual = (result as ExpressionResult.Success).value
        assertEquals(
            "Unexpected value for '$expression'",
            0,
            actual.compareTo(BigDecimal(expected)),
        )
    }

    private fun assertFailure(expected: ExpressionFailure, expression: String) {
        val result = ExpressionEvaluator.evaluate(expression)
        assertTrue("Expected failure for '$expression' but got $result", result is ExpressionResult.Failure)
        assertEquals(
            "Unexpected failure reason for '$expression'",
            expected,
            (result as ExpressionResult.Failure).reason,
        )
    }
}
