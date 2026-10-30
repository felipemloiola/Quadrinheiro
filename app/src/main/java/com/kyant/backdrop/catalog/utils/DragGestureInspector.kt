package com.kyant.backdrop.catalog.utils

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope

// EXCLUSIVE_GLASS_DRAG_V8: preserve taps; reserve held/dragged movement before list scrolling.
suspend fun PointerInputScope.inspectDragGestures(
    onDragStart: (down: PointerInputChange) -> Unit = {},
    onDragEnd: (change: PointerInputChange) -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit
) {
    val slop = viewConfiguration.touchSlop
    val holdTimeout = viewConfiguration.longPressTimeoutMillis
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var ended = false
        var ownsMovement = false
        onDragStart(down)
        try {
            onDrag(down, Offset.Zero)
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) {
                    onDragEnd(change)
                    ended = true
                    break
                }
                if (change.isConsumed) break
                val delta = change.position - change.previousPosition
                val distance = (change.position - down.position).getDistance()
                val held = change.uptimeMillis - down.uptimeMillis >= holdTimeout
                ownsMovement = ownsMovement || distance > slop || held
                if (delta != Offset.Zero) {
                    if (ownsMovement) change.consume()
                    onDrag(change, delta)
                }
            }
        } finally {
            if (!ended) onDragCancel()
        }
    }
}
