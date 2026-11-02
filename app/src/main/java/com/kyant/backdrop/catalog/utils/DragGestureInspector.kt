package com.kyant.backdrop.catalog.utils

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs

/** Highlights observe input; controls claim horizontal drags only after touch slop. */
suspend fun PointerInputScope.inspectDragGestures(
    onDragStart: (PointerInputChange) -> Unit = {},
    onDragEnd: (PointerInputChange) -> Unit = {},
    onDragCancel: () -> Unit = {},
    observeOnly: Boolean = false,
    onDrag: (PointerInputChange, Offset) -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(false, PointerEventPass.Initial)
        onDragStart(down)
        var claimed = false
        var completed = false
        var distance = Offset.Zero
        try {
            while (true) {
                val event = awaitPointerEvent(if (observeOnly) PointerEventPass.Initial else PointerEventPass.Main)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!observeOnly && change.isConsumed) break
                if (!change.pressed) {
                    val p = change.position
                    val padding = viewConfiguration.touchSlop
                    val inside = p.x in -padding..(size.width + padding) &&
                        p.y in -padding..(size.height + padding)
                    if (observeOnly || claimed || inside) {
                        if (!observeOnly) change.consume()
                        onDragEnd(change)
                        completed = true
                    }
                    break
                }
                val delta = change.positionChange()
                distance += delta
                if (observeOnly) onDrag(change, delta)
                else {
                    if (!claimed && abs(distance.y) > viewConfiguration.touchSlop && abs(distance.y) > abs(distance.x)) break
                    if (!claimed && abs(distance.x) > viewConfiguration.touchSlop) {
                        claimed = true
                        onDrag(change, distance)
                    } else if (claimed) onDrag(change, delta)
                    if (claimed) change.consume()
                }
            }
        } finally {
            if (!completed) onDragCancel()
        }
    }
}
