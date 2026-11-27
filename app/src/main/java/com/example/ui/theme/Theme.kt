package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = MaatramBlue,
    onPrimary = Color.White,
    primaryContainer = MaatramBlueContainer,
    onPrimaryContainer = MaatramNavy,
    secondary = MaatramNavy,
    onSecondary = Color.White,
    secondaryContainer = MaatramSurfaceVariant,
    onSecondaryContainer = MaatramNavy,
    tertiary = MaatramBlueBright,
    onTertiary = Color.White,
    tertiaryContainer = MaatramBlueSoft,
    onTertiaryContainer = MaatramBlueDark,
    background = MaatramBackground,
    onBackground = MaatramNavy,
    surface = MaatramSurface,
    onSurface = MaatramNavy,
    surfaceVariant = MaatramSurfaceVariant,
    onSurfaceVariant = MaatramSlate,
    outline = MaatramOutline,
    error = MaatramRed,
    onError = Color.White,
    errorContainer = MaatramRedSoft,
    onErrorContainer = MaatramRed
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = MaatramNavy,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = MaatramBlueContainer,
    secondary = Color(0xFF93C5FD),
    onSecondary = MaatramNavy,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = Color(0xFF38BDF8),
    onTertiary = MaatramNavy,
    background = DarkBackground,
    onBackground = Color(0xFFF8FAFC),
    surface = DarkSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = DarkOutline,
    error = Color(0xFFF87171),
    onError = MaatramNavy
)

val MaatramShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun MaatramConnectTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = MaatramShapes,
        content = content
    )
}
