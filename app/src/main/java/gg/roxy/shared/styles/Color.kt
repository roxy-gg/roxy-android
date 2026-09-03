package gg.roxy.shared.styles

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class RoxyColors(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val elevated: Color,
    val border: Color,
    val borderStrong: Color,
    val text: Color,
    val textMuted: Color,
    val textSubtle: Color,
    val accent: Color,
    val accentHover: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val white: Color,
    val black: Color,
) {
    val edge: Color
        get() = white.copy(alpha = if (isDark) 0.05f else 0.08f)

    val edgeStrong: Color
        get() = white.copy(alpha = if (isDark) 0.09f else 0.14f)

    val edgeLit: Color
        get() = if (isDark) white.copy(alpha = 0.07f) else Color.Transparent

    val selection: Color
        get() = accent.copy(alpha = 0.30f)

    val scrollbarThumb: Color
        get() = borderStrong

    val scrollbarThumbHover: Color
        get() = mixSrgb(textSubtle, borderStrong, 0.70f)
}

private fun mixSrgb(
    from: Color,
    to: Color,
    toFraction: Float,
): Color = Color(
    red = from.red * (1f - toFraction) + to.red * toFraction,
    green = from.green * (1f - toFraction) + to.green * toFraction,
    blue = from.blue * (1f - toFraction) + to.blue * toFraction,
    alpha = from.alpha * (1f - toFraction) + to.alpha * toFraction,
)

val RoxyDarkColors = RoxyColors(
    isDark = true,
    bg = Color(0xFF0A0A0A),
    surface = Color(0xFF0F0F10),
    surface2 = Color(0xFF161618),
    elevated = Color(0xFF1D1D20),
    border = Color(0xFF232326),
    borderStrong = Color(0xFF303035),
    text = Color(0xFFEDEDED),
    textMuted = Color(0xFF9A9AA3),
    textSubtle = Color(0xFF6A6A73),
    accent = Color(0xFF4D8DFF),
    accentHover = Color(0xFF6AA0FF),
    success = Color(0xFF3FB950),
    warning = Color(0xFFD9A441),
    danger = Color(0xFFF0556A),
    white = Color(0xFFFFFFFF),
    black = Color(0xFF000000),
)

val RoxyLightColors = RoxyColors(
    isDark = false,
    bg = Color(0xFFFFFFFF),
    surface = Color(0xFFF7F7F8),
    surface2 = Color(0xFFEFEFF1),
    elevated = Color(0xFFFFFFFF),
    border = Color(0xFFE2E2E5),
    borderStrong = Color(0xFFC9C9CF),
    text = Color(0xFF1A1A1C),
    textMuted = Color(0xFF5C5C66),
    textSubtle = Color(0xFF8A8A94),
    accent = Color(0xFF2563EB),
    accentHover = Color(0xFF1D4ED8),
    success = Color(0xFF177D3C),
    warning = Color(0xFF9A6700),
    danger = Color(0xFFC81E3D),
    // These mirror the desktop polarity tokens, not literal color names.
    white = Color(0xFF18181B),
    black = Color(0xFFFFFFFF),
)
