package com.kicklite.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.kicklite.designsystem.tokens.KdsColor
import com.kicklite.designsystem.tokens.KdsShape
import com.kicklite.designsystem.tokens.KdsTypography

private val DarkScheme = darkColorScheme(
    primary = KdsColor.Brand,
    onPrimary = Color.Black,
    primaryContainer = KdsColor.BrandContainer,
    onPrimaryContainer = Color(0xFFD7FFC4),
    secondary = KdsColor.BrandPressed,
    onSecondary = Color.Black,
    tertiary = KdsColor.Live,
    background = KdsColor.DarkBackground,
    onBackground = KdsColor.DarkText,
    surface = KdsColor.DarkSurface,
    onSurface = KdsColor.DarkText,
    surfaceVariant = KdsColor.DarkSurfaceElevated,
    onSurfaceVariant = KdsColor.DarkTextMuted,
    outline = KdsColor.DarkOutline,
    error = KdsColor.Live
)

private val LightScheme = lightColorScheme(
    primary = KdsColor.BrandContainer,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFF7B8),
    onPrimaryContainer = Color(0xFF0B2A00),
    secondary = KdsColor.BrandPressed,
    onSecondary = Color.Black,
    tertiary = KdsColor.Live,
    background = KdsColor.LightBackground,
    onBackground = KdsColor.LightText,
    surface = KdsColor.LightSurface,
    onSurface = KdsColor.LightText,
    surfaceVariant = KdsColor.LightSurfaceVariant,
    onSurfaceVariant = KdsColor.LightTextMuted,
    error = KdsColor.Live
)

/** App-independent theme entry point for the Kickstra Design System. */
@Composable
fun KickDesignSystemTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = KdsTypography.material,
        shapes = KdsShape.material,
        content = content
    )
}
