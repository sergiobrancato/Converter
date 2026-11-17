package dev.currency.converter.data.remote

import com.squareup.moshi.JsonClass
import dev.currency.converter.domain.model.RateTable
import java.math.BigDecimal
import java.util.Locale

/**
 * One row of `GET https://api.frankfurter.dev/v2/rates?base=EUR`.
 *
 * The v2 endpoint returns a flat array with e.g.
 * `{"date":"2026-11-17","base":"EUR","quote":"TWD","rate":36.285}`.
 * Unlike v1 it includes New Taiwan Dollar (TWD) — and also crypto/metals,
 * which [supportedEntries] filters out against the bundled currency catalog.
 */
@JsonClass(generateAdapter = false)
data class RateEntryDto(
    val date: String,
    val base: String,
    val quote: String,
    val rate: Double,
)

/**
 * Keeps only quotes the picker can select, and drops malformed rows so a bad
 * provider response can never make the converter divide by zero.
 */
internal fun List<RateEntryDto>.supportedEntries(allowedCodes: Set<String>): List<RateEntryDto> =
    filter { entry ->
        entry.base.isNotBlank() &&
            entry.quote.uppercase(Locale.ROOT) in allowedCodes &&
            entry.rate.isFinite() &&
            entry.rate > 0.0
    }

/**
 * Normalises provider rows into the domain model.
 *
 * The base currency is pinned to 1.0. Returns `null` when nothing usable arrived,
 * which the repository reports as a failed refresh (and the UI keeps the cache).
 */
internal fun List<RateEntryDto>.toRateTable(
    fetchedAtMillis: Long,
    allowedCodes: Set<String>,
): RateTable? {
    val usable = supportedEntries(allowedCodes)
    if (usable.isEmpty()) return null

    val baseCode = usable.first().base.uppercase(Locale.ROOT)
    val date = usable.maxOf { it.date }

    val normalizedRates = usable
        .associate { it.quote.uppercase(Locale.ROOT) to BigDecimal.valueOf(it.rate) }
        .toMutableMap()
        .apply { put(baseCode, BigDecimal.ONE) }

    return RateTable(
        baseCode = baseCode,
        date = date,
        fetchedAtMillis = fetchedAtMillis,
        rates = normalizedRates,
    )
}
