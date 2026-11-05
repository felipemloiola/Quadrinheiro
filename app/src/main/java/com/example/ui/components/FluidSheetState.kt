package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.ui.theme.rememberReducedMotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

internal fun projectSheetEndpoint(position: Float, velocity: Float): Float =
    position + (velocity / 1000f) * 0.99f / (1f - 0.99f)

internal fun sheetResistance(position: Float, dimension: Float): Float =
    if (position >= 0f) position else position * dimension * 0.55f / (dimension + 0.55f * abs(position))

@Stable
class FluidSheetState internal constructor(
    private val scope: CoroutineScope,
    private val height: Float,
    private val dismissThreshold: Float,
    private val expandThreshold: Float,
    private val reduced: () -> Boolean,
    private val onDismiss: () -> Unit,
    private val onExpand: () -> Unit
) {
    private val animation = Animatable(if (reduced()) 0f else height)
    private var held by mutableStateOf<Float?>(null)
    private var job: Job? = null
    private val tracker = VelocityTracker()
    private var rawPosition = 0f
    val offset: Float get() = held ?: animation.value
    val scrimFraction: Float get() = (1f - offset / height.coerceAtLeast(1f)).coerceIn(0f, 1f)

    fun dismiss() = settle(height, 0f, dismiss = true)
    fun enter() = settle(0f, 0f)

    private fun settle(target: Float, velocity: Float, dismiss: Boolean = false) {
        val current = offset
        val carryVelocity = if (abs(velocity) > 1f) velocity else animation.velocity
        job?.cancel()
        job = scope.launch {
            animation.snapTo(current)
            held = null
            if (reduced()) animation.snapTo(target)
            else animation.animateTo(target, spring(if (abs(carryVelocity) > 1f) 0.8f else 1f, 500f), initialVelocity = carryVelocity)
            if (dismiss) onDismiss()
        }
    }

    val dragModifier = Modifier.pointerInput(this) {
        detectVerticalDragGestures(
            onDragStart = {
                rawPosition = offset
                job?.cancel()
                held = rawPosition
                tracker.resetTracking()
                // Event timestamps are added on movement; never use wall-clock time.
            },
            onVerticalDrag = { change, amount ->
                change.consume()
                rawPosition += amount
                tracker.addPosition(change.uptimeMillis, Offset(0f, rawPosition))
                held = sheetResistance(rawPosition, height)
            },
            onDragEnd = {
                val velocity = tracker.calculateVelocity().y
                val endpoint = projectSheetEndpoint(rawPosition, velocity)
                when {
                    endpoint > dismissThreshold -> settle(height, velocity, dismiss = true)
                    endpoint < -expandThreshold -> { onExpand(); settle(0f, velocity) }
                    else -> settle(0f, velocity)
                }
            },
            onDragCancel = { settle(0f, 0f) }
        )
    }
}

@Composable
fun rememberFluidSheet(onDismiss: () -> Unit, onExpand: () -> Unit): FluidSheetState {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val height = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val dismissThreshold = with(density) { 95.dp.toPx() }
    val expandThreshold = with(density) { 55.dp.toPx() }
    val dismiss = rememberUpdatedState(onDismiss)
    val expand = rememberUpdatedState(onExpand)
    val reduced = rememberReducedMotion()
    val state = remember(scope, height, dismissThreshold, expandThreshold) {
        FluidSheetState(scope, height, dismissThreshold, expandThreshold, reduced, { dismiss.value() }, { expand.value() })
    }
    LaunchedEffect(state) { state.enter() }
    BackHandler { state.dismiss() }
    return state
}
