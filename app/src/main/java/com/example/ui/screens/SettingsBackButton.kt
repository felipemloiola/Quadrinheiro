package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.ui.components.LucideIcons
import com.example.ui.components.ProgressiveBlurContent
import com.example.ui.components.ProgressiveBlurDirection
import com.example.ui.components.adaptiveGlassHighlight
import com.example.ui.components.appleGlassColorControls
import com.example.ui.components.rememberAdaptiveLiquidGlassInk
import com.example.ui.components.rememberCurrentBackdrop
import com.example.ui.components.rememberInteractiveHighlight
import com.example.ui.theme.LocalAppMotion
import com.example.ui.theme.LocalAppSettings
import com.example.ui.theme.LocalAppleMaterials
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.theme.LocalIsDarkTheme
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.ContinuousCapsule
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Botão/pílula de voltar padrão iOS 26 Liquid Glass:
 * - `ContinuousCapsule` com altura fixa `44.dp` e mínimo de `44.dp` de largura (bolinha exata ao recolher)
 * - Escala elástica e `InteractiveHighlight` dentro de `layerBlock` (sem distorcer o backdrop)
 * - Suporte opcional a `adaptiveLuminance` e compatibilidade com `ReduceTransparency`/`ReduceMotion`.
 */
@Composable
internal fun SettingsBackButton(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    collapsed: Boolean = false,
    label: String = "Voltar",
    backdrop: Backdrop = rememberCurrentBackdrop(),
    adaptiveLuminance: Boolean = false,
    testTag: String = "settings_back_button"
) {
    val motion = LocalAppMotion.current
    val settings = LocalAppSettings.current
    val mat = LocalAppleMaterials.current
    val theme = LocalIOS27Theme.current
    val isDark = LocalIsDarkTheme.current
    val opaqueFallback = settings.reduceTransparency

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (isPressed && !motion.reduced) 1f else 0f,
        animationSpec = if (motion.reduced) motion.tween(0) else motion.spring(dampingRatio = 0.52f, stiffness = 440f),
        label = "backButtonJelly"
    )
    val interactiveHighlight = rememberInteractiveHighlight()
    val adaptiveInk = rememberAdaptiveLiquidGlassInk(
        backdrop = backdrop,
        enabled = adaptiveLuminance && !opaqueFallback,
        fallbackColor = theme.textPrimary,
        fallbackIsLightBackground = !isDark
    )
    val inkColor = if (adaptiveLuminance) adaptiveInk.color else theme.textPrimary

    val glassAlpha = ((if (!isDark) 0.54f else 0.40f) * settings.glassOpacity).coerceIn(0.24f, 0.85f)
    val surfaceFill = if (opaqueFallback) {
        if (!isDark) Color(0xFFF2F2F7).copy(alpha = 0.94f) else Color(0xFF1C1C22).copy(alpha = 0.94f)
    } else {
        if (!isDark) Color.White.copy(alpha = glassAlpha) else Color(0xFF15151B).copy(alpha = glassAlpha)
    }

    Row(
        modifier
            .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
            .height(44.dp)
            .testTag(testTag)
            .then(if (adaptiveLuminance) adaptiveInk.samplingModifier else Modifier)
            .then(
                if (opaqueFallback) {
                    Modifier
                        .graphicsLayer {
                            if (!motion.reduced && pressProgress > 0.001f) {
                                val s = lerp(1f, 1.06f, pressProgress)
                                scaleX = s
                                scaleY = s
                            }
                        }
                        .clip(ContinuousCapsule)
                        .background(surfaceFill, ContinuousCapsule)
                } else {
                    Modifier
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { ContinuousCapsule },
                            highlight = {
                                adaptiveGlassHighlight(
                                    isLight = !isDark,
                                    alpha = if (!isDark) 0.82f else 0.66f
                                )
                            },
                            shadow = {
                                Shadow(
                                    radius = 10.dp,
                                    offset = DpOffset(0.dp, 3.dp),
                                    color = Color.Black.copy(alpha = if (!isDark) 0.14f else 0.32f)
                                )
                            },
                            innerShadow = {
                                InnerShadow(
                                    radius = 4.5.dp,
                                    alpha = lerp(if (!isDark) 0.16f else 0.28f, 0.40f, pressProgress)
                                )
                            },
                            effects = {
                                appleGlassColorControls(
                                    isLight = !isDark,
                                    isFrosted = settings.glassVariant == "frosted",
                                    reduceTransparency = settings.reduceTransparency
                                )
                                val blurPx = ((5.5f * settings.glassBlurIntensity).coerceIn(2.5f, 16f)).dp.toPx()
                                if (blurPx >= 1f && !blurPx.isNaN()) {
                                    blur(blurPx)
                                }
                                if (mat.edgeRefraction &&
                                    settings.enableLensRefraction &&
                                    this.size.isSpecified &&
                                    this.size.width > 4f &&
                                    this.size.height > 4f
                                ) {
                                    val mult = settings.glassIntensity.coerceIn(0.5f, 2.2f) * (1f + 0.24f * pressProgress)
                                    val maxH = min(this.size.width, this.size.height) * 0.46f
                                    val rHeight = ((14f * mult).dp.toPx()).coerceAtMost(maxH)
                                    val rAmount = (28f * mult).dp.toPx()
                                    if (rHeight >= 1f && rAmount >= 1f) {
                                        lens(
                                            refractionHeight = rHeight,
                                            refractionAmount = rAmount,
                                            depthEffect = true,
                                            chromaticAberration = settings.enableChromaticAberration
                                        )
                                    }
                                }
                            },
                            layerBlock = if (!motion.reduced) {
                                {
                                    if (this.size.isSpecified && this.size.width > 0f && this.size.height > 0f) {
                                        val swell = lerp(1f, 1.07f, pressProgress)
                                        val maxOffset = this.size.minDimension
                                        val offset = interactiveHighlight.offset
                                        translationX = maxOffset * tanh(0.055f * offset.x / maxOffset)
                                        translationY = maxOffset * tanh(0.055f * offset.y / maxOffset)

                                        val maxDragScale = 5.5f.dp.toPx() / this.size.height
                                        val angle = atan2(offset.y, offset.x)
                                        val stretchX = maxDragScale * abs(cos(angle) * offset.x / this.size.maxDimension)
                                        val stretchY = maxDragScale * abs(sin(angle) * offset.y / this.size.maxDimension)
                                        scaleX = swell + stretchX - stretchY * 0.28f
                                        scaleY = swell + stretchY - stretchX * 0.28f
                                    }
                                }
                            } else null,
                            onDrawSurface = {
                                drawRect(surfaceFill)
                            }
                        )
                        .clip(ContinuousCapsule)
                        .then(
                            if (!motion.reduced) {
                                Modifier
                                    .then(interactiveHighlight.modifier)
                                    .then(interactiveHighlight.gestureModifier)
                            } else {
                                Modifier
                            }
                        )
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = if (opaqueFallback) LocalIndication.current else null,
                role = Role.Button,
                onClick = onBack
            )
            .padding(horizontal = if (collapsed) 0.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = LucideIcons.ChevronLeft,
            contentDescription = "Voltar",
            tint = inkColor,
            modifier = Modifier.size(20.dp)
        )
        AnimatedVisibility(
            visible = !collapsed,
            enter = expandHorizontally(motion.spring(0.84f, 540f)) + fadeIn(motion.tween(180)),
            exit = shrinkHorizontally(motion.spring(0.84f, 540f)) + fadeOut(motion.tween(120))
        ) {
            Text(
                text = label,
                color = inkColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(start = 2.dp, end = 6.dp)
            )
        }
    }
}

