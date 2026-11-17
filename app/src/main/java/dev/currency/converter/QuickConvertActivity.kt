package dev.currency.converter

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.currency.converter.ui.converter.QuickConvertScreen
import dev.currency.converter.ui.converter.rememberConverterViewModel
import dev.currency.converter.ui.theme.ConverterTheme

/**
 * Lightweight converter launched from the Quick Settings tile.
 *
 * The activity window is translucent; [QuickConvertScreen] draws the scrim and
 * card. It uses its own ViewModel but the same DataStore as the main activity, so
 * a pair changed here is immediately visible on the main screen.
 */
class QuickConvertActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        setContent {
            ConverterTheme {
                val viewModel = rememberConverterViewModel()
                val state = viewModel.uiState.collectAsStateWithLifecycle()

                QuickConvertScreen(
                    state = state.value,
                    onInputChange = viewModel::onInputChange,
                    onSourceSelected = viewModel::onSourceSelected,
                    onTargetSelected = viewModel::onTargetSelected,
                    onSwap = viewModel::swapCurrencies,
                    onRefresh = viewModel::refreshRates,
                    onDismiss = { finish() },
                )
            }
        }
    }
}
