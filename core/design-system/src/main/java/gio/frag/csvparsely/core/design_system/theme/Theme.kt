package gio.frag.csvparsely.core.design_system.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = RabobankBlue,
    onPrimary = Color.White,
    primaryContainer = RabobankBlue,
    onPrimaryContainer = Color.White,
    tertiary = RabobankOrange,
    background = Color(0xFFF4F6F9),
    onBackground = Color(0xFF191C20),
    surfaceContainerLow = Color.White,
    onSurface = Color(0xFF191C20),
    onSurfaceVariant = Color(0xFF4A5361),
    inverseSurface = Color(0xFF2E3135),
    inverseOnSurface = Color(0xFFF0F1F4),
    inversePrimary = Color(0xFFA3C9FF),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA3C9FF),
    onPrimary = Color(0xFF00315C),
    primaryContainer = Color(0xFF004A86),
    onPrimaryContainer = Color(0xFFD4E3FF),
    tertiary = Color(0xFFFF9A5C),
    background = Color(0xFF111418),
    onBackground = Color(0xFFE2E6EC),
    surfaceContainerLow = Color(0xFF1D2127),
    onSurface = Color(0xFFE2E6EC),
    onSurfaceVariant = Color(0xFFAEB6C2),
    inverseSurface = Color(0xFFE2E6EC),
    inverseOnSurface = Color(0xFF2E3135),
    inversePrimary = RabobankBlue,
)

/**
 * Created by Giorgos Fragkis on 12-11-2026
 */
@Composable
fun CsvParselyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
