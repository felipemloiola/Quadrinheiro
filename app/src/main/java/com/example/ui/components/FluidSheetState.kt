package com.example.ui.components

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.ui.theme.rememberReducedMotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlin.math.abs

internal fun projectSheetEndpoint(position: Float, velocity: Float): Float =
    position + (velocity / 1000f) * 0.99f / (1f - 0.99f)

internal fun sheetResistance(position: Float, dimension: Float): Float =
    if (position >= 0f) position else position * dimension * 0.55f / (dimension + 0.55f * abs(position))

@Stable
class FluidSheetState internal constructor(
    private val scope: CoroutineScope,
    private val height: Float,
    private val initialOffset: Float,
    private val dismissThreshold: Float,
    private val expandThreshold: Float,
    private val reduced: () -> Boolean,
    private val onDismiss: () -> Unit,
    private val onExpand: (() -> Unit)?,
    private val onDragDismissRequest: () -> Boolean
) {
    private val animation = Animatable(if (reduced()) initialOffset else height)
    private var held by mutableStateOf<Float?>(null)
    private var job: Job? = null
    private val tracker = VelocityTracker()
    private var rawPosition = initialOffset
    private var backStartOffset = initialOffset
    private var restOffset = initialOffset
    val offset: Float get() = held ?: animation.value
    val isMoving: Boolean get() = held != null || animation.isRunning
    val scrimFraction: Float get() = (1f - offset / height.coerceAtLeast(1f)).coerceIn(0f, 1f)

    fun dismiss() = settle(height, 0f, dismiss = true)
    fun enter() = settle(restOffset, 0f)
    fun expandToTop() {
        if (restOffset == 0f) return
        restOffset = 0f
        settle(restOffset, 0f)
    }
    fun snapToRest() {
        restOffset = initialOffset
        settle(restOffset, 0f)
    }

    fun beginBack() {
        backStartOffset = offset
        job?.cancel()
        held = backStartOffset
    }

    fun updateBack(progress: Float) {
        if (!reduced()) held = backStartOffset + (height - backStartOffset) * progress.coerceIn(0f, 1f)
    }

    fun cancelBack() = settle(backStartOffset, 0f)

    private fun settle(target: Float, velocity: Float, dismiss: Boolean = false) {
        val current = offset
        val carryVelocity = if (abs(velocity) > 1f) velocity else animation.velocity
        job?.cancel()
        job = scope.launch {
            animation.snapTo(current)
            held = null
            if (reduced()) animation.snapTo(target)
            else animation.animateTo(target, spring(if (abs(carryVelocity) > 1f) 0.85f else 1f, 480f), initialVelocity = carryVelocity)
            if (dismiss) onDismiss()
        }
    }

    private fun trackDrag(delta: Float, timeMillis: Long) {
        rawPosition += delta
        tracker.addPosition(timeMillis, Offset(0f, rawPosition))
        held = sheetResistance(rawPosition, height)
    }

    private fun finishDrag() {
        val velocity = tracker.calculateVelocity().y
        val endpoint = projectSheetEndpoint(rawPosition, velocity)
        when {
            endpoint > restOffset + dismissThreshold -> {
                if (onDragDismissRequest()) settle(height, velocity, dismiss = true)
                else settle(restOffset, 0f)
            }
            onExpand != null && restOffset > 0f && endpoint < restOffset - expandThreshold -> {
                restOffset = 0f
                settle(restOffset, velocity)
                onExpand.invoke()
            }
            else -> settle(restOffset, velocity)
        }
    }

    val dragModifier = Modifier.pointerInput(this) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var overSlop = 0f
            val start = if (animation.isRunning) {
                down.consume()
                down
            } else {
                awaitVerticalTouchSlopOrCancellation(down.id) { change, excess ->
                    overSlop = excess
                    change.consume()
                } ?: return@awaitEachGesture
            }
            rawPosition = offset
            job?.cancel()
            held = rawPosition
            tracker.resetTracking()
            tracker.addPosition(down.uptimeMillis, Offset(0f, rawPosition))
            if (overSlop != 0f) trackDrag(overSlop, start.uptimeMillis)
            val released = verticalDrag(start.id) { change ->
                trackDrag(change.positionChange().y, change.uptimeMillis)
                change.consume()
            }
            if (released) finishDrag() else settle(restOffset, 0f)
        }
    }
}

@Composable
fun rememberFluidSheet(
    initialHeightFraction: Float = 0.82f,
    onDismiss: () -> Unit,
    onExpand: (() -> Unit)? = null,
    backEnabled: Boolean = true,
    onDragDismissRequest: () -> Boolean = { true }
): FluidSheetState {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val totalHeight = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val initialOffset = totalHeight * (1f - initialHeightFraction)
    val dismissThreshold = with(density) { 95.dp.toPx() }
    val expandThreshold = with(density) { 55.dp.toPx() }
    val dismiss = rememberUpdatedState(onDismiss)
    val expand = rememberUpdatedState(onExpand)
    val dragDismiss = rememberUpdatedState(onDragDismissRequest)
    val reduced = rememberReducedMotion()
    val state = remember(scope, totalHeight, initialOffset, dismissThreshold, expandThreshold, onExpand != null) {
        FluidSheetState(scope, totalHeight, initialOffset, dismissThreshold, expandThreshold, reduced,
            { dismiss.value() }, if (onExpand != null) ({ expand.value?.invoke() }) else null, { dragDismiss.value() })
    }
    LaunchedEffect(state) { state.enter() }
    PredictiveBackHandler(enabled = backEnabled) {
        state.beginBack()
        try {
            it.collect { event -> state.updateBack(event.progress) }
            state.dismiss()
        } catch (cancelled: CancellationException) {
            state.cancelBack()
            throw cancelled
        }
    }
    return state
}
