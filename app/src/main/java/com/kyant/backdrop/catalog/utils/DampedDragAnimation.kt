package com.kyant.backdrop.catalog.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Position follows the finger directly; only settling uses an interruptible spring. */
class DampedDragAnimation(
    private val animationScope: CoroutineScope,
    val initialValue: Float,
    val valueRange: ClosedRange<Float>,
    val visibilityThreshold: Float,
    val initialScale: Float,
    val pressedScale: Float,
    val onDragStarted: DampedDragAnimation.(position: Offset) -> Unit,
    val onDragStopped: DampedDragAnimation.() -> Unit,
    val onDrag: DampedDragAnimation.(size: IntSize, dragAmount: Offset) -> Unit,
    val onDragCancelled: (DampedDragAnimation.() -> Unit)? = null,
    val reduceMotion: () -> Boolean = { false }
) {
    private val valueAnimation = Animatable(initialValue, visibilityThreshold)
    private val pressAnimation = Animatable(0f)
    private var heldValue by mutableStateOf<Float?>(null)
    private var settleJob: Job? = null
    private var pressJob: Job? = null
    private val tracker = VelocityTracker()
    private var startTarget = initialValue
    private var valuePerPixel = 0f
    private var releaseVelocity = 0f
    private var trackedPosition = Offset.Zero

    val value: Float get() = heldValue ?: valueAnimation.value
    val progress: Float get() {
        val span = valueRange.endInclusive - valueRange.start
        return if (span > 0f) ((value - valueRange.start) / span).coerceIn(0f, 1f) else 0f
    }
    val targetValue: Float get() = heldValue ?: valueAnimation.targetValue
    val pressProgress: Float get() = if (reduceMotion()) 0f else pressAnimation.value
    val scaleX: Float get() = if (reduceMotion()) initialScale else
        initialScale + (pressedScale - initialScale) * pressProgress
    val scaleY: Float get() = scaleX
    val velocity: Float get() = if (heldValue != null) releaseVelocity else valueAnimation.velocity

    val modifier: Modifier = Modifier.pointerInput(this) {
        inspectDragGestures(
            onDragStart = { down ->
                startTarget = targetValue
                val current = value
                settleJob?.cancel()
                heldValue = current
                tracker.resetTracking()
                trackedPosition = Offset.Zero
                tracker.addPosition(down.uptimeMillis, trackedPosition)
                releaseVelocity = 0f
                valuePerPixel = 0f
                onDragStarted(down.position)
                press()
            },
            onDragEnd = { change ->
                tracker.addPosition(change.uptimeMillis, trackedPosition)
                releaseVelocity = tracker.calculateVelocity().x * valuePerPixel
                onDragStopped()
                release()
            },
            onDragCancel = {
                releaseVelocity = 0f
                if (onDragCancelled != null) onDragCancelled.invoke(this@DampedDragAnimation)
                else animateToValue(startTarget, 0f)
                release()
            }
        ) { change, amount ->
            trackedPosition += amount
            tracker.addPosition(change.uptimeMillis, trackedPosition)
            val before = value
            onDrag(size, amount)
            if (abs(amount.x) > 0.01f) valuePerPixel = (value - before) / amount.x
        }
    }

    fun prepareSnap(value: Float) = snapToValue(value)

    fun press() {
        pressJob?.cancel()
        pressJob = animationScope.launch {
            if (reduceMotion()) pressAnimation.snapTo(1f)
            else pressAnimation.animateTo(1f, spring(1f, 1000f))
        }
    }

    fun release() {
        pressJob?.cancel()
        pressJob = animationScope.launch {
            if (reduceMotion()) pressAnimation.snapTo(0f)
            else pressAnimation.animateTo(0f, spring(1f, 1000f))
        }
    }

    fun updateValue(value: Float) {
        settleJob?.cancel()
        heldValue = value.coerceIn(valueRange)
    }

    fun animateToValue(value: Float, initialVelocity: Float = releaseVelocity) {
        val current = this.value
        val wasHeld = heldValue != null
        val inheritedVelocity = if (wasHeld) initialVelocity else valueAnimation.velocity
        settleJob?.cancel()
        settleJob = animationScope.launch {
            if (wasHeld) valueAnimation.snapTo(current)
            heldValue = null
            if (reduceMotion()) valueAnimation.snapTo(value.coerceIn(valueRange))
            else valueAnimation.animateTo(
                value.coerceIn(valueRange),
                spring(if (abs(inheritedVelocity) > 0.01f) 0.8f else 1f, 700f, visibilityThreshold),
                initialVelocity = inheritedVelocity
            )
        }
    }

    fun snapToValue(value: Float) {
        updateValue(value)
        val next = this.value
        settleJob = animationScope.launch {
            valueAnimation.snapTo(next)
            heldValue = null
        }
    }

    fun snapToAndBounce(value: Float) = animateToValue(value, 0f)
}
