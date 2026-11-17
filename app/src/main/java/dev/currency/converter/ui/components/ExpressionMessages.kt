package dev.currency.converter.ui.components

import androidx.annotation.StringRes
import dev.currency.converter.R
import dev.currency.converter.domain.parser.ExpressionFailure

/** Maps parser failures to localised UI copy. */
@StringRes
internal fun ExpressionFailure.messageRes(): Int = when (this) {
    ExpressionFailure.INCOMPLETE_EXPRESSION -> R.string.error_incomplete_expression
    ExpressionFailure.UNBALANCED_PARENTHESES -> R.string.error_unbalanced_parentheses
    ExpressionFailure.DIVISION_BY_ZERO -> R.string.error_division_by_zero
    ExpressionFailure.INVALID_NUMBER -> R.string.error_invalid_number
    ExpressionFailure.UNEXPECTED_CHARACTER -> R.string.error_unexpected_character
    ExpressionFailure.NUMBER_TOO_LARGE -> R.string.error_number_too_large
    ExpressionFailure.TOO_COMPLEX -> R.string.error_too_complex
    ExpressionFailure.UNKNOWN -> R.string.error_unknown_expression
}
