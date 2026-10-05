package com.gitpush.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val VoidBlack = Color(0xFF05070D)
val Charcoal = Color(0xFF0D1117)
val Panel = Color(0xFF111927)
val PanelLight = Color(0xFFF6F8FA)
val NeonCyan = Color(0xFF22D3EE)
val NeonViolet = Color(0xFFA78BFA)
val NeonGreen = Color(0xFF34D399)
val Warn = Color(0xFFFBBF24)
val Danger = Color(0xFFF87171)
val Muted = Color(0xFF8B949E)

private val DarkScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    secondary = NeonViolet,
    tertiary = NeonGreen,
    background = VoidBlack,
    surface = Charcoal,
    surfaceVariant = Panel,
    onBackground = Color(0xFFE6EDF3),
    onSurface = Color(0xFFE6EDF3),
    error = Danger
)
private val LightScheme = lightColorScheme(
    primary = Color(0xFF0891B2),
    onPrimary = Color.White,
    secondary = Color(0xFF7C3AED),
    tertiary = Color(0xFF059669),
    background = Color(0xFFF6F8FA),
    surface = Color.White,
    onBackground = Color(0xFF0D1117),
    onSurface = Color(0xFF0D1117),
    error = Color(0xFFB91C1C)
)

@Composable
fun GitPushTheme(
    themeMode: String = "SYSTEM",
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = Typography(
            headlineLarge = androidx.compose.ui.text.TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
            headlineMedium = androidx.compose.ui.text.TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
            titleLarge = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
            bodyMedium = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
        ),
        content = content
    )
}

fun accentFor(name: String): Color = when (name) {
    "VIOLET" -> NeonViolet
    "GREEN" -> NeonGreen
    else -> NeonCyan
}
