package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.viewmodel.ThemePreference

private val AppleDarkColorScheme = darkColorScheme(
    primary = iOSBlueLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A6E),
    onPrimaryContainer = Color(0xFFD4E3FF),
    secondary = iOSYellow,
    background = iOSDarkBackground,
    surface = iOSDarkSurface,
    surfaceVariant = iOSDarkSurfaceVariant,
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = iOSGray,
    outline = iOSDarkSeparator,
    error = iOSRed
)

private val AppleLightColorScheme = lightColorScheme(
    primary = iOSBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5F1FF),
    onPrimaryContainer = Color(0xFF003882),
    secondary = iOSYellow,
    background = iOSLightBackground,
    surface = iOSLightSurface,
    surfaceVariant = iOSLightSurfaceVariant,
    onBackground = Color(0xFF000000),
    onSurface = Color(0xFF000000),
    onSurfaceVariant = iOSGray,
    outline = iOSLightSeparator,
    error = iOSRed
)

@Composable
fun PureGalleryTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    dynamicColor: Boolean = false, // Keep iOS signature style by default
    content: @Composable () -> Unit
) {
    val darkTheme = when (themePreference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }

    val colorScheme = if (darkTheme) AppleDarkColorScheme else AppleLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
