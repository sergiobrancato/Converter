package dev.currency.converter.domain.model

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * A complete rate table for one base currency, plus cache metadata.
 *
 * The API returns all supported rates for a single base (EUR by default), so
 * any source → target pair can be derived locally:
 *
 *     rate(from → to) = rate(to) / rate(from)
 *
 * This is what makes switching currencies instant and lets every conversion work
 * offline once one successful response has been cached.
 */
data class RateTable(
    val baseCode: String,
    val date: String,
    val fetchedAtMillis: Long,
    val rates: Map<String, BigDecimal>,
    /** True for the bundled offline snapshot rather than a real cache/network table. */
    val isBundled: Boolean = false,
) {
    fun rateBetween(fromCode: String, toCode: String): BigDecimal? {
        if (fromCode == toCode) return BigDecimal.ONE

        val fromRate = rates[fromCode] ?: return null
        val toRate = rates[toCode] ?: return null
        if (fromRate.signum() == 0) return null

        return toRate.divide(fromRate, MATH_CONTEXT)
    }

    private companion object {
        val MATH_CONTEXT = MathContext(16, RoundingMode.HALF_UP)
    }
}
