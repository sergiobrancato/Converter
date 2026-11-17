package dev.currency.converter.ui.converter

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.currency.converter.R
import dev.currency.converter.domain.model.Currency
import dev.currency.converter.domain.model.CurrencyCatalog
import dev.currency.converter.domain.model.RateTable
import dev.currency.converter.domain.parser.ExpressionResult
import dev.currency.converter.tile.QuickSettingsTileHelper
import dev.currency.converter.ui.components.CurrencyPickerSheet
import dev.currency.converter.ui.components.ExpressionInput
import dev.currency.converter.ui.format.AmountFormatter
import dev.currency.converter.ui.theme.ConverterTheme
import dev.currency.converter.util.findActivity
import java.math.BigDecimal

private const val PICKER_NONE = ""
private const val PICKER_SOURCE = "source"
private const val PICKER_TARGET = "target"

/** Gap between the From and To cards; the swap button straddles its centre. */
private val CardGap = 10.dp
private val SwapButtonSize = 44.dp

/**
 * Stateful entry point: owns the ViewModel and translates UI events into calls.
 * [ConverterScreen] below stays stateless and is therefore trivial to preview/test.
 */
@Composable
fun ConverterRoute(
    viewModel: ConverterViewModel = rememberConverterViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    ConverterScreen(
        state = state,
        onInputChange = viewModel::onInputChange,
        onSourceSelected = viewModel::onSourceSelected,
        onTargetSelected = viewModel::onTargetSelected,
        onSwap = viewModel::swapCurrencies,
        onRefresh = viewModel::refreshRates,
        onDismissQuickSettingsCard = viewModel::dismissQuickSettingsCard,
        onAddQuickSettingsTile = {
            activity?.let(QuickSettingsTileHelper::requestAddTile)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(
    state: ConverterUiState,
    onInputChange: (String) -> Unit,
    onSourceSelected: (Currency) -> Unit,
    onTargetSelected: (Currency) -> Unit,
    onSwap: () -> Unit,
    onRefresh: () -> Unit,
    onDismissQuickSettingsCard: () -> Unit,
    onAddQuickSettingsTile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pickerSlot by rememberSaveable { mutableStateOf(PICKER_NONE) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = stringResource(R.string.app_tagline),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            ConverterCards(
                state = state,
                onSourceClick = { pickerSlot = PICKER_SOURCE },
                onTargetClick = { pickerSlot = PICKER_TARGET },
                onInputChange = onInputChange,
                onSwap = onSwap,
            )

            RateStatusPill(
                state = state,
                onRefresh = onRefresh,
            )

            if (!state.isQuickSettingsCardDismissed) {
                QuickSettingsCard(
                    onDismiss = onDismissQuickSettingsCard,
                    onAddTile = onAddQuickSettingsTile,
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (pickerSlot != PICKER_NONE) {
        val editingSource = pickerSlot == PICKER_SOURCE
        CurrencyPickerSheet(
            title = stringResource(
                if (editingSource) R.string.select_source_currency
                else R.string.select_target_currency,
            ),
            selectedCode = if (editingSource) {
                state.sourceCurrency.code
            } else {
                state.targetCurrency.code
            },
            onSelect = { currency ->
                if (editingSource) onSourceSelected(currency) else onTargetSelected(currency)
                pickerSlot = PICKER_NONE
            },
            onDismiss = { pickerSlot = PICKER_NONE },
        )
    }
}

/**
 * From/To cards with a shared seam. Their heights depend on font scale, so the
 * swap button is positioned from the measured source-card height instead of a
 * fixed offset; that keeps it exactly on the 10dp gap on every device.
 */
@Composable
private fun ConverterCards(
    state: ConverterUiState,
    onSourceClick: () -> Unit,
    onTargetClick: () -> Unit,
    onInputChange: (String) -> Unit,
    onSwap: () -> Unit,
) {
    var sourceHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(CardGap)) {
            SourceCard(
                state = state,
                onCurrencyClick = onSourceClick,
                onInputChange = onInputChange,
                modifier = Modifier.onSizeChanged { sourceHeightPx = it.height },
            )
            TargetCard(
                state = state,
                onCurrencyClick = onTargetClick,
            )
        }

        if (sourceHeightPx > 0) {
            // Button centre = source bottom + half the gap.
            val sourceHeight = with(density) { sourceHeightPx.toDp() }
            val offsetY = sourceHeight + CardGap / 2 - SwapButtonSize / 2

            SwapButton(
                onSwap = onSwap,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = offsetY),
            )
        }
    }
}

@Composable
private fun SourceCard(
    state: ConverterUiState,
    onCurrencyClick: () -> Unit,
    onInputChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ConverterCard(modifier = modifier) {
        CurrencySelector(
            label = stringResource(R.string.from_label),
            currency = state.sourceCurrency,
            onClick = onCurrencyClick,
        )
        Spacer(Modifier.height(14.dp))

        ExpressionInput(
            value = state.input,
            expression = state.expression,
            onValueChange = onInputChange,
        )

        // Clearance for the swap button that sits on the shared seam.
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun TargetCard(
    state: ConverterUiState,
    onCurrencyClick: () -> Unit,
) {
    val converted = state.convertedAmount
    val displayValue = converted?.let(AmountFormatter::formatConverted)
        ?: stringResource(R.string.conversion_unavailable)
    val rate = state.crossRate

    ConverterCard {
        // Clearance for the swap button that sits on the shared seam.
        Spacer(Modifier.height(10.dp))

        CurrencySelector(
            label = stringResource(R.string.to_label),
            currency = state.targetCurrency,
            onClick = onCurrencyClick,
        )
        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.converted_amount),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = displayValue,
                style = MaterialTheme.typography.displayMedium,
                color = if (converted != null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = state.targetCurrency.code,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        Spacer(Modifier.height(8.dp))

        when {
            rate != null -> {
                Text(
                    text = stringResource(
                        R.string.rate_template,
                        state.sourceCurrency.code,
                        AmountFormatter.formatRate(rate),
                        state.targetCurrency.code,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            state.expression is ExpressionResult.Success && !state.isRestoring -> {
                Text(
                    text = stringResource(R.string.rate_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/** Charcoal card with the single hairline border used across the screen. */
@Composable
private fun ConverterCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun SwapButton(
    onSwap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current

    FilledTonalIconButton(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onSwap()
        },
        modifier = modifier.size(SwapButtonSize),
        shape = CircleShape,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Icon(
            imageVector = Icons.Rounded.SwapVert,
            contentDescription = stringResource(R.string.swap_currencies),
        )
    }
}

@Composable
private fun CurrencySelector(
    label: String,
    currency: Currency,
    onClick: () -> Unit,
) {
    val description = stringResource(
        R.string.currency_selector_description,
        label,
        currency.code,
        currency.name,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                onClickLabel = stringResource(R.string.choose_currency),
                role = Role.Button,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = currency.code,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = currency.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Consolidated refresh control: the whole sync pill is the button, and the spinner
 * replaces the status icon while a request is in flight.
 */
@Composable
private fun RateStatusPill(
    state: ConverterUiState,
    onRefresh: () -> Unit,
) {
    val status = ratesStatusLabel(state)
    val source = ratesSourceLabel(state)

    Surface(
        onClick = onRefresh,
        enabled = !state.isRefreshing,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                val icon = when {
                    state.lastSyncFailed -> Icons.Rounded.CloudOff
                    state.rateTable?.isBundled == true -> Icons.Rounded.Schedule
                    state.rateTable == null -> Icons.Rounded.Schedule
                    else -> Icons.Rounded.CloudDone
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (state.lastSyncFailed) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = source,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun QuickSettingsCard(
    onDismiss: () -> Unit,
    onAddTile: () -> Unit,
) {
    // Android 13 introduced a supported in-app "add tile" prompt. Older versions
    // still support the tile, but users add it from the Quick Settings edit screen.
    val canRequestTile = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    ConverterCard {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Widgets,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.quick_settings_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(
                        if (canRequestTile) R.string.quick_settings_body
                        else R.string.quick_settings_manual,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (canRequestTile) {
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(onClick = onAddTile) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.quick_settings_add))
                    }
                }
            }

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.dismiss_quick_settings_card),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Preview — no ViewModel or network required.
// ---------------------------------------------------------------------------
private val PreviewRates = RateTable(
    baseCode = "EUR",
    date = "2026-11-16",
    fetchedAtMillis = System.currentTimeMillis(),
    rates = mapOf(
        "EUR" to BigDecimal.ONE,
        "USD" to BigDecimal("1.1014"),
        "GBP" to BigDecimal("0.8345"),
        "TWD" to BigDecimal("36.285"),
    ),
)

@Preview(name = "Converter — dark", showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun ConverterScreenPreview() {
    ConverterTheme {
        ConverterScreen(
            state = ConverterUiState(
                input = "12.5 * (3 + 2)",
                rateTable = PreviewRates,
                expression = ExpressionResult.Success(BigDecimal("62.5")),
                crossRate = BigDecimal("1.1014"),
                convertedAmount = BigDecimal("68.8375"),
                isRestoring = false,
            ),
            onInputChange = {},
            onSourceSelected = {},
            onTargetSelected = {},
            onSwap = {},
            onRefresh = {},
            onDismissQuickSettingsCard = {},
            onAddQuickSettingsTile = {},
        )
    }
}
