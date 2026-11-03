package com.example.ui.theme

import androidx.compose.ui.graphics.toArgb
import com.example.model.AppSettings

/** Visual parameters only; persisted preferences retain the user's original slider values. */
fun AppSettings.withGlassAppearance(): AppSettings {
    val level = glassLevel.coerceIn(0, 100) / 100f
    // Calibrated around the existing 26% default; the full range changes real controls.
    val blur = glassBlurIntensity * (.48f + 2f * level)
    val opacity = glassOpacity * (.48f + 2f * level)
    return copy(
        glassBlurIntensity = (if (glassVariant == "frosted") blur * 2.2f else blur).coerceIn(.25f, 4f),
        glassOpacity = (if (glassVariant == "frosted") opacity * 2.4f else opacity).coerceIn(.2f, 4f),
        glassIntensity = glassIntensity * (1.13f - .5f * level) * if (glassVariant == "frosted") .65f else 1f,
        enableChromaticAberration = enableChromaticAberration && glassVariant != "frosted"
    )
}

fun accentThemesByDescendingHue(): List<AppTheme> = listOf(AppTheme.BLACK, AppTheme.WHITE) +
    AppTheme.entries.filter { it != AppTheme.BLACK && it != AppTheme.WHITE }.sortedByDescending {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(it.swatchLight.toArgb(), hsv)
        if (hsv[0] >= 345f) 0f else hsv[0]
    }
