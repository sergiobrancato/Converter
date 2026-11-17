package dev.currency.converter.di

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import dev.currency.converter.BuildConfig
import dev.currency.converter.data.CurrencyRepository
import dev.currency.converter.data.prefs.UserPreferencesRepository
import dev.currency.converter.data.remote.FrankfurterApi
import dev.currency.converter.domain.CurrencyDefaults
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Minimal manual dependency container. No DI framework: there are only three
 * long-lived objects, so keeping construction explicit is easier to follow.
 */
class AppContainer(context: Context) {

    private val moshi: Moshi = Moshi.Builder()
        // KotlinJsonAdapterFactory enables the reflection-based @JsonClass models.
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CurrencyDefaults.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(CurrencyDefaults.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(CurrencyDefaults.NETWORK_TIMEOUT_SECONDS * 2, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    },
                )
            }
        }
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(CurrencyDefaults.API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val frankfurterApi: FrankfurterApi = retrofit.create(FrankfurterApi::class.java)

    private val userPreferencesRepository = UserPreferencesRepository(context)

    val currencyRepository = CurrencyRepository(
        api = frankfurterApi,
        preferences = userPreferencesRepository,
        moshi = moshi,
    )
}
