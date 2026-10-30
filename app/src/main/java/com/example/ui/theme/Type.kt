package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// APP_HIG_TEXT_STYLES_V12: Android adaptation, not Apple's proprietary SF font.
val AppSansFamily = FontFamily.SansSerif

// Compatibility aliases for existing screens; both now resolve to the same native family.
val PlusJakartaSansFamily = AppSansFamily
val SpaceGroteskTabularFamily = AppSansFamily

fun buildQuadrinheiroTypography(scaleFactor: Float, forceBold: Boolean): Typography {
    val scale = if (scaleFactor.isFinite()) scaleFactor.coerceIn(0.5f, 3f) else 1f
    fun weight(base: FontWeight): FontWeight = if (forceBold) {
        if (base.weight < FontWeight.SemiBold.weight) FontWeight.SemiBold else FontWeight.Bold
    } else base
    fun style(size: Float, leading: Float, base: FontWeight = FontWeight.Normal) = TextStyle(
        fontFamily = AppSansFamily,
        fontWeight = weight(base),
        fontSynthesis = FontSynthesis.None,
        fontSize = (size * scale).sp,
        lineHeight = (leading * scale).sp,
        letterSpacing = 0.sp
    )
    // Apple HIG hierarchy adapted to Android sp; not a pixel-identical iOS rendering.
    return Typography(
        displayLarge = style(34f, 41f, FontWeight.Bold),
        displayMedium = style(28f, 34f),
        displaySmall = style(22f, 28f),
        headlineLarge = style(28f, 34f),
        headlineMedium = style(22f, 28f),
        headlineSmall = style(20f, 25f),
        titleLarge = style(20f, 25f, FontWeight.SemiBold),
        titleMedium = style(17f, 22f, FontWeight.SemiBold),
        titleSmall = style(15f, 20f, FontWeight.SemiBold),
        bodyLarge = style(17f, 22f),
        bodyMedium = style(15f, 20f),
        bodySmall = style(13f, 18f),
        labelLarge = style(16f, 21f, FontWeight.Medium),
        labelMedium = style(12f, 16f, FontWeight.Medium),
        labelSmall = style(11f, 13f, FontWeight.Medium)
    )
}
