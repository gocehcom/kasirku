package com.kasirku.app.ui.theme

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

private val LightScheme = lightColorScheme(
    primary = Emerald40,
    onPrimary = Color.White,
    primaryContainer = Emerald95,
    onPrimaryContainer = Color(0xFF062E21),
    secondary = Sage40,
    onSecondary = Color.White,
    secondaryContainer = Sage90,
    tertiary = Amber40,
    onTertiary = Color.White,
    tertiaryContainer = Amber90,
    background = PaperLight,
    onBackground = Color(0xFF191C1A),
    surface = Color(0xFFFFFCF6),
    onSurface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFFE4E7E2),
    error = ErrorLight,
)

private val DarkScheme = darkColorScheme(
    primary = Emerald90,
    onPrimary = Color(0xFF003826),
    primaryContainer = Color(0xFF00513B),
    onPrimaryContainer = Emerald95,
    secondary = Sage90,
    tertiary = Amber90,
    background = InkDark,
    onBackground = Color(0xFFE1E3DE),
    surface = SurfaceDark,
    onSurface = Color(0xFFE1E3DE),
    surfaceVariant = Color(0xFF2A3530),
    error = ErrorDark,
)

@Composable
fun KasirKuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(LocalContext.current)
            else dynamicLightColorScheme(LocalContext.current)
        darkTheme -> DarkScheme
        else -> LightScheme
    }
    MaterialTheme(colorScheme = scheme, typography = KasirKuType, content = content)
}
