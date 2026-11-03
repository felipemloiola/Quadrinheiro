package com.example

import com.example.ui.theme.*
import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test

class ReadableColorsTest {
    @Test fun allThemesRetainReadableTextAndFilledButtonLabels() {
        for (theme in AppTheme.entries) for (dark in listOf(false, true)) for (high in listOf(false, true)) {
            val colors = readableTheme(getIOS27ThemeColors(theme, dark), high)
            val label = "$theme dark=$dark contrast=$high"
            assertTrue(label, contrastRatio(colors.textPrimary, colors.bgSecondary) >= 6.99f)
            assertTrue(label, contrastRatio(colors.textSecondary, colors.bgSecondary) >= if (high) 6.99f else 5.49f)
            assertTrue(label, contrastRatio(colors.textTertiary, colors.bgSecondary) >= if (high) 6.99f else 4.49f)
            assertTrue(label, contrastRatio(colors.accent, colors.bgSecondary) >= 4.49f)
            assertTrue(label, contrastRatio(readableForeground(Color.White, colors.accent), colors.accent) >= 4.49f)
        }
    }
}
