package dev.currency.converter.domain.model

import java.math.BigDecimal

/**
 * Bundled snapshot of reference rates, used before the first successful network
 * refresh (and whenever no cache exists, e.g. a first launch in airplane mode).
 *
 * The table is EUR-based like the provider response. It is deliberately a plain
 * data object: no parsing, no I/O, and it can never crash a conversion.
 *
 * Refresh values by copying the provider's EUR table; keep the date in sync with
 * the snapshot you copied.
 */
object BundledRates {
    const val AS_OF_DATE = "2026-11-17"

    val table: RateTable = RateTable(
        baseCode = "EUR",
        date = AS_OF_DATE,
        fetchedAtMillis = 0L,
        isBundled = true,
        rates = mapOf(
            "AUD" to BigDecimal("1.6319"),
            "BRL" to BigDecimal("6.3173"),
            "CAD" to BigDecimal("1.5733"),
            "CHF" to BigDecimal("0.92442"),
            "CNY" to BigDecimal("7.3368"),
            "CZK" to BigDecimal("24.427"),
            "DKK" to BigDecimal("7.4796"),
            "EUR" to BigDecimal.ONE,
            "GBP" to BigDecimal("0.8339"),
            "HKD" to BigDecimal("8.6482"),
            "HUF" to BigDecimal("364.96"),
            "IDR" to BigDecimal("20098"),
            "ILS" to BigDecimal("3.5641"),
            "INR" to BigDecimal("103.17"),
            "ISK" to BigDecimal("136.93"),
            "JPY" to BigDecimal("175.72"),
            "KRW" to BigDecimal("1471.56"),
            "MXN" to BigDecimal("20.417"),
            "MYR" to BigDecimal("4.5069"),
            "NOK" to BigDecimal("10.5956"),
            "NZD" to BigDecimal("1.998"),
            "PHP" to BigDecimal("70.303"),
            "PLN" to BigDecimal("4.4069"),
            "RON" to BigDecimal("5.3529"),
            "SEK" to BigDecimal("11.1109"),
            "SGD" to BigDecimal("1.419"),
            "THB" to BigDecimal("37.301"),
            "TRY" to BigDecimal("55.501"),
            "TWD" to BigDecimal("36.285"),
            "USD" to BigDecimal("1.1014"),
            "ZAR" to BigDecimal("18.6443"),
        ),
    )
}
