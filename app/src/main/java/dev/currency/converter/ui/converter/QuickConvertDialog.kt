package dev.currency.converter.ui.converter

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.currency.converter.R
import dev.currency.converter.domain.model.Currency
import dev.currency.converter.domain.parser.ExpressionResult
import dev.currency.converter.ui.components.CurrencyPickerSheet
import dev.currency.converter.ui.components.ExpressionInput
import dev.currency.converter.ui.format.AmountFormatter

private const val QUICK_PICKER_NONE = ""
private const val QUICK_PICKER_SOURCE = "source"
private const val QUICK_PICKER_TARGET = "target"

/**
 * Compact converter hosted by [dev.currency.converter.QuickConvertActivity].
 *
 * Implemented as a full-screen scrim + card rather than an XML dialog so the
 * surface, keyboard behavior and light/dark colours match the main app exactly.
 * Tapping the scrim, pressing back, or the close button dismisses the activity.
 */
@Composable
fun QuickConvertScreen(
    state: ConverterUiState,
    onInputChange: (String) -> Unit,
    onSourceSelected: (Currency) -> Unit,
    onTargetSelected: (Currency) -> Unit,
    onSwap: () -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
) {
    var pickerSlot by rememberSaveable { mutableStateOf(QUICK_PICKER_NONE) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val haptics = LocalHapticFeedback.current
    val scrimInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }

    // Fast path: focus the amount immediately so the keyboard is ready to type.
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))
            .clickable(
                interactionSource = scrimInteraction,
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .systemBarsPadding()
                .imePadding(),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Box(
                // Consume taps on the card so they do not reach the dismiss scrim.
                modifier = Modifier.clickable(
                    interactionSource = cardInteraction,
                    indication = null,
                    onClick = {},
                ),
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    QuickHeader(onDismiss = onDismiss)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CompactCurrencyButton(
                            label = stringResource(R.string.from_label),
                            currency = state.sourceCurrency,
                            modifier = Modifier.weight(1f),
                            onClick = { pickerSlot = QUICK_PICKER_SOURCE },
                        )

                        FilledTonalIconButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSwap()
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SwapVert,
                                contentDescription = stringResource(R.string.swap_currencies),
                            )
                        }

                        CompactCurrencyButton(
                            label = stringResource(R.string.to_label),
                            currency = state.targetCurrency,
                            modifier = Modifier.weight(1f),
                            onClick = { pickerSlot = QUICK_PICKER_TARGET },
                        )
                    }

                    ExpressionInput(
                        value = state.input,
                        expression = state.expression,
                        onValueChange = onInputChange,
                        compact = true,
                        focusRequester = focusRequester,
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    QuickResult(state = state)

                    QuickFooter(
                        state = state,
                        onRefresh = onRefresh,
                    )
                }
            }
        }
    }

    if (pickerSlot != QUICK_PICKER_NONE) {
        val editingSource = pickerSlot == QUICK_PICKER_SOURCE
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
                pickerSlot = QUICK_PICKER_NONE
            },
            onDismiss = { pickerSlot = QUICK_PICKER_NONE },
        )
    }
}

@Composable
private fun QuickHeader(onDismiss: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.quick_dialog_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.close),
            )
        }
    }
}

@Composable
private fun CompactCurrencyButton(
    label: String,
    currency: Currency,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(
                onClickLabel = stringResource(R.string.choose_currency),
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = currency.code,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun QuickResult(state: ConverterUiState) {
    val converted = state.convertedAmount
    val displayValue = converted?.let(AmountFormatter::formatConverted)
        ?: stringResource(R.string.conversion_unavailable)
    val rate = state.crossRate

    Column {
        Text(
            text = stringResource(R.string.converted_amount),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = displayValue,
                style = MaterialTheme.typography.displaySmall,
                color = if (converted != null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = state.targetCurrency.code,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }

        if (rate != null) {
            Spacer(Modifier.height(2.dp))
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
        } else if (state.expression is ExpressionResult.Success && !state.isRestoring) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.rate_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun QuickFooter(
    state: ConverterUiState,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !state.isRefreshing, onClick = onRefresh)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
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
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = ratesStatusLabel(state),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}
