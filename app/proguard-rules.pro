# Keep the reflection-based Moshi models in case minification is enabled.
# The sample ships with minify disabled, but these rules make opt-in safe.
-keep class dev.currency.converter.data.remote.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers,allowobfuscation class * {
    @com.squareup.moshi.Json <fields>;
}

# Retrofit interfaces are resolved reflectively.
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# OkHttp platform hooks.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
