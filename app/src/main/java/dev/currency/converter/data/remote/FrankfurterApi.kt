package dev.currency.converter.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Frankfurter v2 requires no API key and publishes ECB reference rates once per
 * working day. The `rates` endpoint returns one row per quote, which is then
 * cached locally for offline conversions.
 *
 * v2 is used instead of v1 because it includes New Taiwan Dollar (TWD).
 *
 * Docs: https://frankfurter.dev/
 */
interface FrankfurterApi {
    @GET("rates")
    suspend fun latestRates(@Query("base") base: String): List<RateEntryDto>
}
