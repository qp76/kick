package com.kicklite.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = KickGreen,
    onPrimary = Color.Black,
    primaryContainer = KickGreenDeep,
    onPrimaryContainer = Color(0xFFD7FFC4),
    secondary = KickGreenDim,
    onSecondary = Color.Black,
    tertiary = KickLiveRed,
    background = KickDark,
    onBackground = KickTextPrimary,
    surface = KickGray,
    onSurface = KickTextPrimary,
    surfaceVariant = KickSurfaceHigh,
    onSurfaceVariant = KickTextSecondary,
    outline = KickOutline,
    error = KickLiveRed
)

private val LightColorScheme = lightColorScheme(
    primary = KickGreenDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFF7B8),
    onPrimaryContainer = Color(0xFF0B2A00),
    secondary = KickGreenDim,
    onSecondary = Color.Black,
    tertiary = KickLiveRed,
    background = KickLightBackground,
    onBackground = KickLightText,
    surface = KickLightSurface,
    onSurface = KickLightText,
    surfaceVariant = KickLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF4A5750),
    error = KickLiveRed
)

private val KickShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun KickLiteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Off by default so the app keeps its Kick-green identity instead of wallpaper colors
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Transparent bars (edge-to-edge) with icons that stay readable in both themes
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = KickShapes,
        content = content
    )
}
