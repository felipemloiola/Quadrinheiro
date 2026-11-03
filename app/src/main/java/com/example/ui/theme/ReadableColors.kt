package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

fun contrastRatio(foreground: Color, background: Color): Float {
    val a = foreground.compositeOver(background).luminance()
    val b = background.luminance()
    return (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
}

fun readableForeground(color: Color, background: Color, minimum: Float = 4.5f): Color {
    if (contrastRatio(color, background) >= minimum) return color
    val end = if (contrastRatio(Color.White, background) > contrastRatio(Color.Black, background)) Color.White else Color.Black
    var low = 0f
    var high = 1f
    repeat(18) {
        val middle = (low + high) / 2f
        if (contrastRatio(lerp(color, end, middle), background) >= minimum) high = middle else low = middle
    }
    return lerp(color, end, high)
}

fun readableTheme(colors: IOS27ThemeColors, highContrast: Boolean = false): IOS27ThemeColors = colors.copy(
    textPrimary = readableForeground(colors.textPrimary, colors.bgSecondary, 7f),
    textSecondary = readableForeground(colors.textSecondary, colors.bgSecondary, if (highContrast) 7f else 5.5f),
    textTertiary = readableForeground(colors.textTertiary, colors.bgSecondary, if (highContrast) 7f else 4.5f),
    accent = readableForeground(colors.accent, colors.bgSecondary, 4.5f)
)
