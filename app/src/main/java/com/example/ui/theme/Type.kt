package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * Apple San Francisco (SF Pro) Font Families:
 * - SF Pro Display: specifically engineered for text 20pt/sp and larger (titles, large headers).
 * - SF Pro Text: specifically engineered for text smaller than 20pt/sp (body, headlines, captions).
 * Ref: https://developer.apple.com/design/human-interface-guidelines/typography
 */
val SfProDisplayFontFamily = FontFamily(
    Font(R.font.sf_pro_display_regular, FontWeight.Normal),
    Font(R.font.sf_pro_display_medium, FontWeight.Medium),
    Font(R.font.sf_pro_display_semibold, FontWeight.SemiBold),
    Font(R.font.sf_pro_display_bold, FontWeight.Bold)
)

val SfProTextFontFamily = FontFamily(
    Font(R.font.sf_pro_text_regular, FontWeight.Normal),
    Font(R.font.sf_pro_text_medium, FontWeight.Medium),
    Font(R.font.sf_pro_text_semibold, FontWeight.SemiBold),
    Font(R.font.sf_pro_text_bold, FontWeight.Bold)
)

// Universal San Francisco font family
val SanFranciscoFontFamily = SfProTextFontFamily
val AppSansFamily = SanFranciscoFontFamily

// Compatibility aliases for existing screens; all resolve to San Francisco
val PlusJakartaSansFamily = SanFranciscoFontFamily
val SpaceGroteskTabularFamily = SanFranciscoFontFamily
val SfProFamily = SanFranciscoFontFamily
val SfDisplayFamily = SfProDisplayFontFamily
val SfTextFamily = SfProTextFontFamily

/**
 * Builds Material 3 Typography strictly adhering to Apple Human Interface Guidelines:
 * - Large Title: 34sp / 41sp, Bold, Display
 * - Title 1: 28sp / 34sp, Bold, Display
 * - Title 2: 22sp / 28sp, Bold, Display
 * - Title 3: 20sp / 25sp, SemiBold, Display
 * - Headline: 17sp / 22sp, SemiBold, Text
 * - Body: 17sp / 22sp, Regular, Text
 * - Callout: 16sp / 21sp, Medium, Text
 * - Subheadline: 15sp / 20sp, Regular, Text
 * - Footnote: 13sp / 18sp, Regular, Text
 * - Caption 1: 12sp / 16sp, Medium, Text
 * - Caption 2: 11sp / 13sp, Medium, Text
 */
fun buildQuadrinheiroTypography(scaleFactor: Float, forceBold: Boolean): Typography {
    val scale = if (scaleFactor.isFinite()) scaleFactor.coerceIn(0.5f, 3f) else 1f

    fun weight(base: FontWeight): FontWeight = if (forceBold) {
        if (base.weight < FontWeight.SemiBold.weight) FontWeight.SemiBold else FontWeight.Bold
    } else base

    fun style(
        size: Float,
        leading: Float,
        base: FontWeight = FontWeight.Normal,
        tracking: Float = 0f,
        family: FontFamily = if (size >= 20f) SfProDisplayFontFamily else SfProTextFontFamily
    ) = TextStyle(
        fontFamily = family,
        fontWeight = weight(base),
        fontSynthesis = FontSynthesis.None,
        fontSize = (size * scale).sp,
        lineHeight = (leading * scale).sp,
        letterSpacing = (tracking * scale).sp
    )

    return Typography(
        // Large Title (34 / 41)
        displayLarge = style(34f, 41f, FontWeight.Bold, tracking = 0.37f, family = SfProDisplayFontFamily),
        // Title 1 (28 / 34)
        displayMedium = style(28f, 34f, FontWeight.Bold, tracking = 0.36f, family = SfProDisplayFontFamily),
        // Title 2 (22 / 28)
        displaySmall = style(22f, 28f, FontWeight.Bold, tracking = 0.35f, family = SfProDisplayFontFamily),
        // Title 1 (28 / 34)
        headlineLarge = style(28f, 34f, FontWeight.Bold, tracking = 0.36f, family = SfProDisplayFontFamily),
        // Title 2 (22 / 28)
        headlineMedium = style(22f, 28f, FontWeight.SemiBold, tracking = 0.35f, family = SfProDisplayFontFamily),
        // Title 3 (20 / 25)
        headlineSmall = style(20f, 25f, FontWeight.SemiBold, tracking = 0.38f, family = SfProDisplayFontFamily),
        // Title 3 (20 / 25)
        titleLarge = style(20f, 25f, FontWeight.SemiBold, tracking = 0.38f, family = SfProDisplayFontFamily),
        // Headline (17 / 22)
        titleMedium = style(17f, 22f, FontWeight.SemiBold, tracking = -0.41f, family = SfProTextFontFamily),
        // Subheadline (15 / 20)
        titleSmall = style(15f, 20f, FontWeight.SemiBold, tracking = -0.24f, family = SfProTextFontFamily),
        // Body (17 / 22)
        bodyLarge = style(17f, 22f, FontWeight.Normal, tracking = -0.41f, family = SfProTextFontFamily),
        // Subheadline (15 / 20)
        bodyMedium = style(15f, 20f, FontWeight.Normal, tracking = -0.24f, family = SfProTextFontFamily),
        // Footnote (13 / 18)
        bodySmall = style(13f, 18f, FontWeight.Normal, tracking = -0.08f, family = SfProTextFontFamily),
        // Callout (16 / 21)
        labelLarge = style(16f, 21f, FontWeight.Medium, tracking = -0.32f, family = SfProTextFontFamily),
        // Caption 1 (12 / 16)
        labelMedium = style(12f, 16f, FontWeight.Medium, tracking = 0f, family = SfProTextFontFamily),
        // Caption 2 (11 / 13)
        labelSmall = style(11f, 13f, FontWeight.Medium, tracking = 0.07f, family = SfProTextFontFamily)
    )
}
