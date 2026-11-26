package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.example.ui.theme.LocalAppMotion
import com.example.ui.theme.LocalAppSettings
import com.example.ui.theme.LocalAppleMaterials
import com.kyant.backdrop.catalog.utils.InteractiveHighlight
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Circular iOS 26 / LiquidGlassKMP-style Liquid Glass menu trigger button (e.g. `LucideIcons.EllipsisCircle`).
 * Uses Tab-Bar-grade live refraction, optical meniscus, balanced jelly spring physics, and seamless
 * cross-morph expansion into `RetainedGlassMenu` via `LocalLiquidGlassMenuHost`.
 */
@Composable
fun <T> AppleIconMenuButton(
    options: List<SegmentOption<T>>,
    value: T,
    onChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = LucideIcons.EllipsisCircle,
    contentDescription: String = "Opções de ordenação",
    buttonSize: Dp = 44.dp,
    iconSize: Dp = 22.dp
) {
    val motion by rememberUpdatedState(LocalAppMotion.current)
    val mat = LocalAppleMaterials.current
    val s = mat.scheme
    val isLight = s.isLight
    val settings = LocalAppSettings.current
    val backdrop = rememberCurrentBackdrop()
    val menuHost = LocalLiquidGlassMenuHost.current
    val menuId = remember { "apple_icon_menu_${System.nanoTime()}" }

    var open by remember { mutableStateOf(false) }
    var anchorBounds by remember { mutableStateOf(IntRect.Zero) }
    val currentOpt = options.firstOrNull { it.value == value } ?: options.first()
    val currentOnChange by rememberUpdatedState(onChange)

    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (pressed && !motion.reduced) 1f else 0f,
        animationSpec = motion.spring(dampingRatio = 0.56f, stiffness = 390f),
        label = "iconMenuJellyPress"
    )
    val animationScope = rememberCoroutineScope()
    val triggerHighlight = remember(animationScope) {
        InteractiveHighlight(reduceMotion = { motion.reduced }, animationScope = animationScope)
    }

    val hostProgress = { menuHost?.progressForMenu(menuId) ?: 0f }
    fun triggerMorphAlpha() = (1f - (hostProgress() / 0.34f)).coerceIn(0f, 1f)
    fun triggerMorphScale() = lerp(1f, 0.92f, (hostProgress() / 0.45f).coerceIn(0f, 1f))

    val menuItems = remember(options, value) {
        options.mapIndexed { idx, opt ->
            LiquidGlassMenuItemData(
                label = opt.label,
                icon = opt.icon,
                selected = opt.value == value,
                showDividerBelow = idx < options.lastIndex,
                onClick = {
                    currentOnChange(opt.value)
                    open = false
                }
            )
        }
    }

    LaunchedEffect(open, anchorBounds, menuItems, currentOpt.label, menuHost) {
        if (menuHost != null) {
            if (open && anchorBounds.width > 0 && anchorBounds.height > 0 &&
                (menuHost.currentRequest?.id != menuId || menuHost.currentRequest?.items !== menuItems)) {
                menuHost.showMenu(
                    LiquidGlassMenuRequest(
                        id = menuId,
                        anchorBounds = anchorBounds,
                        triggerLabel = currentOpt.label,
                        triggerIcon = icon,
                        items = menuItems,
                        onDismiss = { open = false }
                    )
                )
            } else if (!open && menuHost.currentRequest?.id == menuId) {
                menuHost.dismissMenu(menuId)
            }
        }
    }

    DisposableEffect(menuId, menuHost) {
        onDispose { menuHost?.dismissMenu(menuId) }
    }

    var triggerCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    fun computeAnchorBounds(): IntRect {
        val coords = triggerCoords
        if (coords != null && coords.isAttached) {
            val pos = coords.positionInRoot()
            val sz = coords.size
            return IntRect(
                left = pos.x.roundToInt(),
                top = pos.y.roundToInt(),
                right = (pos.x + sz.width).roundToInt(),
                bottom = (pos.y + sz.height).roundToInt()
            )
        }
        return anchorBounds
    }

    val effectiveBlurDp = if (settings.reduceTransparency) 20.dp else (8f * settings.glassBlurIntensity).coerceIn(4f, 18f).dp
    val refractionMult = if (!settings.reduceTransparency && settings.enableLensRefraction) settings.glassIntensity.coerceIn(0.7f, 2.2f) else 0f
    val chromaticAberration = !settings.reduceTransparency && settings.enableChromaticAberration
    val glassAlpha = ((if (isLight) 0.28f else 0.30f) * settings.glassOpacity).coerceIn(0.14f, 0.68f)
    val baseSurfaceColor = if (isLight) Color.White.copy(alpha = glassAlpha) else Color(0xFF16171F).copy(alpha = glassAlpha)
    val activeAccent = if (isLight && s.accentColor == Color.White) s.textPrimary else s.accentColor

    Box(
        modifier = modifier
            .size(buttonSize)
            .onGloballyPositioned { coords ->
                triggerCoords = coords
                if (open && coords.isAttached) {
                    val newBounds = computeAnchorBounds()
                    if (anchorBounds != newBounds) {
                        anchorBounds = newBounds
                        menuHost?.updateAnchorBounds(menuId, newBounds)
                    }
                }
            }
            .graphicsLayer {
                alpha = triggerMorphAlpha()
            }
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                highlight = {
                    Highlight.Default.copy(
                        alpha = (if (isLight) 0.84f else 0.64f) * triggerMorphAlpha()
                    )
                },
                shadow = {
                    Shadow(
                        radius = 9.dp,
                        offset = DpOffset(0.dp, 3.dp),
                        color = Color.Black.copy(alpha = (if (isLight) 0.13f else 0.30f) * triggerMorphAlpha())
                    )
                },
                innerShadow = {
                    InnerShadow(
                        radius = 5.dp,
                        alpha = lerp(if (isLight) 0.16f else 0.28f, 0.42f, pressProgress) * triggerMorphAlpha()
                    )
                },
                effects = {
                    if (!settings.reduceTransparency) vibrancy()
                    val blurPx = effectiveBlurDp.toPx()
                    if (blurPx >= 1f && !blurPx.isNaN()) blur(blurPx)
                    if (refractionMult > 0f && size.isSpecified && size.width > 4f && size.height > 4f) {
                        val boost = 1f + 0.30f * pressProgress
                        val maxAllowedHeight = min(size.width, size.height) * 0.45f
                        lens(
                            refractionHeight = ((14f * refractionMult * boost).dp.toPx()).coerceAtMost(maxAllowedHeight),
                            refractionAmount = (28f * refractionMult * boost).dp.toPx(),
                            depthEffect = true,
                            chromaticAberration = chromaticAberration
                        )
                    }
                },
                layerBlock = if (!motion.reduced) {
                    {
                        if (size.isSpecified && size.width > 0f && size.height > 0f) {
                            val width = size.width
                            val height = size.height
                            val activeP = maxOf(pressProgress, triggerHighlight.pressProgress)
                            val scale = lerp(1f, 1f + 5.5f.dp.toPx() / height, activeP) * triggerMorphScale()

                            val maxOffset = size.minDimension.coerceAtLeast(1f)
                            val offset = triggerHighlight.offset
                            translationX = maxOffset * tanh(0.055f * offset.x / maxOffset)
                            translationY = maxOffset * tanh(0.055f * offset.y / maxOffset)

                            val maxDragScale = 5.5f.dp.toPx() / height
                            val offsetAngle = atan2(offset.y, offset.x)
                            val maxDim = size.maxDimension.coerceAtLeast(1f)
                            scaleX = scale +
                                maxDragScale * abs(cos(offsetAngle) * offset.x / maxDim) *
                                (width / height).fastCoerceAtMost(1f)
                            scaleY = scale +
                                maxDragScale * abs(sin(offsetAngle) * offset.y / maxDim) *
                                (height / width).fastCoerceAtMost(1f)
                        }
                    }
                } else null,
                onDrawSurface = {
                    drawRect(baseSurfaceColor)
                    if (!settings.reduceTransparency) {
                        drawOpticalGlassMeniscus(
                            cornerRadiusPx = min(size.width, size.height) * 0.5f,
                            isLight = isLight,
                            edgeRefraction = refractionMult > 0f,
                            cyanDispersionStrength = 0.36f
                        )
                    }
                }
            )
            .then(if (!motion.reduced) triggerHighlight.modifier else Modifier)
            .then(if (!motion.reduced) triggerHighlight.gestureModifier else Modifier)
            .clip(CircleShape)
            .focusable()
            .semantics(mergeDescendants = true) {
                this.contentDescription = "$contentDescription: ${currentOpt.label}"
            }
            .clickable(
                interactionSource = interactions,
                indication = null,
                role = Role.Button,
                onClickLabel = contentDescription
            ) {
                val nextOpen = !(if (menuHost != null) menuHost.currentRequest?.id == menuId else open)
                if (nextOpen) {
                    anchorBounds = computeAnchorBounds()
                }
                open = nextOpen
                if (menuHost != null) {
                    val activeBounds = anchorBounds
                    if (nextOpen && activeBounds.width > 0 && activeBounds.height > 0) {
                        menuHost.showMenu(
                            LiquidGlassMenuRequest(
                                id = menuId,
                                anchorBounds = activeBounds,
                                triggerLabel = currentOpt.label,
                                triggerIcon = icon,
                                items = menuItems,
                                onDismiss = { open = false }
                            )
                        )
                    } else if (!nextOpen) {
                        menuHost.dismissMenu(menuId)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (open) activeAccent else s.textPrimary,
            modifier = Modifier.size(iconSize)
        )
    }
}
