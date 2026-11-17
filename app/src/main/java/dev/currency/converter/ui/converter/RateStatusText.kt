package dev.currency.converter.ui.converter

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.currency.converter.R
import dev.currency.converter.ui.format.AmountFormatter

/**
 * Human-readable sync label shared by the main screen and the Quick Settings dialog.
 * `DateUtils` keeps "2 min. ago" localised without extra plural resources.
 */
@Composable
internal fun ratesStatusLabel(state: ConverterUiState): String {
    val table = state.rateTable
    return when {
        state.isRestoring -> stringResource(R.string.loading)
        table == null -> stringResource(
            if (state.isRefreshing) R.string.loading else R.string.rates_load_failed,
        )

        state.isRefreshing -> stringResource(R.string.rates_updating)
        // Bundled estimates are always labelled as such, even after a failed refresh.
        table.isBundled -> stringResource(R.string.rates_bundled)
        state.lastSyncFailed -> stringResource(R.string.rates_offline)
        else -> {
            val relativeTime = DateUtils.getRelativeTimeSpanString(
                table.fetchedAtMillis,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE,
            ).toString()
            stringResource(R.string.rates_updated, relativeTime)
        }
    }
}

/** Second, quieter line describing where the rates came from. */
@Composable
internal fun ratesSourceLabel(state: ConverterUiState): String {
    val table = state.rateTable ?: return stringResource(R.string.rates_source)
    val date = AmountFormatter.formatRateDate(table.date)
    return if (table.isBundled) {
        stringResource(R.string.rates_bundled_source, date)
    } else {
        stringResource(R.string.rates_source_date, date)
    }
}
