package com.kickstra.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val KickColorScheme = darkColorScheme(
    primary = KickGreen,
    onPrimary = KickBlack,
    secondary = KickGreen,
    onSecondary = KickBlack,
    background = KickBlack,
    onBackground = KickText,
    surface = KickSurface,
    onSurface = KickText,
    surfaceVariant = KickSurface2,
    onSurfaceVariant = KickMuted,
    outline = ColorOutline,
    outlineVariant = ColorOutline.copy(alpha = .55f)
)

private val ColorOutline = androidx.compose.ui.graphics.Color(0xFF303034)
private val KickLightScheme = androidx.compose.material3.lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF18C900),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = androidx.compose.ui.graphics.Color(0xFF18C900),
    background = androidx.compose.ui.graphics.Color(0xFFF6F6F7),
    onBackground = androidx.compose.ui.graphics.Color(0xFF111113),
    surface = androidx.compose.ui.graphics.Color.White,
    onSurface = androidx.compose.ui.graphics.Color(0xFF111113),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFEDEDEF),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF65656A),
    outline = androidx.compose.ui.graphics.Color(0xFFD4D4D8)
)

@Composable
fun KickstraTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = KickBlack.toArgb()
            window.navigationBarColor = KickBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }
    MaterialTheme(colorScheme = if (darkTheme) KickColorScheme else KickLightScheme, typography = Typography, content = content)
}
