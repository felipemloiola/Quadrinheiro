package com.example

import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.AppSansFamily
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.SanFranciscoFontFamily
import com.example.ui.theme.SfProDisplayFontFamily
import com.example.ui.theme.SfProTextFontFamily
import com.example.ui.theme.SpaceGroteskTabularFamily
import com.example.ui.theme.buildQuadrinheiroTypography
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TypographyPolicyTest {
    @Test
    fun allTextRolesUseAppleSanFranciscoFamiliesConformingToHIG() {
        val t = buildQuadrinheiroTypography(1f, false)

        assertEquals(SanFranciscoFontFamily, AppSansFamily)
        assertEquals(AppSansFamily, PlusJakartaSansFamily)
        assertEquals(AppSansFamily, SpaceGroteskTabularFamily)

        // Apple HIG: Display styles (>= 20sp) use SF Pro Display
        val displayRoles = listOf(
            t.displayLarge, t.displayMedium, t.displaySmall,
            t.headlineLarge, t.headlineMedium, t.headlineSmall,
            t.titleLarge
        )
        displayRoles.forEach {
            assertEquals(SfProDisplayFontFamily, it.fontFamily)
        }

        // Apple HIG: Text styles (< 20sp) use SF Pro Text
        val textRoles = listOf(
            t.titleMedium, t.titleSmall,
            t.bodyLarge, t.bodyMedium, t.bodySmall,
            t.labelLarge, t.labelMedium, t.labelSmall
        )
        textRoles.forEach {
            assertEquals(SfProTextFontFamily, it.fontFamily)
        }
    }

    @Test
    fun bodyHasReadableReferenceMetrics() {
        val body = buildQuadrinheiroTypography(1f, false).bodyLarge
        assertEquals(17f, body.fontSize.value, 0.001f)
        assertEquals(22f, body.lineHeight.value, 0.001f)
        assertEquals(FontWeight.Normal, body.fontWeight)
    }

    @Test
    fun boldPreferenceIncreasesWeightWithoutAddingAShadow() {
        val body = buildQuadrinheiroTypography(1f, true).bodyLarge
        assertTrue(body.fontWeight!!.weight >= FontWeight.SemiBold.weight)
        assertEquals(null, body.shadow)
    }

    @Test
    fun explicitStyleScaleIsAppliedOnce() {
        val body = buildQuadrinheiroTypography(1.15f, false).bodyLarge
        assertEquals(17f * 1.15f, body.fontSize.value, 0.001f)
        assertEquals(22f * 1.15f, body.lineHeight.value, 0.001f)
    }
}
