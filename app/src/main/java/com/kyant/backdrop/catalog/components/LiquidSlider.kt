package com.kyant.backdrop.catalog.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.lerp
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.theme.LocalIsDarkTheme
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
    accentColor: Color = LocalIOS27Theme.current.accent
) {
    val trackColor =
        if (isLightTheme) Color(0xFF787880).copy(alpha = 0.24f)
        else Color(0xFF787880).copy(alpha = 0.44f)

    val trackBackdrop = rememberLayerBackdrop()
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
                onDrag = { _, _ -> }
            )
        }

        val currentValueFn by rememberUpdatedState(value)
        val currentOnValueChange by rememberUpdatedState(onValueChange)
        val currentValueRange by rememberUpdatedState(valueRange)
        val currentUsableTrackWidth by rememberUpdatedState(usableTrackWidth)
        val currentTrackInsetPx by rememberUpdatedState(trackInsetPx)
        val currentThumbWidthPx by rememberUpdatedState(thumbWidthPx)
        val currentIsLtr by rememberUpdatedState(isLtr)
        val currentDampedAnim by rememberUpdatedState(dampedDragAnimation)

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
        Box(
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .layerBackdrop(trackBackdrop)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()

                        val anim = currentDampedAnim
                        val rng = currentValueRange
                        val span = (rng.endInclusive - rng.start).coerceAtLeast(0.0001f)
                        val uWidth = currentUsableTrackWidth
                        val tInset = currentTrackInsetPx
                        val tWidth = currentThumbWidthPx
                        val ltr = currentIsLtr

                        val currentProgress = anim.progress.fastCoerceIn(0f, 1f)
                        val visualProgress = if (ltr) currentProgress else 1f - currentProgress
                        val thumbLeftPx = tInset + uWidth * visualProgress
                        val thumbRightPx = thumbLeftPx + tWidth
                        val hitSlopPx = tWidth * 0.35f

                        isDraggingNow = true
                        SliderTouchLock.isAnySliderActive = true
                        view.parent?.requestDisallowInterceptTouchEvent(true)
                        anim.press()

                        try {
                            // Se o toque inicial foi fora do thumb (direto na barra), move suavemente até o ponto tocado
                            if (down.position.x < thumbLeftPx - hitSlopPx || down.position.x > thumbRightPx + hitSlopPx) {
                                val rawProgress = ((down.position.x - tInset - tWidth / 2f) / uWidth)
                                    .fastCoerceIn(0f, 1f)
                                val effectiveProgress = if (ltr) rawProgress else 1f - rawProgress
                                val tappedValue = lerp(rng.start, rng.endInclusive, effectiveProgress)
                                    .fastCoerceIn(rng.start, rng.endInclusive)
                                anim.updateValue(tappedValue)
                                currentOnValueChange(tappedValue)
                            }

                            var pointerId = down.id
                            while (true) {
                                // PointerEventPass.Initial garante captura e consumo antes de qualquer container pai (LazyColumn, Scrollable, etc.)
                                val event = awaitPointerEvent(PointerEventPass.Initial)

                                val change = event.changes.fastFirstOrNull { it.id == pointerId }
                                    ?: event.changes.fastFirstOrNull { it.pressed }
                                    ?: break

                                pointerId = change.id
                                if (change.changedToUpIgnoreConsumed() || !change.pressed) {
                                    val otherPressed = event.changes.fastFirstOrNull { it.pressed }
                                    if (otherPressed != null) {
                                        pointerId = otherPressed.id
                                        event.changes.forEach { it.consume() }
                                        continue
                                    }
                                    event.changes.forEach { it.consume() }
                                    break
                                }

                                // Calcula o deltaX a partir das coordenadas antes do consume
                                val deltaX = change.position.x - change.previousPosition.x
                                if (deltaX != 0f) {
                                    val activeRange = currentValueRange
                                    val activeSpan = (activeRange.endInclusive - activeRange.start).coerceAtLeast(0.0001f)
                                    val deltaValue = activeSpan * (deltaX / currentUsableTrackWidth)
                                    val nextValue = if (currentIsLtr) {
                                        (anim.targetValue + deltaValue)
                                            .fastCoerceIn(activeRange.start, activeRange.endInclusive)
                                    } else {
                                        (anim.targetValue - deltaValue)
                                            .fastCoerceIn(activeRange.start, activeRange.endInclusive)
                                    }
                                    anim.updateValue(nextValue)
                                    currentOnValueChange(nextValue)
                                }

                                // Consome 100% de todos os movimentos de ponteiro (incluindo qualquer movimento vertical Y para cima/baixo)
                                // Isso impede totalmente que a tela se mova verticalmente enquanto o usuário estiver segurando o slider
                                event.changes.forEach { it.consume() }
                            }
                        } finally {
                            isDraggingNow = false
                            SliderTouchLock.isAnySliderActive = false
                            view.parent?.requestDisallowInterceptTouchEvent(false)
                            currentOnValueChange(anim.targetValue)
                            anim.release()
                        }
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
                .drawBackdrop(
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
                        blur(6f.dp.toPx() * (1f - progress))
                        lens(
                            refractionHeight = 10f.dp.toPx() * progress,
                            refractionAmount = 14f.dp.toPx() * progress,
                            depthEffect = true,
                            chromaticAberration = true
                        )
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
                )
                .size(thumbWidthDp, thumbHeightDp)
        )
    }
}
