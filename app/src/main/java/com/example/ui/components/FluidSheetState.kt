package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppMotion
import com.example.ui.theme.LocalAppMotion
import com.example.ui.theme.rememberReducedMotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

internal fun projectSheetEndpoint(position: Float, velocity: Float): Float =
    AppMotion.projectMomentum(position, velocity, decelerationRate = 0.99f)

internal fun sheetResistance(position: Float, dimension: Float): Float =
    if (position >= 0f) position else AppMotion.rubberband(position, dimension, constant = 0.55f)

@Stable
class FluidSheetState internal constructor(
    private val scope: CoroutineScope,
    private val height: Float,
    private val initialOffset: Float,
    private val dismissThreshold: Float,
    private val expandThreshold: Float,
    private val reduced: () -> Boolean,
    private val haptics: AppHapticsController?,
    private val onDismiss: () -> Unit,
    private val onExpand: () -> Unit
) {
    private val animation = Animatable(if (reduced()) initialOffset else height)
    private var held by mutableStateOf<Float?>(null)
    private var job: Job? = null
    private val tracker = VelocityTracker()
    private var rawPosition = initialOffset
    val offset: Float get() = held ?: animation.value
    val isMoving: Boolean get() = held != null || animation.isRunning
    val scrimFraction: Float get() = (1f - offset / height.coerceAtLeast(1f)).coerceIn(0f, 1f)

    fun dismiss() = settle(height, 0f, dismiss = true)
    fun enter() = settle(initialOffset, 0f, isEntry = true)
    fun expandToTop() = settle(0f, 0f)
    fun snapToRest() = settle(initialOffset, 0f)

    private fun settle(
        target: Float,
        velocity: Float,
        dismiss: Boolean = false,
        isEntry: Boolean = false
    ) {
        val current = offset
        val carryVelocity = if (abs(velocity) > 1f) velocity else animation.velocity
        val carriedMomentum = abs(carryVelocity) > 40f
        job?.cancel()
        job = scope.launch {
            animation.snapTo(current)
            held = null
            val motion = AppMotion(reduced = reduced())
            if (motion.reduced) {
                animation.snapTo(target)
            } else {
                val spec = when {
                    dismiss -> motion.ios26ModalExitSpring<Float>()
                    isEntry -> motion.ios26ModalSpring<Float>()
                    else -> motion.ios26DragReleaseSpring<Float>(carriedMomentum = carriedMomentum)
                }
                animation.animateTo(
                    targetValue = target,
                    animationSpec = spec,
                    initialVelocity = carryVelocity
                )
            }
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
            },
            onVerticalDrag = { change, amount ->
                change.consume()
                rawPosition += amount
                tracker.addPosition(change.uptimeMillis, Offset(0f, rawPosition))
                // 1:1 finger tracking; progressive rubber-band resistance past the top edge
                held = if (rawPosition < 0f) sheetResistance(rawPosition, height) else rawPosition
            },
            onDragEnd = {
                val velocity = tracker.calculateVelocity().y
                val endpoint = projectSheetEndpoint(rawPosition, velocity)
                when {
                    endpoint > initialOffset + dismissThreshold -> {
                        if (abs(velocity) > 650f) haptics?.impactMedium() else haptics?.impactLight()
                        settle(height, velocity, dismiss = true)
                    }
                    endpoint < initialOffset - expandThreshold -> {
                        haptics?.impactLight()
                        settle(0f, velocity)
                        onExpand()
                    }
                    else -> {
                        haptics?.impactLight()
                        settle(initialOffset, velocity)
                    }
                }
            },
            onDragCancel = { settle(initialOffset, 0f) }
        )
    }
}

@Composable
fun rememberFluidSheet(
    initialHeightFraction: Float = 0.82f,
    onDismiss: () -> Unit,
    onExpand: () -> Unit
): FluidSheetState {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptics = rememberAppHaptics()
    val totalHeight = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val initialOffset = totalHeight * (1f - initialHeightFraction)
    val dismissThreshold = with(density) { 95.dp.toPx() }
    val expandThreshold = with(density) { 55.dp.toPx() }
    val dismiss = rememberUpdatedState(onDismiss)
    val expand = rememberUpdatedState(onExpand)
    val reduced = rememberReducedMotion()
    val state = remember(scope, totalHeight, initialOffset, dismissThreshold, expandThreshold, haptics) {
        FluidSheetState(
            scope = scope,
            height = totalHeight,
            initialOffset = initialOffset,
            dismissThreshold = dismissThreshold,
            expandThreshold = expandThreshold,
            reduced = reduced,
            haptics = haptics,
            onDismiss = { dismiss.value() },
            onExpand = { expand.value() }
        )
    }
    val rootDepthHost = LocalRootDepthHost.current
    val depthId = remember { "fluid_sheet_${System.nanoTime()}" }
    LaunchedEffect(state, rootDepthHost) {
        if (rootDepthHost != null) {
            snapshotFlow { state.scrimFraction }.collect { fraction ->
                rootDepthHost.update(depthId, fraction * 0.85f)
            }
        }
    }
    DisposableEffect(rootDepthHost, depthId) {
        onDispose { rootDepthHost?.remove(depthId) }
    }
    LaunchedEffect(state) { state.enter() }
    BackHandler { state.dismiss() }
    return state
}
