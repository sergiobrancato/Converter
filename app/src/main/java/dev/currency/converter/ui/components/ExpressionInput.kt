package dev.currency.converter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.currency.converter.R
import dev.currency.converter.domain.parser.ExpressionResult
import dev.currency.converter.ui.format.AmountFormatter

/**
 * Large editable amount field plus live expression feedback.
 *
 * The operator row is append-only: it keeps cursor handling trivial and lets the
 * field use a decimal keypad. Users can still select/edit the text directly.
 */
@Composable
fun ExpressionInput(
    value: String,
    expression: ExpressionResult,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    showOperatorRow: Boolean = true,
    focusRequester: FocusRequester? = null,
) {
    val focusManager = LocalFocusManager.current
    val inputDescription = stringResource(R.string.amount_input_description)

    val textStyle = if (compact) {
        MaterialTheme.typography.headlineMedium
    } else {
        MaterialTheme.typography.displaySmall
    }.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
    )

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.amount_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (focusRequester != null) {
                            Modifier.focusRequester(focusRequester)
                        } else {
                            Modifier
                        },
                    )
                    .semantics { contentDescription = inputDescription },
                textStyle = textStyle,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() },
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = stringResource(R.string.amount_placeholder),
                                style = textStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                            )
                        }
                        innerTextField()
                    }
                },
            )

            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        imageVector = Icons.Rounded.Clear,
                        contentDescription = stringResource(R.string.clear_amount),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        ExpressionHint(input = value, expression = expression)

        if (showOperatorRow) {
            Spacer(Modifier.height(12.dp))
            OperatorRow(
                onOperator = { operator ->
                    // ViewModel enforces the same max length as the parser.
                    onValueChange(value + operator)
                },
            )
        }
    }
}

@Composable
private fun ExpressionHint(
    input: String,
    expression: ExpressionResult,
) {
    val containsOperator = remember(input) {
        input.any { it in "+-*/×÷()−–" }
    }

    val (text, color) = when (expression) {
        is ExpressionResult.Failure -> {
            stringResource(expression.reason.messageRes()) to MaterialTheme.colorScheme.error
        }

        is ExpressionResult.Success -> {
            if (containsOperator) {
                stringResource(
                    R.string.expression_result,
                    AmountFormatter.formatExpression(expression.value),
                ) to MaterialTheme.colorScheme.primary
            } else {
                "" to MaterialTheme.colorScheme.onSurfaceVariant
            }
        }

        ExpressionResult.Empty -> {
            stringResource(R.string.amount_hint) to MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    // Always render one line of supporting copy so the card height does not jump
    // between a plain number and an evaluated expression.
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        maxLines = 2,
    )
}

@Composable
private fun OperatorRow(onOperator: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Operators.forEach { operator ->
            val actionLabel = stringResource(operator.actionLabelRes)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable(
                        onClickLabel = actionLabel,
                        role = Role.Button,
                    ) { onOperator(operator.insertion) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = operator.display,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private data class Operator(
    val display: String,
    val insertion: String,
    val actionLabelRes: Int,
)

private val Operators = listOf(
    Operator("+", "+", R.string.operator_add),
    Operator("−", "-", R.string.operator_subtract),
    Operator("×", "*", R.string.operator_multiply),
    Operator("÷", "/", R.string.operator_divide),
    Operator("(", "(", R.string.operator_open_parenthesis),
    Operator(")", ")", R.string.operator_close_parenthesis),
)
