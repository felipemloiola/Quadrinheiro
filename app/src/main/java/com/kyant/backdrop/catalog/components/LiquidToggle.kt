package com.kyant.backdrop.catalog.components

import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.focusable
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.theme.LocalAppSettings
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
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
import com.kyant.shapes.ContinuousCapsule
import kotlinx.coroutines.flow.collectLatest

import com.example.ui.theme.LocalIsDarkTheme

@Composable
fun LiquidToggle(
    selected: () -> Boolean,
    onSelect: (Boolean) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    isLightTheme: Boolean = !LocalIsDarkTheme.current,
    accentColor: Color = LocalIOS27Theme.current.accent
) {
    val settings = LocalAppSettings.current
    val trackColor =
        if (isLightTheme) Color(0xFF787880).copy(0.28f)
        else Color(0xFF787880).copy(0.42f)

    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val dragWidth = with(density) { 20f.dp.toPx() }
    val animationScope = rememberCoroutineScope()
    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val reduceMotion = com.example.ui.theme.rememberReducedMotion()
    var didDrag by remember { mutableStateOf(false) }
    val dampedDragAnimation = remember(animationScope, dragWidth, isLtr) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = if (selected()) 1f else 0f,
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.5f,
            reduceMotion = reduceMotion,
            onDragStarted = { didDrag = false },
            onDragStopped = {
                val next = if (didDrag) targetValue >= 0.5f else !currentSelected()
                currentOnSelect(next)
                animateToValue(if (next) 1f else 0f)
                didDrag = false
            },
            onDragCancelled = {
                didDrag = false
                animateToValue(if (currentSelected()) 1f else 0f, 0f)
            },
            onDrag = { _, amount ->
                didDrag = true
                updateValue(targetValue + amount.x / dragWidth * if (isLtr) 1f else -1f)
            }
        )
    }
    LaunchedEffect(selected()) {
        val target = if (selected()) 1f else 0f
        if (dampedDragAnimation.targetValue != target) dampedDragAnimation.animateToValue(target, 0f)
    }

    val trackBackdrop = rememberLayerBackdrop()

    Box(
        modifier
            .size(64.dp, 48.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Switch
                toggleableState = ToggleableState(selected())
                onClick { currentOnSelect(!currentSelected()); true }
            }
            .onKeyEvent {
                if (it.type == KeyEventType.KeyUp && (it.key == Key.Enter || it.key == Key.Spacebar || it.key == Key.DirectionCenter)) {
                    currentOnSelect(!currentSelected())
                    true
                } else false
            }
            .focusable()
            .then(dampedDragAnimation.modifier),
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(
            Modifier
                .layerBackdrop(trackBackdrop)
                .clip(ContinuousCapsule)
                .size(64f.dp, 28f.dp)
        ) {
            val trackColor = lerp(trackColor, accentColor, dampedDragAnimation.value)
            drawRect(trackColor)
            if (settings.onOffLabels) {
                // 'I' indicator for ON (left side)
                drawLine(
                    color = Color.White.copy(alpha = 0.90f * dampedDragAnimation.value),
                    start = androidx.compose.ui.geometry.Offset(x = 13f.dp.toPx(), y = 8f.dp.toPx()),
                    end = androidx.compose.ui.geometry.Offset(x = 13f.dp.toPx(), y = 20f.dp.toPx()),
                    strokeWidth = 2f.dp.toPx()
                )
                // 'O' indicator for OFF (right side)
                drawCircle(
                    color = Color.White.copy(alpha = 0.70f * (1f - dampedDragAnimation.value)),
                    radius = 4f.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(x = 51f.dp.toPx(), y = 14f.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f.dp.toPx())
                )
            }
        }

        Box(
            Modifier
                .graphicsLayer {
                    val fraction = dampedDragAnimation.value
                    val padding = 2f.dp.toPx()
                    translationX =
                        if (isLtr) lerp(padding, padding + dragWidth, fraction)
                        else lerp(-padding, -(padding + dragWidth), fraction)
                }
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(
                        backdrop,
                        rememberBackdrop(trackBackdrop) { drawBackdrop ->
                            val progress = dampedDragAnimation.pressProgress
                            val scaleX = lerp(2f / 3f, 0.75f, progress)
                            val scaleY = lerp(0f, 0.75f, progress)
                            scale(scaleX, scaleY) {
                                drawBackdrop()
                            }
                        }
                    ),
                    shape = { ContinuousCapsule },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Default.copy(
                            style = HighlightStyle.Default(
                                angle = 45f,
                                falloff = 2f
                            ),
                            alpha = progress
                        )
                    },
                    shadow = {
                        Shadow(
                            radius = 4f.dp,
                            color = Color.Black.copy(alpha = 0.18f)
                        )
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = 4f.dp * progress,
                            alpha = progress
                        )
                    },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        blur(8f.dp.toPx() * (1f - progress))
                        lens(
                            5f.dp.toPx() * progress,
                            10f.dp.toPx() * progress,
                            chromaticAberration = true
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 50f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        val knobIdleColor = if (accentColor.luminance() > 0.65f) {
                            lerp(Color.White, Color(0xFF18181B), dampedDragAnimation.value)
                        } else {
                            Color.White
                        }
                        drawRect(knobIdleColor.copy(alpha = 1f - progress))
                    }
                )
                .size(40f.dp, 24f.dp)
        )
    }
}
