package com.example

import com.example.ui.components.SdfShader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Source-contract guard; shader compilation and visual output still require device testing. */
class LiquidGlassOpticsContractTest {
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
}
