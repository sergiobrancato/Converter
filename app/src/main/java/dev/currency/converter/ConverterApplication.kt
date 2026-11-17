package dev.currency.converter

import android.app.Application
import dev.currency.converter.di.AppContainer

/**
 * Application-scoped container. Both the main activity and the Quick Settings
 * dialog obtain the same repository, so they read and write one DataStore.
 */
class ConverterApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
