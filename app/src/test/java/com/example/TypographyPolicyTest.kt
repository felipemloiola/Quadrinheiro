package com.example

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.AppSansFamily
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.SpaceGroteskTabularFamily
import com.example.ui.theme.buildQuadrinheiroTypography
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TypographyPolicyTest {
    @Test
    fun allTextRolesUseOneNativeFamily() {
        val t = buildQuadrinheiroTypography(1f, false)
        val roles = listOf(t.displayLarge, t.displayMedium, t.displaySmall,
            t.headlineLarge, t.headlineMedium, t.headlineSmall,
            t.titleLarge, t.titleMedium, t.titleSmall,
            t.bodyLarge, t.bodyMedium, t.bodySmall,
            t.labelLarge, t.labelMedium, t.labelSmall)
        assertEquals(FontFamily.SansSerif, AppSansFamily)
        assertEquals(AppSansFamily, PlusJakartaSansFamily)
        assertEquals(AppSansFamily, SpaceGroteskTabularFamily)
        roles.forEach { assertEquals(AppSansFamily, it.fontFamily) }
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
