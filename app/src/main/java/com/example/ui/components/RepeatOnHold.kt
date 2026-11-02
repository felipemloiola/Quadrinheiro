package com.example.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Observe release before clickable handles it, so no repeat survives into the next frame. */
@Composable
internal fun Modifier.repeatOnHold(onPress: () -> Unit, onRepeat: () -> Boolean): Modifier {
    val scope = rememberCoroutineScope()
    val press = rememberUpdatedState(onPress)
    val repeat = rememberUpdatedState(onRepeat)
    return pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(false, PointerEventPass.Initial)
            press.value()
            val job = scope.launch {
                delay(360)
                while (repeat.value()) delay(100)
            }
            try {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed || change.isConsumed ||
                        (change.position - down.position).getDistance() > viewConfiguration.touchSlop) break
                }
            } finally {
                job.cancel()
            }
        }
    }
}
