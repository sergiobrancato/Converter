package dev.currency.converter.ui.format

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Locale-aware number and date formatting.
 *
 * Formatting lives in one place so the main screen and the Quick Settings dialog
 * always agree, and so switching to a different display convention is a one-file edit.
 */
object AmountFormatter {

    /** Formatted evaluation result, e.g. `62.5` (grouping and up to 8 decimals). */
    fun formatExpression(value: BigDecimal): String =
        formatNumber(value, maxFractionDigits = 8)

    /**
     * Currency output is always shown with two decimals (`13,38`) so the primary
     * readout is scannable at a glance. The unit rate underneath keeps up to six.
     */
    fun formatConverted(value: BigDecimal): String =
        formatNumber(value, maxFractionDigits = 2, minFractionDigits = 2)

    /** Rate line precision: 1 DKK = 0,133781 EUR; at most 6 decimals. */
    fun formatRate(rate: BigDecimal): String =
        formatNumber(rate, maxFractionDigits = 6, minFractionDigits = 2)

    /**
     * Formats the API's ISO date (`2026-11-16`) for display. Unknown formats fall
     * back to the raw string instead of crashing the UI.
     */
    fun formatRateDate(isoDate: String): String {
        if (isoDate.isBlank()) return isoDate
        return try {
            val date = LocalDate.parse(isoDate)
            date.format(
                java.time.format.DateTimeFormatter
                    .ofLocalizedDate(FormatStyle.MEDIUM)
                    .withLocale(Locale.getDefault()),
            )
        } catch (_: DateTimeParseException) {
            isoDate
        }
    }

    private fun formatNumber(
        value: BigDecimal,
        maxFractionDigits: Int,
        minFractionDigits: Int = 0,
    ): String {
        // A new formatter per call keeps this object thread-safe; formatting is cheap
        // relative to Compose rendering and avoids stale locale configuration.
        val symbols = DecimalFormatSymbols.getInstance(Locale.getDefault())
        val formatter = DecimalFormat("#,##0", symbols).apply {
            maximumFractionDigits = maxFractionDigits
            minimumFractionDigits = minFractionDigits
            roundingMode = RoundingMode.HALF_UP
            isGroupingUsed = true
        }
        return formatter.format(value)
    }
}
