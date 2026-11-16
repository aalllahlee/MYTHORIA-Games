package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DontFallColorScheme = darkColorScheme(
    primary = ChampionGold,
    onPrimary = RoyalNavyDeep,
    primaryContainer = RoyalNavyLight,
    onPrimaryContainer = TextPrimary,
    secondary = SkyCyan,
    onSecondary = RoyalNavyDeep,
    secondaryContainer = SurfaceCardGlass,
    onSecondaryContainer = TextPrimary,
    tertiary = HeroCrimson,
    onTertiary = TextPrimary,
    background = RoyalNavyDeep,
    onBackground = TextPrimary,
    surface = SurfaceCardDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCardGlass,
    onSurfaceVariant = TextSecondary,
    outline = OutlineGold,
    error = DangerRed,
    onError = TextPrimary
)

val DontFallShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun DontFallTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DontFallColorScheme,
        typography = DontFallTypography,
        shapes = DontFallShapes,
        content = content
    )
}
