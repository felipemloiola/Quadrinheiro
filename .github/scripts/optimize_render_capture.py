from pathlib import Path
import hashlib

pending = {}
report = []
def once(text, old, new):
    if text.count(old) != 1:
        raise RuntimeError('Render capture anchor not unique; no source files written: ' + old[:100])
    return text.replace(old, new, 1)

def plan(path, marker, transform):
    p = Path(path)
    raw = p.read_bytes()
    text = raw.decode('utf-8').replace('\r\n', '\n')
    if marker in text:
        report.append('SKIP ' + path)
        return
    result = transform(text, raw)
    pending[p] = (result.replace('\n', '\r\n') if b'\r\n' in raw else result).encode('utf-8')
    report.append('OK ' + path)

def backdrop(text, raw):
    return once(text, '    internal var isRecording: Boolean = false', '''    // SINGLE_PASS_BACKDROP_SOURCE_V16: custom capture callbacks retain legacy semantics.
    internal val recordsUnmodifiedContent: Boolean get() = onDraw === DefaultOnDraw
    internal var isRecording: Boolean = false''')

MODIFIER = '''package com.kyant.backdrop.backdrops

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import com.kyant.backdrop.internal.recordLayer

// SINGLE_PASS_BACKDROP_MODIFIER_V16: explicit opt-in; legacy/custom callers unchanged.
fun Modifier.layerBackdrop(
    backdrop: LayerBackdrop,
    reuseRecordedContent: Boolean = false
): Modifier = this then LayerBackdropElement(backdrop, reuseRecordedContent)

private class LayerBackdropElement(
    val backdrop: LayerBackdrop,
    val reuseRecordedContent: Boolean
) : ModifierNodeElement<LayerBackdropNode>() {
    override fun create() = LayerBackdropNode(backdrop, reuseRecordedContent)

    override fun update(node: LayerBackdropNode) {
        if (node.backdrop != backdrop) {
            node.backdrop.layerCoordinates = null
            node.backdrop = backdrop
        }
        node.reuseRecordedContent = reuseRecordedContent
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "layerBackdrop"
        properties["backdrop"] = backdrop
        properties["reuseRecordedContent"] = reuseRecordedContent
    }

    override fun equals(other: Any?): Boolean = this === other ||
        (other is LayerBackdropElement && backdrop == other.backdrop &&
            reuseRecordedContent == other.reuseRecordedContent)

    override fun hashCode(): Int = 31 * backdrop.hashCode() + reuseRecordedContent.hashCode()
}

private class LayerBackdropNode(
    var backdrop: LayerBackdrop,
    var reuseRecordedContent: Boolean
) : DrawModifierNode, GlobalPositionAwareModifierNode, Modifier.Node() {
    override fun ContentDrawScope.draw() {
        val reuse = reuseRecordedContent && backdrop.recordsUnmodifiedContent && !backdrop.isRecording
        if (!reuse) drawContent()
        var recorded = false
        backdrop.isRecording = true
        try {
            recordLayer(backdrop.graphicsLayer) { backdrop.onDraw(this@draw) }
            recorded = true
        } catch (_: Throwable) {
            // Retain legacy capture-failure tolerance; never blank the visible content.
        } finally {
            backdrop.isRecording = false
        }
        if (reuse) {
            if (recorded) drawLayer(backdrop.graphicsLayer) else drawContent()
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) backdrop.layerCoordinates = coordinates
    }

    override fun onDetach() {
        backdrop.layerCoordinates = null
    }
}
'''
def modifier(text, raw):
    blob = hashlib.sha1(b'blob ' + str(len(raw)).encode() + b'\0' + raw).hexdigest()
    if blob != '7c7f38722a6414f8e1fbf0c5cb512aaa27d4cb63':
        raise RuntimeError('Backdrop modifier changed since review; refusing replacement: ' + blob)
    return MODIFIER

def main(text, raw):
    for name in ('backgroundBackdrop', 'mainContentBackdrop'):
        if 'val ' + name + ' = rememberLayerBackdrop()' not in text:
            raise RuntimeError('Expected an unmodified-content backdrop: ' + name)
        text = once(text, '.layerBackdrop(' + name + ')', '.layerBackdrop(' + name + ', reuseRecordedContent = true)')
    return once(text, '        val backgroundBackdrop = rememberLayerBackdrop()', '        // SINGLE_PASS_APP_BACKDROPS_V16: retain all rendering recipes; reuse recorded content.\n        val backgroundBackdrop = rememberLayerBackdrop()')

def modal(text, raw):
    if 'val sheetBackdrop = rememberLayerBackdrop()' not in text:
        raise RuntimeError('Modal capture callback changed; refusing opt-in')
    return once(text, '''                    .matchParentSize()
                    .layerBackdrop(sheetBackdrop)
            ) {
                LiquidGlassSheetRefractiveLayer(''', '''                    .matchParentSize()
                    // SINGLE_PASS_SHEET_BACKDROP_V16: only this background capture, not controls.
                    .layerBackdrop(sheetBackdrop, reuseRecordedContent = true)
            ) {
                LiquidGlassSheetRefractiveLayer(''')

plan('app/src/main/java/com/kyant/backdrop/backdrops/LayerBackdrop.kt', 'SINGLE_PASS_BACKDROP_SOURCE_V16', backdrop)
plan('app/src/main/java/com/kyant/backdrop/backdrops/LayerBackdropModifier.kt', 'SINGLE_PASS_BACKDROP_MODIFIER_V16', modifier)
plan('app/src/main/java/com/example/MainActivity.kt', 'SINGLE_PASS_APP_BACKDROPS_V16', main)
plan('app/src/main/java/com/example/ui/components/CatalogModal.kt', 'SINGLE_PASS_SHEET_BACKDROP_V16', modal)

TEST = '''package com.example

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
'''
test_path = Path('app/src/androidTest/java/com/example/RenderLayerEquivalenceTest.kt')
if test_path.exists() and test_path.read_text(encoding='utf-8') != TEST:
    raise RuntimeError('Render regression test file already differs; no source files written')
pending[test_path] = TEST.encode('utf-8')
for path, content in pending.items():
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(content)
Path('.audit').mkdir(exist_ok=True)
Path('.audit/render_capture_v16.txt').write_text('\n'.join(report) + '\nSingle-pass rendering is opt-in and restricted to three default-content sources. Custom callbacks, shaders, blur values, offscreen blending, touch handlers, animations and collection data are not removed. Emulator pixel/touch tests and unit tests must pass before commit. No S23 frame-rate measurement is claimed.\n', encoding='utf-8')
