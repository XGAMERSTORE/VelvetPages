package com.xgqrscanner.modern

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B6B61),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA8F2E4),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF4A635E),
    tertiary = Color(0xFF456179),
    surface = Color(0xFFF7FAF8),
    surfaceContainer = Color(0xFFEBF1EE),
    surfaceContainerHigh = Color(0xFFE3EAE7)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8CD6C9),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF005048),
    onPrimaryContainer = Color(0xFFA8F2E4),
    secondary = Color(0xFFB1CCC5),
    tertiary = Color(0xFFADC9E6),
    background = Color(0xFF0E1513),
    surface = Color(0xFF0E1513),
    surfaceContainer = Color(0xFF18211F),
    surfaceContainerHigh = Color(0xFF222B29)
)

@Composable
fun QRScannerTheme(
    themeMode: String,
    dynamicColors: Boolean,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val dark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    val colors = when {
        dynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        content = content
    )
}
