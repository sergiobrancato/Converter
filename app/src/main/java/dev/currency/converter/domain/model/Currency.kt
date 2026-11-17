package dev.currency.converter.domain.model

import dev.currency.converter.domain.CurrencyDefaults
import java.util.Locale

/** One ISO 4217 currency shown in the picker. */
data class Currency(
    val code: String,
    val name: String,
    /** Conventional symbol, e.g. `NT$`. Shown in the picker as a hint. */
    val symbol: String,
)

/**
 * The currencies supported by the Frankfurter v2 API (ECB reference rates plus
 * additional published quotes).
 *
 * This list is bundled so the picker works offline and on first launch. Keep it
 * in sync if you change providers, or replace it with the provider's
 * `/currencies` endpoint — see the README Customization Guide.
 */
object CurrencyCatalog {
    val all: List<Currency> = listOf(
        Currency("AUD", "Australian Dollar", "\$"),
        Currency("BRL", "Brazilian Real", "R\$"),
        Currency("CAD", "Canadian Dollar", "\$"),
        Currency("CHF", "Swiss Franc", "CHF"),
        Currency("CNY", "Chinese Yuan", "¥"),
        Currency("CZK", "Czech Koruna", "Kč"),
        Currency("DKK", "Danish Krone", "kr."),
        Currency("EUR", "Euro", "€"),
        Currency("GBP", "British Pound", "£"),
        Currency("HKD", "Hong Kong Dollar", "\$"),
        Currency("HUF", "Hungarian Forint", "Ft"),
        Currency("IDR", "Indonesian Rupiah", "Rp"),
        Currency("ILS", "Israeli New Shekel", "₪"),
        Currency("INR", "Indian Rupee", "₹"),
        Currency("ISK", "Icelandic Krona", "kr."),
        Currency("JPY", "Japanese Yen", "¥"),
        Currency("KRW", "South Korean Won", "₩"),
        Currency("MXN", "Mexican Peso", "\$"),
        Currency("MYR", "Malaysian Ringgit", "RM"),
        Currency("NOK", "Norwegian Krone", "kr"),
        Currency("NZD", "New Zealand Dollar", "\$"),
        Currency("PHP", "Philippine Peso", "₱"),
        Currency("PLN", "Polish Zloty", "zł"),
        Currency("RON", "Romanian Leu", "Lei"),
        Currency("SEK", "Swedish Krona", "kr"),
        Currency("SGD", "Singapore Dollar", "\$"),
        Currency("THB", "Thai Baht", "฿"),
        Currency("TRY", "Turkish Lira", "₺"),
        Currency("TWD", "New Taiwan Dollar", "NT\$"),
        Currency("USD", "United States Dollar", "\$"),
        Currency("ZAR", "South African Rand", "R"),
    ).sortedBy { it.code }

    private val byCode: Map<String, Currency> = all.associateBy { it.code }

    /** Codes used to filter provider responses down to the bundled catalog. */
    val codes: Set<String> = all.mapTo(LinkedHashSet()) { it.code }

    val defaultSource: Currency = byCode.getValue(CurrencyDefaults.SOURCE_CURRENCY_CODE)
    val defaultTarget: Currency = byCode.getValue(CurrencyDefaults.TARGET_CURRENCY_CODE)

    /** Case-insensitive lookup with a safe null result for stale saved settings. */
    fun find(code: String): Currency? = byCode[code.uppercase(Locale.ROOT)]
}
