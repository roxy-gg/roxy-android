package gg.roxy.shared.styles

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoxyColorsTest {
    @Test
    fun darkPaletteMatchesDesktopTokens() {
        assertPalette(
            actual = RoxyDarkColors,
            isDark = true,
            expected = listOf(
                0xFF0A0A0A,
                0xFF0F0F10,
                0xFF161618,
                0xFF1D1D20,
                0xFF232326,
                0xFF303035,
                0xFFEDEDED,
                0xFF9A9AA3,
                0xFF6A6A73,
                0xFF4D8DFF,
                0xFF6AA0FF,
                0xFF3FB950,
                0xFFD9A441,
                0xFFF0556A,
                0xFFFFFFFF,
                0xFF000000,
            ),
        )
    }

    @Test
    fun lightPaletteMatchesDesktopTokensIncludingPolarityPair() {
        assertPalette(
            actual = RoxyLightColors,
            isDark = false,
            expected = listOf(
                0xFFFFFFFF,
                0xFFF7F7F8,
                0xFFEFEFF1,
                0xFFFFFFFF,
                0xFFE2E2E5,
                0xFFC9C9CF,
                0xFF1A1A1C,
                0xFF5C5C66,
                0xFF8A8A94,
                0xFF2563EB,
                0xFF1D4ED8,
                0xFF177D3C,
                0xFF9A6700,
                0xFFC81E3D,
                0xFF18181B,
                0xFFFFFFFF,
            ),
        )
    }

    @Test
    fun derivedColorsMatchDesktopFallbacks() {
        assertEquals(0x0DFFFFFF, RoxyDarkColors.edge.toArgb())
        assertEquals(0x17FFFFFF, RoxyDarkColors.edgeStrong.toArgb())
        assertEquals(0x12FFFFFF, RoxyDarkColors.edgeLit.toArgb())
        assertEquals(0x1418181B, RoxyLightColors.edge.toArgb())
        assertEquals(0x2418181B, RoxyLightColors.edgeStrong.toArgb())
        assertEquals(Color.Transparent, RoxyLightColors.edgeLit)
        assertEquals(0x4D4D8DFF, RoxyDarkColors.selection.toArgb())
        assertEquals(0x4D2563EB, RoxyLightColors.selection.toArgb())
        assertEquals(0xFF414148.toInt(), RoxyDarkColors.scrollbarThumbHover.toArgb())
        assertEquals(0xFFB6B6BD.toInt(), RoxyLightColors.scrollbarThumbHover.toArgb())
    }

    @Test
    fun materialSchemesContainOnlyRoxyPaletteColors() {
        assertSchemeUsesOnlyPalette(roxyColorScheme(RoxyDarkColors), RoxyDarkColors)
        assertSchemeUsesOnlyPalette(roxyColorScheme(RoxyLightColors), RoxyLightColors)
    }

    private fun assertPalette(
        actual: RoxyColors,
        isDark: Boolean,
        expected: List<Long>,
    ) {
        if (isDark) assertTrue(actual.isDark) else assertFalse(actual.isDark)
        assertEquals(expected.map(::Color), paletteColors(actual))
    }

    private fun assertSchemeUsesOnlyPalette(
        scheme: ColorScheme,
        palette: RoxyColors,
    ) {
        val allowed = paletteColors(palette).toSet()
        val schemeColors = listOf(
            scheme.primary,
            scheme.onPrimary,
            scheme.primaryContainer,
            scheme.onPrimaryContainer,
            scheme.inversePrimary,
            scheme.secondary,
            scheme.onSecondary,
            scheme.secondaryContainer,
            scheme.onSecondaryContainer,
            scheme.tertiary,
            scheme.onTertiary,
            scheme.tertiaryContainer,
            scheme.onTertiaryContainer,
            scheme.background,
            scheme.onBackground,
            scheme.surface,
            scheme.onSurface,
            scheme.surfaceVariant,
            scheme.onSurfaceVariant,
            scheme.surfaceTint,
            scheme.inverseSurface,
            scheme.inverseOnSurface,
            scheme.error,
            scheme.onError,
            scheme.errorContainer,
            scheme.onErrorContainer,
            scheme.outline,
            scheme.outlineVariant,
            scheme.scrim,
            scheme.surfaceBright,
            scheme.surfaceDim,
            scheme.surfaceContainer,
            scheme.surfaceContainerHigh,
            scheme.surfaceContainerHighest,
            scheme.surfaceContainerLow,
            scheme.surfaceContainerLowest,
            scheme.primaryFixed,
            scheme.primaryFixedDim,
            scheme.onPrimaryFixed,
            scheme.onPrimaryFixedVariant,
            scheme.secondaryFixed,
            scheme.secondaryFixedDim,
            scheme.onSecondaryFixed,
            scheme.onSecondaryFixedVariant,
            scheme.tertiaryFixed,
            scheme.tertiaryFixedDim,
            scheme.onTertiaryFixed,
            scheme.onTertiaryFixedVariant,
        )
        assertTrue(schemeColors.all(allowed::contains))
        assertEquals(palette.black, scheme.scrim)
    }

    private fun paletteColors(colors: RoxyColors): List<Color> = listOf(
        colors.bg,
        colors.surface,
        colors.surface2,
        colors.elevated,
        colors.border,
        colors.borderStrong,
        colors.text,
        colors.textMuted,
        colors.textSubtle,
        colors.accent,
        colors.accentHover,
        colors.success,
        colors.warning,
        colors.danger,
        colors.white,
        colors.black,
    )
}
