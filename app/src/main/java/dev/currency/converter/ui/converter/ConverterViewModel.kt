package dev.currency.converter.ui.converter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.currency.converter.data.CurrencyRepository
import dev.currency.converter.data.SavedConverterState
import dev.currency.converter.di.AppContainer
import dev.currency.converter.domain.ConversionMath
import dev.currency.converter.domain.CurrencyDefaults
import dev.currency.converter.domain.model.BundledRates
import dev.currency.converter.domain.model.Currency
import dev.currency.converter.domain.model.CurrencyCatalog
import dev.currency.converter.domain.model.RateTable
import dev.currency.converter.domain.parser.ExpressionEvaluator
import dev.currency.converter.domain.parser.ExpressionResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * Everything the converter UIs need to render.
 *
 * `expression`, `crossRate` and `convertedAmount` are derived values maintained by
 * the ViewModel, so the Compose tree stays declarative and contains no business
 * logic. Both the main screen and the Quick Settings dialog observe this same shape.
 */
data class ConverterUiState(
    val input: String = CurrencyDefaults.INITIAL_INPUT,
    val sourceCurrency: Currency = CurrencyCatalog.defaultSource,
    val targetCurrency: Currency = CurrencyCatalog.defaultTarget,
    val rateTable: RateTable? = null,
    val expression: ExpressionResult = ExpressionResult.Empty,
    val crossRate: BigDecimal? = null,
    val convertedAmount: BigDecimal? = null,
    val isRestoring: Boolean = true,
    val isRefreshing: Boolean = false,
    val lastSyncFailed: Boolean = false,
    val isQuickSettingsCardDismissed: Boolean = false,
)

/**
 * Lightweight MVVM ViewModel.
 *
 * Startup sequence:
 *  1. show defaults immediately;
 *  2. restore the saved pair + cached rates from DataStore;
 *  3. refresh in the background. If that fails, cached rates stay on screen.
 */
class ConverterViewModel(
    private val repository: CurrencyRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(derive(ConverterUiState()))
    val uiState: StateFlow<ConverterUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null

    init {
        observeSavedPair()
        observeQuickSettingsCard()
        restoreSavedState()
    }

    fun onInputChange(rawInput: String) {
        // Newlines can arrive from paste; the field is intentionally single-line.
        val sanitized = rawInput
            .replace("\n", "")
            .replace("\r", "")
            .take(ExpressionEvaluator.MAX_INPUT_LENGTH)

        updateState { it.copy(input = sanitized) }
    }

    fun onSourceSelected(currency: Currency) {
        if (_uiState.value.sourceCurrency == currency) return
        updateState { it.copy(sourceCurrency = currency) }
        persistPair()
    }

    fun onTargetSelected(currency: Currency) {
        if (_uiState.value.targetCurrency == currency) return
        updateState { it.copy(targetCurrency = currency) }
        persistPair()
    }

    fun swapCurrencies() {
        updateState {
            it.copy(
                sourceCurrency = it.targetCurrency,
                targetCurrency = it.sourceCurrency,
            )
        }
        persistPair()
    }

    fun refreshRates() {
        // A user can tap refresh repeatedly; coalesce requests into one network call.
        if (refreshJob?.isActive == true) return

        refreshJob = viewModelScope.launch {
            updateState { it.copy(isRefreshing = true, lastSyncFailed = false) }

            repository.refreshRates()
                .onSuccess { table ->
                    updateState {
                        it.copy(
                            rateTable = table,
                            isRefreshing = false,
                            lastSyncFailed = false,
                        )
                    }
                }
                .onFailure {
                    // Keep the last good (or bundled) table; the status pill explains why.
                    updateState { it.copy(isRefreshing = false, lastSyncFailed = true) }
                }
        }
    }

    /** Hides the "Quick Settings tile" card for good; also called by the tile service. */
    fun dismissQuickSettingsCard() {
        updateState { it.copy(isQuickSettingsCardDismissed = true) }
        viewModelScope.launch {
            runCatching { repository.dismissQuickSettingsCard() }
        }
    }

    /**
     * Keeps the pair in sync with DataStore, including writes from the Quick
     * Settings dialog. Local selections are no-ops when the flow echoes them back,
     * so this cannot loop with [persistPair].
     */
    private fun observeSavedPair() {
        viewModelScope.launch {
            repository.observeSavedPair()
                .distinctUntilChanged()
                .collect { (sourceCode, targetCode) ->
                    // `find` heals pairs saved by an older currency list.
                    val source = CurrencyCatalog.find(sourceCode) ?: CurrencyCatalog.defaultSource
                    val target = CurrencyCatalog.find(targetCode) ?: CurrencyCatalog.defaultTarget

                    updateState {
                        if (it.sourceCurrency == source && it.targetCurrency == target) {
                            it
                        } else {
                            it.copy(sourceCurrency = source, targetCurrency = target)
                        }
                    }
                }
        }
    }

    /**
     * Mirrors the dismissed flag from DataStore. The tile service writes the same
     * key in `onTileAdded()`, so the promo card disappears without a refresh.
     */
    private fun observeQuickSettingsCard() {
        viewModelScope.launch {
            repository.observeQuickSettingsCardDismissed()
                .distinctUntilChanged()
                .collect { dismissed ->
                    updateState {
                        if (it.isQuickSettingsCardDismissed == dismissed) {
                            it
                        } else {
                            it.copy(isQuickSettingsCardDismissed = dismissed)
                        }
                    }
                }
        }
    }

    private fun restoreSavedState() {
        viewModelScope.launch {
            val saved = runCatching { repository.loadSavedState() }.getOrElse {
                // A broken preferences file should not block conversions: use defaults
                // with the bundled offline table.
                SavedConverterState(
                    sourceCurrencyCode = CurrencyDefaults.SOURCE_CURRENCY_CODE,
                    targetCurrencyCode = CurrencyDefaults.TARGET_CURRENCY_CODE,
                    rateTable = BundledRates.table,
                )
            }

            updateState {
                it.copy(
                    // The pair comes from [observeSavedPair]; only rates are restored here.
                    rateTable = saved.rateTable,
                    isRestoring = false,
                )
            }

            refreshRates()
        }
    }

    private fun persistPair() {
        val state = _uiState.value
        viewModelScope.launch {
            // Persistence is a convenience; failure must not interrupt a conversion.
            runCatching {
                repository.saveCurrencyPair(
                    sourceCode = state.sourceCurrency.code,
                    targetCode = state.targetCurrency.code,
                )
            }
        }
    }

    /**
     * Recomputes the expression and conversion whenever input, pair or rates change.
     * This keeps the UI free of parsing/formatting decisions and makes both screens
     * behave identically.
     */
    private fun updateState(transform: (ConverterUiState) -> ConverterUiState) {
        _uiState.update { current -> derive(transform(current)) }
    }

    private fun derive(state: ConverterUiState): ConverterUiState {
        val expression = ExpressionEvaluator.evaluate(state.input)
        val rate = state.rateTable?.rateBetween(
            fromCode = state.sourceCurrency.code,
            toCode = state.targetCurrency.code,
        )
        val converted = if (expression is ExpressionResult.Success && rate != null) {
            ConversionMath.convert(expression.value, rate)
        } else {
            null
        }

        return state.copy(
            expression = expression,
            crossRate = rate,
            convertedAmount = converted,
        )
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { ConverterViewModel(container.currencyRepository) }
        }
    }
}
