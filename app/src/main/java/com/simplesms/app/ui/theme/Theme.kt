package com.simplesms.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SimpleSMSColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = BrandBlueLight,
    onPrimaryContainer = BrandBlueDark,
    secondary = BrandIndigoAccent,
    onSecondary = Color.White,
    secondaryContainer = BrandIndigoLight,
    onSecondaryContainer = BrandIndigoAccent,
    tertiary = BrandWarning,
    onTertiary = Color.White,
    background = BrandBackground,
    onBackground = BrandTextPrimary,
    surface = BrandSurface,
    onSurface = BrandTextPrimary,
    surfaceVariant = BrandSurfaceSecondary,
    onSurfaceVariant = BrandTextSecondary,
    outline = BrandBorder,
    error = BrandError,
    onError = Color.White,
    errorContainer = BrandErrorBg,
    onErrorContainer = BrandError
)

@Composable
fun SimpleSMSTheme(
    darkTheme: Boolean = false,
    darkIntensity: Float = 0f,
    content: @Composable () -> Unit
) {
    // Pure professional light design system as instructed
    MaterialTheme(
        colorScheme = SimpleSMSColorScheme,
        typography = Typography,
        content = content
    )
}
