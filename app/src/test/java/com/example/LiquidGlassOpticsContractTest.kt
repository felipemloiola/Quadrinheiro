package com.example

import com.example.ui.components.LiquidGlassOptics
import com.example.ui.components.SdfShader
import com.example.ui.components.SegmentedControlGeometry
import com.kyant.backdrop.internal.RoundedRectRefractionShaderString
import com.kyant.backdrop.internal.RoundedRectRefractionWithDispersionShaderString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Source-contract guard; shader compilation and visual output still require device testing. */
class LiquidGlassOpticsContractTest {

    @Test
    fun testStandardizedSegmentedControlLupaOverhangAcrossAllTabCounts() {
        val density = 2f // 1.dp = 2px (same density as the 736x156 Day/Week/Month reference)
        val trackWidthPx = 320f * density
        val trackHeightPx = SegmentedControlGeometry.TRACK_HEIGHT_DP * density // 40.dp = 80px
        val insetPx = SegmentedControlGeometry.TRACK_HORIZONTAL_INSET_DP * density // 4.dp = 8px
        val trackExpandPx = SegmentedControlGeometry.TRACK_PRESS_EXPAND_DP * density // 2.dp = 4px
        val lupaExpandWidthPx = SegmentedControlGeometry.LUPA_PRESS_EXPAND_WIDTH_DP * density // 28.dp = 56px
        val lupaExpandHeightPx = SegmentedControlGeometry.LUPA_PRESS_EXPAND_HEIGHT_DP * density // 16.dp = 32px

        val trackScaleX = SegmentedControlGeometry.computeTrackScaleX(trackWidthPx, trackExpandPx, pressProgress = 1f)
        val trackScaleY = SegmentedControlGeometry.computeTrackScaleY(trackHeightPx, trackExpandPx, pressProgress = 1f)
        val trackHalfHorizontalGrowthPx = (trackWidthPx * (trackScaleX - 1f)) / 2f
        val trackHalfVerticalGrowthPx = (trackHeightPx * (trackScaleY - 1f)) / 2f

        // Track expands by exactly 1.dp (2px) per side and 1.dp (2px) top/bottom
        assertEquals(1f * density, trackHalfHorizontalGrowthPx, 0.01f)
        assertEquals(1f * density, trackHalfVerticalGrowthPx, 0.01f)

        // Verify 2-tab ("Descobrir | Comunidade"), 3-tab ("Day | Week | Month"), and 4-tab ("Todos | Lidos | Lendo | Fila")
        for (tabsCount in listOf(2, 3, 4)) {
            val indicatorWidthPx = (trackWidthPx - insetPx * 2f) / tabsCount
            val lupaScaleX = SegmentedControlGeometry.computeLupaScaleX(
                indicatorWidthPx = indicatorWidthPx,
                expandWidthPx = lupaExpandWidthPx,
                dampedScaleX = SegmentedControlGeometry.PRESSED_SCALE_TARGET,
                velocity = 0f
            )
            val lupaScaleY = SegmentedControlGeometry.computeLupaScaleY(
                indicatorHeightPx = trackHeightPx,
                expandHeightPx = lupaExpandHeightPx,
                dampedScaleY = SegmentedControlGeometry.PRESSED_SCALE_TARGET,
                velocity = 0f
            )

            val lupaHalfHorizontalGrowthPx = (indicatorWidthPx * (lupaScaleX - 1f)) / 2f
            val lupaHalfVerticalGrowthPx = (trackHeightPx * (lupaScaleY - 1f)) / 2f

            // Lupa always grows by +14.dp (28px) on each side and +8.dp (16px) on top/bottom
            assertEquals(14f * density, lupaHalfHorizontalGrowthPx, 0.01f)
            assertEquals(8f * density, lupaHalfVerticalGrowthPx, 0.01f)

            // Net overhang beyond the pressed Track outer border:
            // Horizontal: 14.dp (lupa growth) - 4.dp (track inset) - 1.dp (track growth) = 9.dp (18px)
            // Vertical: 8.dp (lupa growth) - 1.dp (track growth) = 7.dp (14px)
            val netOuterSideOverhangPx = lupaHalfHorizontalGrowthPx - insetPx - trackHalfHorizontalGrowthPx
            val netVerticalOverhangPx = lupaHalfVerticalGrowthPx - trackHalfVerticalGrowthPx
            assertEquals(9f * density, netOuterSideOverhangPx, 0.01f)
            assertEquals(7f * density, netVerticalOverhangPx, 0.01f)
        }
    }

