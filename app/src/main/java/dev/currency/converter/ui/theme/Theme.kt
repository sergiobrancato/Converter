package dev.currency.converter.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * App-wide Material 3 theme.
 *
 * The converter is dark-first: the near-black palette is the product design, so
 * [darkTheme] defaults to true regardless of the system setting. Pass false to
 * use the neutral light scheme (used by previews). Set [dynamicColor] to true on
 * Android 12+ to opt into wallpaper colours instead.
 */
@Composable
fun ConverterTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ConverterTypography,
        content = content,
    )
}