@Composable
internal fun rememberScrollCollapsed(
    listState: LazyListState,
    thresholdPx: Int = 24
): State<Boolean> = remember(listState, thresholdPx) {
    derivedStateOf {
        listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > thresholdPx
    }
}

@Composable
internal fun rememberScrollCollapsed(
    gridState: LazyGridState,
    thresholdPx: Int = 24
): State<Boolean> = remember(gridState, thresholdPx) {
    derivedStateOf {
        gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > thresholdPx
    }
}

@Composable
internal fun rememberScrollCollapsed(
    scrollState: ScrollState,
    thresholdPx: Int = 24
): State<Boolean> = remember(scrollState, thresholdPx) {
    derivedStateOf {
        scrollState.value > thresholdPx
    }
}

@Composable
internal fun rememberScrollEdgeFraction(
    listState: LazyListState,
    fadeDistanceDp: Dp = 64.dp
): State<Float> {
    val fadePx = with(LocalDensity.current) { fadeDistanceDp.toPx().coerceAtLeast(1f) }
    return remember(listState, fadePx) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / fadePx).coerceIn(0f, 1f)
        }
    }
}

@Composable
internal fun rememberScrollEdgeFraction(
    gridState: LazyGridState,
    fadeDistanceDp: Dp = 64.dp
): State<Float> {
    val fadePx = with(LocalDensity.current) { fadeDistanceDp.toPx().coerceAtLeast(1f) }
    return remember(gridState, fadePx) {
        derivedStateOf {
            if (gridState.firstVisibleItemIndex > 0) 1f
            else (gridState.firstVisibleItemScrollOffset / fadePx).coerceIn(0f, 1f)
        }
    }
}

