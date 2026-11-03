package com.kyant.backdrop.catalog.utils

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Kyant Backdrop spring profiles, with Android gesture cancellation and reduced motion. */
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
    val reduceMotion: () -> Boolean = { false },
    val horizontalSlop: Float? = null
) {
    private val valueAnimation = Animatable(initialValue, visibilityThreshold)
    private val velocityAnimation = Animatable(0f, 5f)
    private val pressAnimation = Animatable(0f, 0.001f)
    private val scaleXAnimation = Animatable(initialScale, 0.001f)
    private val scaleYAnimation = Animatable(initialScale, 0.001f)
    private var requestedValue by mutableFloatStateOf(initialValue)
    private var settleJob: Job? = null
    private var pressJob: Job? = null
    private var releaseJob: Job? = null
    private var velocityJob: Job? = null
    private val tracker = VelocityTracker()
    private var startTarget = initialValue
    private var pressed = false

    val value: Float get() = valueAnimation.value
    private val span: Float get() = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
    val progress: Float get() = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    // Accumulate gesture deltas independently of the visual spring's lag.
    val targetValue: Float get() = requestedValue
    val pressProgress: Float get() = if (reduceMotion()) 0f else pressAnimation.value
    val scaleX: Float get() = if (reduceMotion()) initialScale else scaleXAnimation.value
    val scaleY: Float get() = if (reduceMotion()) initialScale else scaleYAnimation.value
    val velocity: Float get() = if (reduceMotion()) 0f else velocityAnimation.value

    val modifier: Modifier = Modifier.pointerInput(this) {
        inspectDragGestures(
            horizontalSlop = horizontalSlop,
            onDragStart = { down ->
                startTarget = targetValue
                press()
                onDragStarted(down.position)
            },
            onDragEnd = { onDragStopped(); release() },
            onDragCancel = {
                if (onDragCancelled != null) onDragCancelled.invoke(this@DampedDragAnimation)
                else animateToValue(startTarget, 0f)
                release()
            }
        ) { _, amount -> onDrag(size, amount) }
    }

    fun prepareSnap(value: Float) = snapToValue(value)

    fun press() {
        releaseJob?.cancel()
        if (!pressed) {
            settleJob?.cancel()
            requestedValue = value
            tracker.resetTracking()
            animateVelocity(0f)
        }
        pressed = true
        animatePress(true)
    }

    private fun animatePress(expanded: Boolean) {
        pressJob?.cancel()
        pressJob = animationScope.launch {
            val progress = if (expanded) 1f else 0f
            val scale = if (expanded) pressedScale else initialScale
            if (reduceMotion()) {
                pressAnimation.snapTo(progress)
                scaleXAnimation.snapTo(scale)
                scaleYAnimation.snapTo(scale)
            } else {
                launch { pressAnimation.animateTo(progress, spring(1f, 1000f, 0.001f)) }
                launch { scaleXAnimation.animateTo(scale, spring(0.6f, 250f, 0.001f)) }
                launch { scaleYAnimation.animateTo(scale, spring(0.7f, 250f, 0.001f)) }
            }
        }
    }

    fun release() {
        pressed = false
        releaseJob?.cancel()
        releaseJob = animationScope.launch {
            if (!reduceMotion()) {
                withFrameNanos { }
                snapshotFlow { abs(value - targetValue) < maxOf(span * 0.025f, visibilityThreshold) }.first { it }
            }
            animatePress(false)
        }
    }

    fun updateValue(value: Float) = moveTo(value, valueAnimation.velocity)

    fun animateToValue(value: Float, initialVelocity: Float = valueAnimation.velocity) {
        if (!pressed) {
            releaseJob?.cancel()
            animatePress(true)
        }
        moveTo(value, initialVelocity)
        if (!pressed) release()
    }

    private fun moveTo(value: Float, initialVelocity: Float) {
        requestedValue = value.coerceIn(valueRange)
        settleJob?.cancel()
        settleJob = animationScope.launch {
            if (reduceMotion()) valueAnimation.snapTo(requestedValue)
            else valueAnimation.animateTo(
                requestedValue,
                spring(1f, 1000f, visibilityThreshold),
                initialVelocity = initialVelocity
            ) {
                tracker.addPosition(SystemClock.uptimeMillis(), Offset(this.value, 0f))
                animateVelocity(tracker.calculateVelocity().x / span)
            }
            animateVelocity(0f)
        }
    }

    private fun animateVelocity(value: Float) {
        velocityJob?.cancel()
        velocityJob = animationScope.launch {
            if (reduceMotion()) velocityAnimation.snapTo(0f)
            else velocityAnimation.animateTo(value, spring(0.5f, 300f, visibilityThreshold * 10f))
        }
    }

    fun snapToValue(value: Float) {
        requestedValue = value.coerceIn(valueRange)
        settleJob?.cancel()
        settleJob = animationScope.launch { valueAnimation.snapTo(requestedValue) }
    }

    fun snapToAndBounce(value: Float) = animateToValue(value, 0f)
}
