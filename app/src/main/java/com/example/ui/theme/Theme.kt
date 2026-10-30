package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = ConvoyPrimaryCyan,
    onPrimary = Color(0xFF003642),
    primaryContainer = Color(0xFF004E5F),
    onPrimaryContainer = Color(0xFFB8EAFF),
    secondary = ConvoyAccentGreen,
    onSecondary = Color(0xFF003823),
    tertiary = ConvoyWarningAmber,
    background = ConvoyNavyDark,
    onBackground = ConvoyTextPrimaryDark,
    surface = ConvoySurfaceDark,
    onSurface = ConvoyTextPrimaryDark,
    surfaceVariant = ConvoySurfaceCard,
    onSurfaceVariant = ConvoyTextSecondaryDark,
    error = ConvoyDangerRed
)

private val LightColorScheme = lightColorScheme(
    primary = ConvoyPrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001B3E),
    secondary = ConvoyAccentGreen,
    onSecondary = Color.White,
    tertiary = ConvoyWarningAmber,
    background = ConvoyLightBg,
    onBackground = ConvoyTextPrimaryLight,
    surface = ConvoyLightSurface,
    onSurface = ConvoyTextPrimaryLight,
    surfaceVariant = ConvoyLightCard,
    onSurfaceVariant = ConvoyTextSecondaryLight,
    error = ConvoyDangerRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent navigation branding
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
