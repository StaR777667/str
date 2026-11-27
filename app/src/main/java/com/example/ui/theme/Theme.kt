package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val PulseBoostDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = ObsidianVoid,
    primaryContainer = CyanContainer,
    onPrimaryContainer = CyanSoft,
    secondary = MintEmerald,
    onSecondary = ObsidianVoid,
    secondaryContainer = EmeraldContainer,
    onSecondaryContainer = MintEmerald,
    tertiary = SolarAmber,
    onTertiary = ObsidianVoid,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = SolarAmber,
    error = CoralCrimson,
    onError = TextPrimary,
    errorContainer = CrimsonContainer,
    onErrorContainer = CoralCrimson,
    background = ObsidianVoid,
    onBackground = TextPrimary,
    surface = DeepSpace,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedSlate,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianVoid,
    surfaceContainerLow = DeepSpace,
    surfaceContainer = ElevatedSlate,
    surfaceContainerHigh = SurfaceGlass,
    outline = OutlineSubtle,
    outlineVariant = ColorTokens.OutlineHairline
)

private object ColorTokens {
    val OutlineHairline = androidx.compose.ui.graphics.Color(0xFF1D2E54)
}

val PulseBoostShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun PulseBoostTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PulseBoostDarkColorScheme,
        typography = PulseBoostTypography,
        shapes = PulseBoostShapes,
        content = content
    )
}
