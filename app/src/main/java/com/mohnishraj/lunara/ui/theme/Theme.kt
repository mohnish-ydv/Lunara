package com.mohnishraj.lunara.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkScheme = darkColorScheme(
    primary = Violet,
    onPrimary = Moon,
    primaryContainer = VioletDeep,
    onPrimaryContainer = Moon,
    secondary = Mint,
    onSecondary = Night,
    tertiary = Peach,
    background = Night,
    onBackground = Moon,
    surface = NightElevated,
    onSurface = Moon,
    surfaceVariant = NightSoft,
    onSurfaceVariant = MoonMuted,
    error = ErrorRose,
)

private val LightScheme = lightColorScheme(
    primary = VioletDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7E1FF),
    onPrimaryContainer = LightText,
    secondary = Color(0xFF007C70),
    onSecondary = Color.White,
    tertiary = Color(0xFF9A4B12),
    background = LightBackground,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = Color(0xFFEDEAF5),
    onSurfaceVariant = LightMuted,
    error = Color(0xFFBA1A1A),
)

@Composable
fun LunaraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkScheme else LightScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = LunaraTypography,
        content = content,
    )
}
