package ru.pikahk.outdateapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme =
    lightColorScheme(
        primary = BrandBlue,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD6E8F7),
        onPrimaryContainer = Color(0xFF0F4C7A),
        secondaryContainer = Color(0xFFD6E8F7),
        onSecondaryContainer = Color(0xFF0F4C7A),
        background = SurfaceLight,
        onBackground = InkLight,
        surface = SurfaceLight,
        onSurface = InkLight,
        surfaceVariant = SurfaceMutedLight,
        onSurfaceVariant = MutedLight,
        surfaceContainerLowest = SurfaceLight,
        surfaceContainerLow = Color(0xFFF8F9FA),
        surfaceContainer = SurfaceMutedLight,
        surfaceContainerHigh = Color(0xFFECEEF1),
        surfaceContainerHighest = DividerLight,
        outline = Color(0xFFA8ADB5),
        outlineVariant = DividerLight,
        error = RedLight,
        errorContainer = RedContainerLight
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = BrandBlueDark,
        onPrimary = Color(0xFF10202C),
        primaryContainer = Color(0xFF1E3A52),
        onPrimaryContainer = Color(0xFFCFE6F7),
        secondaryContainer = Color(0xFF1E3A52),
        onSecondaryContainer = Color(0xFFCFE6F7),
        background = SurfaceDark,
        onBackground = InkDark,
        surface = SurfaceDark,
        onSurface = InkDark,
        surfaceVariant = SurfaceMutedDark,
        onSurfaceVariant = MutedDark,
        surfaceContainerLowest = Color(0xFF0F171F),
        surfaceContainerLow = Color(0xFF1A2530),
        surfaceContainer = SurfaceMutedDark,
        surfaceContainerHigh = Color(0xFF25313E),
        surfaceContainerHighest = Color(0xFF2C3947),
        outline = Color(0xFF5B6B7A),
        outlineVariant = Color(0xFF26323E),
        error = RedDark,
        errorContainer = RedContainerDark
    )

@Composable
fun OutDateAppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
