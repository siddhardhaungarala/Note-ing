package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ColorF39F5A,
    secondary = ColorF0C38E,
    background = SamsungDarkBg,
    surface = SamsungSlateDark,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color48426D,
    onSurfaceVariant = ColorE8BCB9
)

private val LightColorScheme = lightColorScheme(
    primary = Color312051,
    secondary = Color48426D,
    background = SamsungLightBg,
    surface = SamsungWhite,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1D1A39),
    onSurface = Color(0xFF1D1A39),
    surfaceVariant = ColorE8BCB9,
    onSurfaceVariant = Color48426D
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
