package com.example

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.example.ui.components.SdfShader
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device-only shader compilation check. Unlike source-contract tests, this calls
 * Android's real RuntimeShader compiler on an API supporting AGSL.
 */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class LiquidGlassRuntimeShaderDeviceTest {
    @Test fun agslCompilesInBothOpticalModes() {
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        try {
            for (chromatic in listOf(0f, 0.55f)) {
                val shader = RuntimeShader(SdfShader.AGSL_SDF_LIQUID_GLASS_SOURCE).apply {
                    setFloatUniform("size", 64f, 64f)
                    setFloatUniform("cornerRadii", 16f, 16f, 16f, 16f)
                    setFloatUniform("refractionHeight", 20f)
                    setFloatUniform("refractionAmount", 12f)
                    setFloatUniform("eccentricFactor", 0.16f)
                    setFloatUniform("chromaticAberration", chromatic)
                    setFloatUniform("lightAngle", -0.7853982f)
                    setFloatUniform("highlightStrength", 0.2f)
                    setInputShader("content", BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
                }
                assertNotNull(RenderEffect.createRuntimeShaderEffect(shader, "content"))
            }
        } finally {
            bitmap.recycle()
        }
    }
}
