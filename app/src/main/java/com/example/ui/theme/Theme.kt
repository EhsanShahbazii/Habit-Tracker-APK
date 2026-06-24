package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun MyApplicationTheme(
    themeMode: String = "SYSTEM",
    accentColor: Color = NeonElectricLime,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode.uppercase()) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = accentColor,
            secondary = ToxicOrange,
            tertiary = CyberCyan,
            background = DeepOnyx,
            surface = CarbonGray,
            onPrimary = Color.Black,
            onSecondary = Color.White,
            onBackground = TextWhite,
            onSurface = TextWhite,
            outline = BorderGray
        )
    } else {
        lightColorScheme(
            primary = accentColor,
            secondary = ToxicOrange,
            tertiary = CyberCyan,
            background = Color(0xFFF4F4F6), // Premium light slate background
            surface = Color(0xFFFFFFFF),    // Pure white cards
            onPrimary = Color.Black,
            onSecondary = Color.White,
            onBackground = Color(0xFF141416), // Premium high contrast near-black text
            onSurface = Color(0xFF141416),
            outline = Color(0xFFE2E2E8)       // Elegant border gray
        )
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
