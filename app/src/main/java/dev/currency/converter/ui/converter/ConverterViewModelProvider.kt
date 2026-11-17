package dev.currency.converter.ui.converter

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.currency.converter.ConverterApplication

/**
 * Compose-friendly access to the ViewModel. Kept out of [ConverterViewModel] so the
 * ViewModel itself has no Compose dependencies.
 */
@Composable
fun rememberConverterViewModel(): ConverterViewModel {
    val application = LocalContext.current.applicationContext as ConverterApplication
    return viewModel(factory = ConverterViewModel.factory(application.container))
}
