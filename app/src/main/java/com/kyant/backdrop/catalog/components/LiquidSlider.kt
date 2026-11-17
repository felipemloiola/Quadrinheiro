package com.kyant.backdrop.catalog.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.lerp
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.LocalAppSettings
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.catalog.utils.DampedDragAnimation
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest

/**
 * Global reactive lock indicating that a slider is currently being touched/dragged.
 * Parent LazyColumns observe `!SliderTouchLock.isAnySliderActive` via `userScrollEnabled`
 * in addition to full pointer consumption so the screen never scrolls vertically while using a slider.
 */
object SliderTouchLock {
    var isAnySliderActive by mutableStateOf(false)
}

@Composable
fun LiquidSlider(
    value: () -> Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    visibilityThreshold: Float,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    isLightTheme: Boolean = !LocalIsDarkTheme.current,
    accentColor: Color = LocalIOS27Theme.current.accent,
    commitOnRelease: Boolean = false,
    valueStep: Float? = null
) {
    val trackColor =
        if (isLightTheme) Color(0xFF787880).copy(alpha = 0.24f)
        else Color(0xFF787880).copy(alpha = 0.44f)

    val trackBackdrop = rememberLayerBackdrop()
    val reduceTransparency = LocalAppSettings.current.reduceTransparency
    val density = LocalDensity.current
    val view = LocalView.current

    val thumbWidthDp = 40.dp
    val thumbHeightDp = 24.dp
    val trackHeightDp = 10.dp
    val trackHorizontalInsetDp = 10.dp

    var totalWidthPx by remember { mutableFloatStateOf(with(density) { 260.dp.toPx() }) }

    Box(
        modifier
            .fillMaxWidth()
            .height(36.dp)
            .onSizeChanged { sz ->
                if (sz.width > 0) {
                    totalWidthPx = sz.width.toFloat()
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val thumbWidthPx = with(density) { thumbWidthDp.toPx() }
        val trackInsetPx = with(density) { trackHorizontalInsetDp.toPx() }
        val trackWidthPx = (totalWidthPx - trackInsetPx * 2f).coerceAtLeast(thumbWidthPx + 1f)
        val usableTrackWidth = (trackWidthPx - thumbWidthPx).coerceAtLeast(1f)

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        val reduceMotion = com.example.ui.theme.rememberReducedMotion()
        var isDraggingNow by remember { mutableStateOf(false) }

        val rangeSpan = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
        val effectiveThreshold = (rangeSpan * 0.0005f).coerceAtMost(visibilityThreshold).coerceAtLeast(0.0001f)

        val dampedDragAnimation = remember(animationScope, valueRange.start, valueRange.endInclusive) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = value().fastCoerceIn(valueRange.start, valueRange.endInclusive),
                valueRange = valueRange,
                visibilityThreshold = effectiveThreshold,
                initialScale = 1f,
                pressedScale = 1.45f,
                onDragStarted = {},
                onDragStopped = {},
                onDrag = { _, _ -> },
                reduceMotion = reduceMotion,
                directTracking = true
            )
        }

        val currentValueFn by rememberUpdatedState(value)
        val currentOnValueChange by rememberUpdatedState(onValueChange)
        val deferChanges by rememberUpdatedState(commitOnRelease)
        val currentStep by rememberUpdatedState(valueStep)
        val currentValueRange by rememberUpdatedState(valueRange)
        val currentUsableTrackWidth by rememberUpdatedState(usableTrackWidth)
        val currentTrackInsetPx by rememberUpdatedState(trackInsetPx)
        val currentThumbWidthPx by rememberUpdatedState(thumbWidthPx)
        val currentIsLtr by rememberUpdatedState(isLtr)
        val currentDampedAnim by rememberUpdatedState(dampedDragAnimation)

        fun commit(requested: Float): Float {
            val range = currentValueRange
            val step = currentStep
            val bounded = requested.coerceIn(range)
            val next = if (step != null && step > 0f && bounded > range.start && bounded < range.endInclusive) {
                (range.start + ((bounded - range.start) / step).roundToInt() * step).coerceIn(range)
            } else bounded
            currentOnValueChange(next)
            return next
        }

        LaunchedEffect(Unit) {
            snapshotFlow { currentValueFn() }.collectLatest { externalValue ->
                val rng = currentValueRange
                if (!isDraggingNow && abs(currentDampedAnim.targetValue - externalValue) > effectiveThreshold) {
                    currentDampedAnim.updateValue(externalValue.fastCoerceIn(rng.start, rng.endInclusive))
                }
            }
        }

        // Camada da trilha gravada em largura total para que a refração da lente
        // enxergue perfeitamente as pontas arredondadas da barra dentro do vidro em 0% e 100%.
        val isScrollInProgress = com.example.ui.components.LocalLiquidGlassExtremeFling.current
        Box(
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .then(if (reduceTransparency) Modifier else Modifier.layerBackdrop(
                    backdrop = trackBackdrop,
                    freezeSnapshot = isScrollInProgress && !isDraggingNow && dampedDragAnimation.pressProgress <= 0.005f
                ))
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    val step = valueStep ?: rangeSpan / 20f
                    val direction = if (isLtr) 1f else -1f
                    val current = dampedDragAnimation.targetValue
                    val next = when (event.key) {
                        Key.DirectionLeft -> current - step * direction
                        Key.DirectionRight -> current + step * direction
                        Key.MoveHome -> valueRange.start
                        Key.MoveEnd -> valueRange.endInclusive
                        else -> return@onKeyEvent false
                    }
                    dampedDragAnimation.updateValue(commit(next))
                    true
                }
                .focusable()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val anim = currentDampedAnim
                        val range = currentValueRange
                        val usableWidth = currentUsableTrackWidth
                        val trackInset = currentTrackInsetPx
                        val thumbWidth = currentThumbWidthPx
                        val ltr = currentIsLtr
                        val direction = if (ltr) 1f else -1f
                        val touchSlop = viewConfiguration.touchSlop

                        fun valueAt(x: Float): Float {
                            val progress = ((x - trackInset - thumbWidth / 2) / usableWidth).coerceIn(0f, 1f)
                            return lerp(range.start, range.endInclusive, if (ltr) progress else 1f - progress)
                        }

                        val initialValue = anim.value.coerceIn(range)
                        val visualProgress = if (ltr) anim.progress else 1f - anim.progress
                        val thumbLeft = trackInset + usableWidth * visualProgress
                        val thumbSlop = thumbWidth * 0.35f
                        val onThumb = down.position.x in (thumbLeft - thumbSlop)..(thumbLeft + thumbWidth + thumbSlop)
                        var horizontalDrag = false
                        var released = false

                        try {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.fastFirstOrNull { it.id == down.id } ?: break
                                if (change.isConsumed || event.changes.any { it.id != down.id && it.pressed }) break

                                val delta = change.position - down.position
                                if (!change.pressed) {
                                    released = true
                                    if (!horizontalDrag && !onThumb && delta.getDistance() <= touchSlop &&
                                        change.position.x in 0f..size.width.toFloat() &&
                                        change.position.y in 0f..size.height.toFloat()
                                    ) {
                                        anim.updateValue(commit(valueAt(change.position.x)))
                                    }
                                    if (horizontalDrag) change.consume()
                                    break
                                }

                                if (!horizontalDrag) {
                                    if (abs(delta.y) > touchSlop && abs(delta.y) >= abs(delta.x)) break
                                    if (abs(delta.x) <= touchSlop || abs(delta.x) <= abs(delta.y)) continue
                                    horizontalDrag = true
                                    isDraggingNow = true
                                    SliderTouchLock.isAnySliderActive = true
                                    view.parent?.requestDisallowInterceptTouchEvent(true)
                                    anim.press()
                                }

                                val next = if (onThumb) {
                                    (initialValue + (range.endInclusive - range.start) * delta.x / usableWidth * direction)
                                        .coerceIn(range)
                                } else valueAt(change.position.x)
                                anim.updateValue(next)
                                if (!deferChanges) currentOnValueChange(next)
                                change.consume()
                            }
                        } finally {
                            if (horizontalDrag) {
                                val target = when {
                                    !deferChanges -> anim.targetValue
                                    released -> commit(anim.targetValue)
                                    else -> currentValueFn().coerceIn(range)
                                }
                                anim.animateToValue(target, initialVelocity = 0f)
                                anim.release()
                                isDraggingNow = false
                                SliderTouchLock.isAnySliderActive = false
                                view.parent?.requestDisallowInterceptTouchEvent(false)
                            }
                        }
                    }
                }
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = (if (isDraggingNow) dampedDragAnimation.targetValue else value())
                            .fastCoerceIn(valueRange.start, valueRange.endInclusive),
                        range = valueRange
                    )
                    setProgress { target ->
                        dampedDragAnimation.updateValue(commit(target))
                        true
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                Modifier
                    .padding(horizontal = trackHorizontalInsetDp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                // Trilha inativa (cinza com pontas em cápsula)
                Box(
                    Modifier
                        .clip(ContinuousCapsule)
                        .background(trackColor)
                        .height(trackHeightDp)
                        .fillMaxWidth()
                )

                // Barra ativa (azul / accentColor com ponta direita em cápsula arredondada)
                Box(
                    Modifier
                        .clip(ContinuousCapsule)
                        .background(accentColor)
                        .height(trackHeightDp)
                        .layout { measurable, constraints ->
                            val progress = dampedDragAnimation.progress.fastCoerceIn(0f, 1f)
                            val width = if (progress <= 0.002f) {
                                0
                            } else {
                                (constraints.maxWidth * progress).roundToInt().coerceIn(0, constraints.maxWidth)
                            }
                            val placeable = measurable.measure(
                                Constraints.fixed(width, constraints.maxHeight)
                            )
                            layout(width, placeable.height) {
                                placeable.place(0, 0)
                            }
                        }
                )
            }
        }

        // Pílula do thumb: sólida e branca em repouso (alinhada exatamente às bordas da trilha),
        // expandindo-se ao redor do próprio centro quando pressionada para que o vidro líquido
        // ultrapasse as extremidades arredondadas da barra nos limites de 0% e 100%.
        Box(
            Modifier
                .graphicsLayer {
                    val progress = dampedDragAnimation.progress.fastCoerceIn(0f, 1f)
                    val visualProgress = if (isLtr) progress else 1f - progress
                    translationX = trackInsetPx + usableTrackWidth * visualProgress
                }
                .then(if (reduceTransparency) {
                    Modifier.graphicsLayer {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                    }.background(if (isLightTheme) Color.White else Color(0xFFE5E5EA), ContinuousCapsule)
                } else Modifier.drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, trackBackdrop),
                    shape = { ContinuousCapsule },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Default.copy(
                            width = 0.75.dp,
                            style = HighlightStyle.Default(
                                angle = 45f,
                                falloff = 1.6f
                            ),
                            alpha = progress * 0.85f
                        )
                    },
                    shadow = {
                        val press = dampedDragAnimation.pressProgress
                        Shadow(
                            radius = lerp(4f, 8f, press).dp,
                            color = Color.Black.copy(alpha = lerp(0.16f, 0.24f, press))
                        )
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = 4f.dp * progress,
                            alpha = progress * 0.75f
                        )
                    },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        if (progress > 0.005f) {
                            blur(6f.dp.toPx() * (1f - progress))
                            lens(
                                refractionHeight = 10f.dp.toPx() * progress,
                                refractionAmount = 14f.dp.toPx() * progress,
                                depthEffect = true,
                                chromaticAberration = true
                            )
                        }
                    },
                    layerBlock = {
                        val velocity = dampedDragAnimation.velocity / 10f
                        val stretchX = (velocity * 0.65f).fastCoerceIn(-0.16f, 0.16f)
                        val stretchY = (velocity * 0.22f).fastCoerceIn(-0.12f, 0.12f)
                        scaleX = dampedDragAnimation.scaleX / (1f - stretchX)
                        scaleY = dampedDragAnimation.scaleY * (1f - stretchY)
                    },
                    onDrawSurface = {
                        val press = dampedDragAnimation.pressProgress.fastCoerceIn(0f, 1f)
                        if (press > 0.01f) {
                            val sliderProg = dampedDragAnimation.progress.fastCoerceIn(0f, 1f)
                            val neutralRim = if (isLightTheme) {
                                Color.Black.copy(alpha = 0.22f * press)
                            } else {
                                Color.White.copy(alpha = 0.42f * press)
                            }
                            val activeRim = accentColor.copy(alpha = 0.90f * press)
                            val rimBrush = when {
                                sliderProg <= 0.01f -> SolidColor(neutralRim)
                                sliderProg >= 0.99f -> SolidColor(activeRim)
                                else -> {
                                    val tStart = (sliderProg - 0.08f).coerceIn(0f, 1f)
                                    val tEnd = (sliderProg + 0.08f).coerceIn(tStart + 0.01f, 1f)
                                    Brush.horizontalGradient(
                                        0.0f to activeRim,
                                        tStart to activeRim,
                                        tEnd to neutralRim,
                                        1.0f to neutralRim
                                    )
                                }
                            }
                            val strokeWidthPx = 1.25.dp.toPx()
                            val halfStroke = strokeWidthPx / 2f
                            val radius = (size.height - strokeWidthPx) / 2f
                            drawRoundRect(
                                brush = rimBrush,
                                topLeft = Offset(halfStroke, halfStroke),
                                size = Size(size.width - strokeWidthPx, size.height - strokeWidthPx),
                                cornerRadius = CornerRadius(radius, radius),
                                style = Stroke(width = strokeWidthPx)
                            )
                        }
                        if (press < 1f) {
                            drawRect(Color.White.copy(alpha = 1f - press))
                        }
                    }
                ))
                .size(thumbWidthDp, thumbHeightDp)
        )
    }
}
