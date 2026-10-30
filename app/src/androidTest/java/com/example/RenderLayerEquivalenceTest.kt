package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class RenderLayerEquivalenceTest {
    @get:Rule val compose = createComposeRule()

    @Composable
    private fun Scene(reuse: Boolean, nested: Boolean = false, custom: Boolean = false,
                      onClick: () -> Unit = {}) {
        val normal = rememberLayerBackdrop()
        val customized = rememberLayerBackdrop(onDraw = { drawContent(); drawRect(Color.Green) })
        val inner = rememberLayerBackdrop()
        val backdrop = if (custom) customized else normal
        Box(Modifier.size(96.dp).testTag("scene").layerBackdrop(backdrop, reuseRecordedContent = reuse)) {
            Box(Modifier.fillMaxSize().then(if (nested) Modifier.layerBackdrop(inner, reuseRecordedContent = reuse) else Modifier)) {
                Canvas(Modifier.fillMaxSize().testTag("touch").clickable(onClick = onClick)) {
                    drawRect(Color(0xFF112233))
                    drawRect(Color(0xFFDA2233), size = Size(size.width / 2f, size.height))
                    drawCircle(Color.White, radius = size.minDimension * 0.2f,
                        center = Offset(size.width * 0.75f, size.height * 0.5f))
                }
            }
        }
    }

    private fun assertSamePixels(before: ImageBitmap, after: ImageBitmap) {
        assertEquals(before.width, after.width)
        assertEquals(before.height, after.height)
        val left = before.toPixelMap()
        val right = after.toPixelMap()
        for (y in 0 until before.height) for (x in 0 until before.width) {
            val a = left[x, y].toArgb()
            val b = right[x, y].toArgb()
            for (shift in intArrayOf(0, 8, 16, 24)) {
                assertTrue("Pixel changed at $x,$y channel $shift",
                    abs(((a ushr shift) and 255) - ((b ushr shift) and 255)) <= 2)
            }
        }
    }

    private fun compareModes(nested: Boolean, custom: Boolean) {
        val reuse = mutableStateOf(false)
        compose.setContent { Scene(reuse.value, nested, custom) }
        compose.waitForIdle()
        val before = compose.onNodeWithTag("scene").captureToImage()
        compose.runOnIdle { reuse.value = true }
        compose.waitForIdle()
        assertSamePixels(before, compose.onNodeWithTag("scene").captureToImage())
    }

    @Test fun defaultCaptureMatchesVisibleContent() = compareModes(false, false)
    @Test fun nestedCapturesMatchVisibleContent() = compareModes(true, false)
    @Test fun customCaptureKeepsLegacyVisibleContent() = compareModes(false, true)

    @Test fun optimizedCapturePreservesTouchDelivery() {
        val clicks = AtomicInteger(0)
        compose.setContent { Scene(true, onClick = { clicks.incrementAndGet() }) }
        compose.onNodeWithTag("touch").performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, clicks.get()) }
    }
}
