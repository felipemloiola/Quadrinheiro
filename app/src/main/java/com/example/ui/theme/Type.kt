package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val PlusJakartaSansFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal)
)

val SpaceGroteskTabularFamily = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Normal)
)

fun buildQuadrinheiroTypography(scaleFactor: Float, forceBold: Boolean): Typography {
    fun weight(base: FontWeight): FontWeight = if (forceBold) {
        when {
            base.weight <= 400 -> FontWeight.Bold
            base.weight <= 500 -> FontWeight.ExtraBold
            else -> FontWeight.Black
        }
    } else base

    return Typography(
        displayLarge = TextStyle(
            fontFamily = SpaceGroteskTabularFamily,
            fontWeight = weight(FontWeight.Bold),
            fontSynthesis = FontSynthesis.All,
            fontSize = (34f * scaleFactor).sp,
            lineHeight = (40f * scaleFactor).sp,
            letterSpacing = (-0.5).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = weight(FontWeight.Bold),
            fontSynthesis = FontSynthesis.All,
            fontSize = (24f * scaleFactor).sp,
            lineHeight = (30f * scaleFactor).sp,
            letterSpacing = (-0.3).sp
        ),
        titleLarge = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = weight(FontWeight.Bold),
            fontSynthesis = FontSynthesis.All,
            fontSize = (19f * scaleFactor).sp,
            lineHeight = (25f * scaleFactor).sp
        ),
        titleMedium = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = weight(FontWeight.SemiBold),
            fontSynthesis = FontSynthesis.All,
            fontSize = (16f * scaleFactor).sp,
            lineHeight = (22f * scaleFactor).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = weight(FontWeight.Normal),
            fontSynthesis = FontSynthesis.All,
            fontSize = (15f * scaleFactor).sp,
            lineHeight = (22f * scaleFactor).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = weight(FontWeight.Normal),
            fontSynthesis = FontSynthesis.All,
            fontSize = (13.5f * scaleFactor).sp,
            lineHeight = (19f * scaleFactor).sp
        ),
        labelLarge = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = weight(FontWeight.SemiBold),
            fontSynthesis = FontSynthesis.All,
            fontSize = (13f * scaleFactor).sp,
            lineHeight = (18f * scaleFactor).sp
        ),
        labelSmall = TextStyle(
            fontFamily = SpaceGroteskTabularFamily,
            fontWeight = weight(FontWeight.Medium),
            fontSynthesis = FontSynthesis.All,
            fontSize = (11f * scaleFactor).sp,
            lineHeight = (15f * scaleFactor).sp
        )
    )
}
