package com.example.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Semantic Android haptic feedback controller following Apple WWDC *Designing Audio-Haptic Experiences*
 * and Emil Kowalski's mobile principles:
 * - Trigger on the exact causal frame (selection tick, detent catch, snap back, commit)
 * - Never per scroll/drag frame; always paired with visual feedback
 * - Respects system haptic settings automatically via `View.performHapticFeedback`
 */
@Immutable
class AppHapticsController internal constructor(
    private val view: View
) {
    /** Crisp tick when stepping across picker options, segmented controls, or menu rows. */
    fun selection() {
        performSafe(HapticFeedbackConstants.CLOCK_TICK)
    }

    /** Light impact when a sheet snaps home, catches a detent, or a Liquid Glass button commits. */
    fun impactLight() {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.GESTURE_END
        } else {
            HapticFeedbackConstants.KEYBOARD_TAP
        }
        performSafe(constant)
    }

    /** Medium impact for firm flick dismissals or destructive actions (remove favorite/comic). */
    fun impactMedium() {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            HapticFeedbackConstants.CONTEXT_CLICK
        } else {
            HapticFeedbackConstants.VIRTUAL_KEY
        }
        performSafe(constant)
    }

    /** Semantic success confirmation (saving profile, catalog entry, or goals). */
    fun success() {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.KEYBOARD_TAP
        }
        performSafe(constant)
    }

    /** Semantic warning / error feedback (validation failure). */
    fun error() {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.REJECT
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
        performSafe(constant)
    }

    private fun performSafe(constant: Int) {
        runCatching { view.performHapticFeedback(constant) }
    }
}

@Composable
fun rememberAppHaptics(): AppHapticsController {
    val view = LocalView.current
    return remember(view) { AppHapticsController(view) }
}
