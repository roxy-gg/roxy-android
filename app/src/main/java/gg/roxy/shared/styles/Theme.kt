package gg.roxy.shared.styles

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

internal fun roxyColorScheme(colors: RoxyColors): ColorScheme = ColorScheme(
    primary = colors.accent,
    onPrimary = colors.black,
    primaryContainer = colors.elevated,
    onPrimaryContainer = colors.accentHover,
    inversePrimary = colors.accentHover,
    secondary = colors.textMuted,
    onSecondary = colors.bg,
    secondaryContainer = colors.surface2,
    onSecondaryContainer = colors.text,
    tertiary = colors.success,
    onTertiary = colors.black,
    tertiaryContainer = colors.surface2,
    onTertiaryContainer = colors.success,
    background = colors.bg,
    onBackground = colors.text,
    surface = colors.surface,
    onSurface = colors.text,
    surfaceVariant = colors.surface2,
    onSurfaceVariant = colors.textMuted,
    surfaceTint = colors.accent,
    inverseSurface = colors.white,
    inverseOnSurface = colors.black,
    error = colors.danger,
    onError = colors.black,
    errorContainer = colors.surface2,
    onErrorContainer = colors.danger,
    outline = colors.borderStrong,
    outlineVariant = colors.border,
    scrim = colors.black,
    surfaceBright = colors.elevated,
    surfaceDim = colors.bg,
    surfaceContainer = colors.surface,
    surfaceContainerHigh = colors.surface2,
    surfaceContainerHighest = colors.elevated,
    surfaceContainerLow = colors.surface,
    surfaceContainerLowest = colors.bg,
    primaryFixed = colors.accent,
    primaryFixedDim = colors.accentHover,
    onPrimaryFixed = colors.black,
    onPrimaryFixedVariant = colors.bg,
    secondaryFixed = colors.textMuted,
    secondaryFixedDim = colors.textSubtle,
    onSecondaryFixed = colors.bg,
    onSecondaryFixedVariant = colors.surface,
    tertiaryFixed = colors.success,
    tertiaryFixedDim = colors.success,
    onTertiaryFixed = colors.black,
    onTertiaryFixedVariant = colors.bg,
)

private val DarkColorScheme = roxyColorScheme(RoxyDarkColors)
private val LightColorScheme = roxyColorScheme(RoxyLightColors)

private val RoxyShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp),
)

private val LocalRoxyColors = staticCompositionLocalOf { RoxyDarkColors }

val MaterialTheme.roxyColors: RoxyColors
    @Composable
    @ReadOnlyComposable
    get() = LocalRoxyColors.current

@Composable
fun RoxyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) RoxyDarkColors else RoxyLightColors
    val materialColors = if (darkTheme) DarkColorScheme else LightColorScheme
    val selectionColors = TextSelectionColors(
        handleColor = colors.accent,
        backgroundColor = colors.selection,
    )

    CompositionLocalProvider(
        LocalRoxyColors provides colors,
        LocalTextSelectionColors provides selectionColors,
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = RoxyTypography,
            shapes = RoxyShapes,
            content = content,
        )
    }
}
