package dev.currency.converter.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.currency.converter.domain.CurrencyDefaults
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * One process-wide DataStore instance. Creating it at file scope guarantees that
 * every activity and the Quick Settings launcher share the same store.
 */
private val Context.converterDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "converter_preferences",
)

/** Snapshot of everything persisted between launches. */
data class StoredPreferences(
    val sourceCurrencyCode: String = CurrencyDefaults.SOURCE_CURRENCY_CODE,
    val targetCurrencyCode: String = CurrencyDefaults.TARGET_CURRENCY_CODE,
    val cachedRatesJson: String? = null,
    val cachedRatesFetchedAtMillis: Long = 0L,
    val quickSettingsCardDismissed: Boolean = false,
)

/**
 * Thin, intentionally boring wrapper around Preferences DataStore.
 *
 * Why DataStore: writes are transactional and observed as a Flow, so a currency
 * picked in the Quick Settings dialog is reflected by the main screen immediately,
 * without any custom file handling. The rate payload is ~1 KB, which comfortably
 * fits Preferences DataStore.
 */
class UserPreferencesRepository(context: Context) {

    private val dataStore = context.applicationContext.converterDataStore

    val settings: Flow<StoredPreferences> = dataStore.data
        // A corrupt/unreadable file should degrade to defaults, not crash the app.
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            StoredPreferences(
                sourceCurrencyCode = preferences[Keys.SOURCE_CURRENCY]
                    ?: CurrencyDefaults.SOURCE_CURRENCY_CODE,
                targetCurrencyCode = preferences[Keys.TARGET_CURRENCY]
                    ?: CurrencyDefaults.TARGET_CURRENCY_CODE,
                cachedRatesJson = preferences[Keys.CACHED_RATES_JSON],
                cachedRatesFetchedAtMillis = preferences[Keys.CACHED_RATES_FETCHED_AT] ?: 0L,
                quickSettingsCardDismissed = preferences[Keys.QUICK_SETTINGS_CARD_DISMISSED] ?: false,
            )
        }

    suspend fun saveCurrencyPair(sourceCode: String, targetCode: String) {
        dataStore.edit { preferences ->
            // Both currencies are written in one transaction to avoid a mixed pair
            // if the process dies halfway through a swap.
            preferences[Keys.SOURCE_CURRENCY] = sourceCode
            preferences[Keys.TARGET_CURRENCY] = targetCode
        }
    }

    suspend fun saveCachedRates(json: String, fetchedAtMillis: Long) {
        dataStore.edit { preferences ->
            preferences[Keys.CACHED_RATES_JSON] = json
            preferences[Keys.CACHED_RATES_FETCHED_AT] = fetchedAtMillis
        }
    }

    suspend fun setQuickSettingsCardDismissed(dismissed: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.QUICK_SETTINGS_CARD_DISMISSED] = dismissed
        }
    }

    private object Keys {
        val SOURCE_CURRENCY = stringPreferencesKey("source_currency")
        val TARGET_CURRENCY = stringPreferencesKey("target_currency")
        val CACHED_RATES_JSON = stringPreferencesKey("cached_rates_json")
        val CACHED_RATES_FETCHED_AT = longPreferencesKey("cached_rates_fetched_at")
        val QUICK_SETTINGS_CARD_DISMISSED = booleanPreferencesKey("quick_settings_card_dismissed")
    }
}
