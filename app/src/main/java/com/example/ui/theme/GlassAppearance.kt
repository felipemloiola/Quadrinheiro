package com.example.ui.theme

import androidx.compose.ui.graphics.toArgb
import com.example.model.AppSettings

/** Visual parameters only; persisted preferences retain the user's original slider values. */
fun AppSettings.withGlassAppearance(): AppSettings = if (glassVariant != "frosted") this else copy(
    glassBlurIntensity = (glassBlurIntensity * 2.2f).coerceIn(1.6f, 3f),
    glassOpacity = (glassOpacity * 2.4f).coerceIn(1.8f, 3f),
    glassIntensity = glassIntensity * .65f,
    enableChromaticAberration = false
)

fun accentThemesByDescendingHue(): List<AppTheme> = listOf(AppTheme.BLACK, AppTheme.WHITE) +
    AppTheme.entries.filter { it != AppTheme.BLACK && it != AppTheme.WHITE }.sortedByDescending {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(it.swatchLight.toArgb(), hsv)
        if (hsv[0] >= 345f) 0f else hsv[0]
    }
