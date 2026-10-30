package com.example.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/** Observes unconsumed background taps; never consumes text-field, button or scrolling gestures.
 * Separate Dialog/Popup windows must apply this modifier to their own root too.
 */
fun Modifier.dismissFocusOnBackgroundTap(): Modifier = composed {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    pointerInput(focus, keyboard) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
            if (!down.isConsumed) {
                val up = waitForUpOrCancellation(pass = PointerEventPass.Final)
                if (up != null && !up.isConsumed) {
                    focus.clearFocus()
                    keyboard?.hide()
                }
            }
        }
    }
}