    @Test
    fun testUnifiedRefractionHeightFormulaAndCeiling() {
        // Normal dimensions: 100 x 100 -> minDim = 100, ceiling = 40f
        val ceiling40 = LiquidGlassOptics.computeEffectiveRefractionHeightPx(
            requestedHeightPx = 60f,
            widthPx = 100f,
            heightPx = 100f,
            compactScale = 1.0f
        )
        assertEquals(40f, ceiling40, 0.001f)

        // Below ceiling: requested 20f -> returns 20f
        val normal20 = LiquidGlassOptics.computeEffectiveRefractionHeightPx(
            requestedHeightPx = 20f,
            widthPx = 100f,
            heightPx = 100f,
            compactScale = 1.0f
        )
        assertEquals(20f, normal20, 0.001f)

        // Compact scale applied once: requested 20f * 0.5 = 10f
        val scaled10 = LiquidGlassOptics.computeEffectiveRefractionHeightPx(
            requestedHeightPx = 20f,
            widthPx = 100f,
            heightPx = 100f,
            compactScale = 0.5f
        )
        assertEquals(10f, scaled10, 0.001f)

        // Diminutive dimensions: 2 x 2 -> minDim = 2, ceiling = 0.8f.
        // Must never exceed geometric ceiling (0.8f) even with MIN_REFRACTION_PX = 1.0f!
        val tinyDim = LiquidGlassOptics.computeEffectiveRefractionHeightPx(
            requestedHeightPx = 10f,
            widthPx = 2f,
            heightPx = 2f,
            compactScale = 1.0f
        )
        assertEquals(0.8f, tinyDim, 0.001f)

        // Invalid / NaN / Infinite dimensions -> 0f
        assertEquals(0f, LiquidGlassOptics.computeEffectiveRefractionHeightPx(20f, Float.NaN, 100f), 0.001f)
        assertEquals(0f, LiquidGlassOptics.computeEffectiveRefractionHeightPx(20f, -10f, 100f), 0.001f)
        assertEquals(0f, LiquidGlassOptics.computeEffectiveRefractionHeightPx(20f, 0f, 100f), 0.001f)
    }

    @Test
    fun noChromaticAberrationPathSamplesTheBackgroundOnlyOnce() {
        val source = SdfShader.AGSL_SDF_LIQUID_GLASS_SOURCE
        val monochromePath = source.substringAfter("if (abs(chromaticAberration) < 0.001)")
            .substringBefore("} else {")
        assertEquals(1, Regex("""content\.eval\(""").findAll(monochromePath).count())
        assertTrue(monochromePath.contains("content.eval(posG)"))
    }

    @Test
    fun chromaticModeRetainsSeparateRgbSamplesAndDualRim() {
        val source = SdfShader.AGSL_SDF_LIQUID_GLASS_SOURCE
        val chromaticPath = source.substringAfter("} else {\n                float2 posR")
            .substringBefore("float2 lightDir")
        assertEquals(3, Regex("""content\.eval\(""").findAll(chromaticPath).count())
        assertTrue(source.contains("float fresnel = pow("))
        assertTrue(source.contains("oppositeAlignment"))
        assertTrue(source.contains("rimEdgeMask * highlightStrength"))
    }

    @Test
    fun shaderSourcesContainUnifiedDefensiveClamp() {
        // AGSL SdfShader
        assertTrue(SdfShader.AGSL_SDF_LIQUID_GLASS_SOURCE.contains("0.40"))
        assertTrue(SdfShader.AGSL_SDF_LIQUID_GLASS_SOURCE.contains("float maxRefraction = min(safeSize.x, safeSize.y) * 0.40;"))

        // Backdrop Shaders
        assertTrue(RoundedRectRefractionShaderString.contains("0.40"))
        assertTrue(RoundedRectRefractionShaderString.contains("float maxRefraction = min(size.x, size.y) * 0.40;"))
        assertTrue(RoundedRectRefractionWithDispersionShaderString.contains("0.40"))
        assertTrue(RoundedRectRefractionWithDispersionShaderString.contains("float maxRefraction = min(size.x, size.y) * 0.40;"))
    }
}
