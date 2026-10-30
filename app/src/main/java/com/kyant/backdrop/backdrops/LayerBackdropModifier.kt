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
