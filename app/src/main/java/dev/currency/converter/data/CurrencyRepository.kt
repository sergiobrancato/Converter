package dev.currency.converter.data

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dev.currency.converter.data.prefs.UserPreferencesRepository
import dev.currency.converter.data.remote.FrankfurterApi
import dev.currency.converter.data.remote.RateEntryDto
import dev.currency.converter.data.remote.supportedEntries
import dev.currency.converter.data.remote.toRateTable
import dev.currency.converter.domain.CurrencyDefaults
import dev.currency.converter.domain.model.BundledRates
import dev.currency.converter.domain.model.CurrencyCatalog
import dev.currency.converter.domain.model.RateTable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Saved pair plus the last successfully cached rate table (if any). */
data class SavedConverterState(
    val sourceCurrencyCode: String,
    val targetCurrencyCode: String,
    val rateTable: RateTable,
)

/**
 * Single entry point for currency data.
 *
 * Responsibilities:
 *  - restore the last pair and cached rates from DataStore;
 *  - fetch the latest table from Frankfurter;
 *  - persist every successful response so conversions keep working offline.
 *
 * When no cache exists (fresh install, cleared storage, first launch offline) the
 * bundled [BundledRates] snapshot is used so the converter always works.
 */
class CurrencyRepository(
    private val api: FrankfurterApi,
    private val preferences: UserPreferencesRepository,
    moshi: Moshi,
) {
    private val ratesAdapter: JsonAdapter<List<RateEntryDto>> = moshi.adapter(
        Types.newParameterizedType(List::class.java, RateEntryDto::class.java),
    )

    suspend fun loadSavedState(): SavedConverterState {
        val stored = preferences.settings.first()
        return SavedConverterState(
            sourceCurrencyCode = stored.sourceCurrencyCode,
            targetCurrencyCode = stored.targetCurrencyCode,
            rateTable = decodeCachedRates(
                json = stored.cachedRatesJson,
                fetchedAtMillis = stored.cachedRatesFetchedAtMillis,
            ) ?: BundledRates.table,
        )
    }

    suspend fun saveCurrencyPair(sourceCode: String, targetCode: String) {
        preferences.saveCurrencyPair(sourceCode, targetCode)
    }

    /**
     * Emits the persisted pair on every change, whichever screen wrote it.
     *
     * This is what keeps the main screen and the Quick Settings dialog in sync:
     * each activity owns its own ViewModel, but both observe the same DataStore
     * file, so a swap in the dialog is visible in the main screen immediately.
     */
    fun observeSavedPair(): Flow<Pair<String, String>> =
        preferences.settings.map { it.sourceCurrencyCode to it.targetCurrencyCode }

    /** Whether the user (or the tile itself) dismissed the "Quick Settings" card. */
    fun observeQuickSettingsCardDismissed(): Flow<Boolean> =
        preferences.settings.map { it.quickSettingsCardDismissed }

    suspend fun dismissQuickSettingsCard() {
        preferences.setQuickSettingsCardDismissed(true)
    }

    /**
     * Returns a [Result] instead of throwing: the ViewModel treats a failed
     * refresh as a normal offline state and keeps using the cached table.
     */
    suspend fun refreshRates(): Result<RateTable> {
        return try {
            val entries = api.latestRates(CurrencyDefaults.RATES_BASE_CODE)
                .supportedEntries(CurrencyCatalog.codes)
            val fetchedAtMillis = System.currentTimeMillis()

            // `toRateTable` returns null when the payload contained nothing usable.
            val table = entries.toRateTable(
                fetchedAtMillis = fetchedAtMillis,
                allowedCodes = CurrencyCatalog.codes,
            ) ?: return Result.failure(IllegalStateException("No usable exchange rates in response"))

            // A cache-write failure must not discard a perfectly good fresh table.
            runCatching { preferences.saveCachedRates(ratesAdapter.toJson(entries), fetchedAtMillis) }

            Result.success(table)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private fun decodeCachedRates(json: String?, fetchedAtMillis: Long): RateTable? {
        if (json.isNullOrBlank() || fetchedAtMillis <= 0L) return null

        // Corrupt or outdated cache schemas simply behave like "no cache".
        return runCatching {
            ratesAdapter.fromJson(json)?.toRateTable(
                fetchedAtMillis = fetchedAtMillis,
                allowedCodes = CurrencyCatalog.codes,
            )
        }.getOrNull()
    }
}
