package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val StudioDarkColorScheme = darkColorScheme(
    primary = AsianPressCrimson,
    onPrimary = InkWhite,
    primaryContainer = AsianPressCrimsonContainer,
    onPrimaryContainer = AsianPressCrimsonBright,
    secondary = StudioGold,
    onSecondary = StudioObsidian,
    secondaryContainer = StudioGoldContainer,
    onSecondaryContainer = StudioGold,
    tertiary = SignalEmerald,
    onTertiary = StudioObsidian,
    tertiaryContainer = SignalEmeraldContainer,
    onTertiaryContainer = SignalEmerald,
    background = StudioObsidian,
    onBackground = InkWhite,
    surface = StudioControlSurface,
    onSurface = InkWhite,
    surfaceVariant = StudioCardElevated,
    onSurfaceVariant = InkMuted,
    surfaceContainerHigh = StudioCardElevated,
    surfaceContainerHighest = StudioCardHighest,
    outline = StudioBorder,
    error = AsianPressCrimsonBright,
    onError = StudioObsidian
)

private val StudioLightColorScheme = lightColorScheme(
    primary = PressCrimson,
    onPrimary = PressSurface,
    primaryContainer = PressCrimsonContainer,
    onPrimaryContainer = PressCrimson,
    secondary = PressGold,
    onSecondary = PressSurface,
    secondaryContainer = PressGoldContainer,
    onSecondaryContainer = PressGold,
    tertiary = SignalEmerald,
    onTertiary = PressSurface,
    background = PressPaper,
    onBackground = PressInk,
    surface = PressSurface,
    onSurface = PressInk,
    surfaceVariant = PressSurfaceVariant,
    onSurfaceVariant = PressInkMuted,
    outline = PressInkMuted.copy(alpha = 0.35f)
)

val StudioShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun AsianPressLiveTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) StudioDarkColorScheme else StudioLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = StudioTypography,
        shapes = StudioShapes,
        content = content
    )
}