@Composable
internal fun rememberScrollEdgeFraction(
    scrollState: ScrollState,
    fadeDistanceDp: Dp = 64.dp
): State<Float> {
    val fadePx = with(LocalDensity.current) { fadeDistanceDp.toPx().coerceAtLeast(1f) }
    return remember(scrollState, fadePx) {
        derivedStateOf {
            (scrollState.value / fadePx).coerceIn(0f, 1f)
        }
    }
}

/**
 * Overlay superior fixo com botão de voltar recolhível (`SettingsBackButton`) e faixa opcional de
 * Scroll Edge Effect (`ProgressiveBlurContent`), projetado para ficar como irmão fora da subárvore `.layerBackdrop`.
 */
@Composable
internal fun LiquidGlassBackOverlay(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    collapsed: Boolean = false,
    label: String = "Voltar",
    backdrop: Backdrop = rememberCurrentBackdrop(),
    adaptiveLuminance: Boolean = false,
    applyStatusBarsPadding: Boolean = true,
    horizontalPadding: Dp = 16.dp,
    topPadding: Dp = 12.dp,
    bottomPadding: Dp = 8.dp,
    showScrollEdgeBlur: Boolean = true,
    scrollEdgeFraction: Float = if (collapsed) 1f else 0f,
    scrollEdgeHeight: Dp = 92.dp,
    backButtonTestTag: String = "settings_back_button",
    centerContent: (@Composable RowScope.() -> Unit)? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null
) {
    val motion = LocalAppMotion.current
    val theme = LocalIOS27Theme.current
    val edgeAlpha by animateFloatAsState(
        targetValue = if (showScrollEdgeBlur) scrollEdgeFraction.coerceIn(0f, 1f) else 0f,
        animationSpec = motion.tween(160),
        label = "backOverlayScrollEdge"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (showScrollEdgeBlur) Modifier.heightIn(min = scrollEdgeHeight) else Modifier)
    ) {
        if (showScrollEdgeBlur && edgeAlpha > 0.01f) {
            ProgressiveBlurContent(
                backdrop = backdrop,
                direction = ProgressiveBlurDirection.TopToBottom,
                blurRadius = 16.dp,
                tintColor = theme.bgPrimary.copy(alpha = 0.72f),
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = edgeAlpha }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (applyStatusBarsPadding) Modifier.statusBarsPadding() else Modifier)
                .padding(horizontal = horizontalPadding)
                .padding(top = topPadding, bottom = bottomPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsBackButton(
                onBack = onBack,
                collapsed = collapsed,
                label = label,
                backdrop = backdrop,
                adaptiveLuminance = adaptiveLuminance,
                testTag = backButtonTestTag
            )
            if (centerContent != null) {
                centerContent()
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            trailingContent?.invoke(this)
        }
    }
}
