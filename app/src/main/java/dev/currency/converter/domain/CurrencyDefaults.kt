package dev.currency.converter.domain

/**
 * Single source of truth for the values a developer is most likely to change.
 * See README → Customization Guide.
 */
object CurrencyDefaults {
    /** Currency selected on a fresh install. */
    const val SOURCE_CURRENCY_CODE = "EUR"

    /** Currency the fresh-install source amount is converted into. */
    const val TARGET_CURRENCY_CODE = "USD"

    /** Initial text in the amount field. */
    const val INITIAL_INPUT = "100"

    /**
     * Frankfurter v2 base URL. Swap this (and the Retrofit DTO if needed) to use a
     * different free rate provider. The URL must end with a slash for Retrofit.
     *
     * v1 (`/v1/latest`) does not publish TWD; v2 does.
     */
    const val API_BASE_URL = "https://api.frankfurter.dev/v2/"

    /** Base currency of the fetched table; all pairs are derived locally. */
    const val RATES_BASE_CODE = "EUR"

    /** Network timeouts in seconds, kept short so the UI falls back to cache quickly. */
    const val NETWORK_TIMEOUT_SECONDS = 15L
}
