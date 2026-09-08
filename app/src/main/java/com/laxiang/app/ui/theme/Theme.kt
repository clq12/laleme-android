package com.laxiang.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF6E8B7C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7E8DE),
    onPrimaryContainer = Color(0xFF1F3A2D),
    secondary = Color(0xFF8C6D5A),
    secondaryContainer = Color(0xFFFFDBCB),
    onSecondaryContainer = Color(0xFF311302),
    background = Color(0xFFF6F3EE),
    onBackground = Color(0xFF404040),
    surface = Color.White,
    onSurface = Color(0xFF404040),
    surfaceVariant = Color(0xFFEDE7DE),
    onSurfaceVariant = Color(0xFF737373),
    outline = Color(0xFFD8D2CA)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA7C9B8),
    onPrimary = Color(0xFF1E352A),
    primaryContainer = Color(0xFF375045),
    onPrimaryContainer = Color(0xFFD7E8DE),
    secondary = Color(0xFFE2BCA5),
    background = Color(0xFF1B1C1A),
    onBackground = Color(0xFFE4E2DD),
    surface = Color(0xFF222422),
    onSurface = Color(0xFFE4E2DD),
    surfaceVariant = Color(0xFF3C3F3B),
    onSurfaceVariant = Color(0xFFC4C7C1),
    outline = Color(0xFF6F736D)
)

@Composable
fun LaxiangTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
