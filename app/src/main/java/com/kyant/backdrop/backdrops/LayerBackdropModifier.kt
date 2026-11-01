package com.kyant.backdrop.backdrops

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
// RASTER_SNAPSHOT_FREEZE_V19: when freezeSnapshot = true (e.g. during extreme fling inertia),
// freezes the recorded decorative layer so it is not re-rasterized on every 120Hz frame.
fun Modifier.layerBackdrop(
    backdrop: LayerBackdrop,
    reuseRecordedContent: Boolean = false,
    freezeSnapshot: Boolean = false
): Modifier = this then LayerBackdropElement(backdrop, reuseRecordedContent, freezeSnapshot)

private class LayerBackdropElement(
    val backdrop: LayerBackdrop,
    val reuseRecordedContent: Boolean,
    val freezeSnapshot: Boolean
) : ModifierNodeElement<LayerBackdropNode>() {
    override fun create() = LayerBackdropNode(backdrop, reuseRecordedContent, freezeSnapshot)

    override fun update(node: LayerBackdropNode) {
        if (node.backdrop != backdrop) {
            node.backdrop.layerCoordinates = null
            node.backdrop = backdrop
            node.hasRecordedAtLeastOnce = false
        }
        val wasFrozen = node.freezeSnapshot
        node.reuseRecordedContent = reuseRecordedContent
        node.freezeSnapshot = freezeSnapshot
        if (wasFrozen && !freezeSnapshot) {
            node.invalidateDraw()
        } else if (!freezeSnapshot) {
            node.invalidateDraw()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "layerBackdrop"
        properties["backdrop"] = backdrop
        properties["reuseRecordedContent"] = reuseRecordedContent
        properties["freezeSnapshot"] = freezeSnapshot
    }

    override fun equals(other: Any?): Boolean = this === other ||
        (other is LayerBackdropElement && backdrop == other.backdrop &&
            reuseRecordedContent == other.reuseRecordedContent &&
            freezeSnapshot == other.freezeSnapshot)

    override fun hashCode(): Int {
        var result = backdrop.hashCode()
        result = 31 * result + reuseRecordedContent.hashCode()
        result = 31 * result + freezeSnapshot.hashCode()
        return result
    }
}

private class LayerBackdropNode(
    var backdrop: LayerBackdrop,
    var reuseRecordedContent: Boolean,
    var freezeSnapshot: Boolean
) : DrawModifierNode, GlobalPositionAwareModifierNode, Modifier.Node() {
    var hasRecordedAtLeastOnce: Boolean = false

    override fun ContentDrawScope.draw() {
        val reuse = reuseRecordedContent && backdrop.recordsUnmodifiedContent && !backdrop.isRecording
        if (freezeSnapshot && hasRecordedAtLeastOnce && reuse) {
            // Congela a camada decorativa (raster snapshot) em VRAM durante inércia extrema (fling),
            // desenhando diretamente a GraphicsLayer já gravada sem re-executar shaders ou árvore filha.
            drawLayer(backdrop.graphicsLayer)
            return
        }

        if (!reuse) drawContent()
        var recorded = false
        if (!(freezeSnapshot && hasRecordedAtLeastOnce)) {
            backdrop.isRecording = true
            try {
                recordLayer(backdrop.graphicsLayer) { backdrop.onDraw(this@draw) }
                recorded = true
                hasRecordedAtLeastOnce = true
            } catch (_: Throwable) {
                // Retain legacy capture-failure tolerance; never blank the visible content.
            } finally {
                backdrop.isRecording = false
            }
        } else {
            recorded = true
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
        hasRecordedAtLeastOnce = false
    }
}

