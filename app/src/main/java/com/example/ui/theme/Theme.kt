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

private val StudyHubLightColorScheme = lightColorScheme(
    primary = ElectricPurple,
    onPrimary = Color.White,
    primaryContainer = LavenderSurface,
    onPrimaryContainer = RoyalViolet,
    secondary = BlueStart,
    onSecondary = Color.White,
    secondaryContainer = PastelBlueBg,
    onSecondaryContainer = BlueEnd,
    tertiary = GreenStart,
    onTertiary = Color.White,
    tertiaryContainer = PastelGreenBg,
    onTertiaryContainer = GreenEnd,
    background = SoftLavenderBg,
    onBackground = TextPrimaryLight,
    surface = Color.White,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFEDF0FA),
    onSurfaceVariant = TextSecondaryLight,
    outline = DividerLight,
    error = DangerRed,
    onError = Color.White
)

private val StudyHubDarkColorScheme = darkColorScheme(
    primary = ElectricPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2B1F66),
    onPrimaryContainer = Color(0xFFE5DCFF),
    secondary = CyanAccent,
    onSecondary = NavyDeep,
    secondaryContainer = Color(0xFF1A2A5E),
    onSecondaryContainer = Color(0xFFD7ECFF),
    tertiary = GreenStart,
    onTertiary = NavyDeep,
    tertiaryContainer = Color(0xFF11382A),
    onTertiaryContainer = Color(0xFFC5F9DD),
    background = NavyDeep,
    onBackground = TextPrimaryDark,
    surface = NavySurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = NavyCard,
    onSurfaceVariant = TextSecondaryDark,
    outline = NavyCardBorder,
    error = DangerRed,
    onError = Color.White
)

val StudyHubShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun StudyHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) StudyHubDarkColorScheme else StudyHubLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = StudyHubTypography,
        shapes = StudyHubShapes,
        content = content
    )
}
