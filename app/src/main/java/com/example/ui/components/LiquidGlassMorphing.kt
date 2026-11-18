package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.example.ui.theme.LocalAppMotion
import com.example.ui.theme.LocalAppSettings
import com.example.ui.theme.LocalAppleMaterials
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.theme.LocalIsDarkTheme
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.catalog.utils.InteractiveHighlight
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.ContinuousCapsule
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Coordinates sibling Liquid Glass controls in a floating navigation/action bar
 * (Compose adaptation of SwiftUI's WWDC25 `GlassEffectContainer`).
 * Never nests glass-on-glass: each child refracts the shared underlying [backdrop] directly
 * while reacting sympathetically when a neighbour swells on touch.
 */
@Stable
internal class GlassContainerCoordinator {
    var activeIndex by mutableIntStateOf(-1)
    var activePressProgress by mutableFloatStateOf(0f)
}

internal val LocalGlassContainerCoordinator = compositionLocalOf<GlassContainerCoordinator?> { null }

@Composable
internal fun LiquidGlassEffectContainer(
    modifier: Modifier = Modifier,
    spacing: Dp = 10.dp,
    backdrop: Backdrop = rememberCurrentBackdrop(),
    content: @Composable RowScope.() -> Unit
) {
    val coordinator = remember { GlassContainerCoordinator() }
    CompositionLocalProvider(
        LocalLiquidGlassBackdrop provides backdrop,
        LocalGlassContainerCoordinator provides coordinator
    ) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * Interactive WWDC25 Liquid Glass capsule button powered by `Kyant0/AndroidLiquidGlass`:
 * - Real `vibrancy` -> `blur` -> `lens` refraction with chromatic aberration
 * - Radial AGSL `InteractiveHighlight` following the finger
 * - Anisotropic jelly stretch (`tanh` translation + directional `scaleX`/`scaleY` + underdamped spring bounce)
 * - Strict single-line typography (`maxLines = 1`, `softWrap = false`) so labels never wrap awkwardly
 * - Full `reduceTransparency` and `reduceMotion` accessibility fallbacks.
 */
@Composable
internal fun LiquidGlassPillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    prominent: Boolean = false,
    slotIndex: Int = 0,
    height: Dp = 50.dp,
    fontSize: TextUnit = 14.5.sp,
    accessibilityLabel: String? = null
) {
    val backdrop = rememberCurrentBackdrop()
    val theme = LocalIOS27Theme.current
    val mat = LocalAppleMaterials.current
    val settings = LocalAppSettings.current
    val motion = LocalAppMotion.current
    val isDark = LocalIsDarkTheme.current
    val coordinator = LocalGlassContainerCoordinator.current
    val scope = rememberCoroutineScope()

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (isPressed && enabled && !motion.reduced) 1f else 0f,
        animationSpec = if (motion.reduced) motion.tween(0) else motion.spring(dampingRatio = 0.52f, stiffness = 440f),
        label = "liquidPillPress"
    )

    SideEffect {
        if (coordinator != null) {
            if (isPressed && enabled) {
                coordinator.activeIndex = slotIndex
                coordinator.activePressProgress = pressProgress
            } else if (coordinator.activeIndex == slotIndex && pressProgress <= 0.01f) {
                coordinator.activeIndex = -1
                coordinator.activePressProgress = 0f
            }
        }
    }

    val interactiveHighlight = remember(scope) {
        InteractiveHighlight(
            animationScope = scope,
            reduceMotion = { motion.reduced }
        )
    }

    val foregroundColor = when {
        prominent -> theme.bgPrimary
        else -> theme.textPrimary
    }.copy(alpha = if (enabled) 1f else 0.42f)

    val opaqueFallback = settings.reduceTransparency
    val glassAlpha = ((if (!isDark) 0.56f else 0.44f) * settings.glassOpacity).coerceIn(0.28f, 0.86f)
    val surfaceFill = when {
        prominent -> theme.textPrimary.copy(alpha = if (opaqueFallback) 0.96f else 0.90f)
        opaqueFallback -> if (!isDark) Color(0xFFF2F2F7).copy(alpha = 0.94f) else Color(0xFF1C1C22).copy(alpha = 0.94f)
        else -> if (!isDark) Color.White.copy(alpha = glassAlpha) else Color(0xFF15151B).copy(alpha = glassAlpha)
    }

    Row(
        modifier = modifier
            .height(height)
            .semantics {
                if (accessibilityLabel != null) contentDescription = accessibilityLabel
            }
            .then(
                if (opaqueFallback) {
                    Modifier
                        .graphicsLayer {
                            if (!motion.reduced && size.isSpecified && size.height > 0f) {
                                val s = lerp(1f, 1.035f, pressProgress)
                                scaleX = s
                                scaleY = s
                            }
                        }
                        .clip(ContinuousCapsule)
                        .background(surfaceFill, ContinuousCapsule)
                } else {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousCapsule },
                        highlight = {
                            adaptiveGlassHighlight(
                                isLight = !isDark,
                                alpha = if (prominent) 0.38f else if (!isDark) 0.84f else 0.68f
                            )
                        },
                        shadow = {
                            Shadow(
                                radius = 12.dp,
                                offset = DpOffset(0.dp, 4.dp),
                                color = Color.Black.copy(alpha = if (!isDark) 0.14f else 0.34f)
                            )
                        },
                        innerShadow = {
                            InnerShadow(
                                radius = 5.dp,
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
                            if (mat.edgeRefraction && settings.enableLensRefraction && size.isSpecified && size.width > 4f && size.height > 4f) {
                                val mult = settings.glassIntensity.coerceIn(0.5f, 2.2f) * (1f + 0.24f * pressProgress)
                                val maxH = min(size.width, size.height) * 0.46f
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
                                if (size.isSpecified && size.width > 0f && size.height > 0f) {
                                    val w = size.width
                                    val h = size.height
                                    val swell = lerp(1f, 1f + 5.5f.dp.toPx() / h, pressProgress)
                                    val maxOffset = size.minDimension
                                    val offset = interactiveHighlight.offset
                                    val symNudge = if (coordinator != null && coordinator.activeIndex >= 0 && coordinator.activeIndex != slotIndex) {
                                        val dir = if (slotIndex > coordinator.activeIndex) 1f else -1f
                                        dir * 2.dp.toPx() * coordinator.activePressProgress
                                    } else 0f

                                    translationX = maxOffset * tanh(0.055f * offset.x / maxOffset) + symNudge
                                    translationY = maxOffset * tanh(0.055f * offset.y / maxOffset)

                                    val maxDragScale = 6f.dp.toPx() / h
                                    val angle = atan2(offset.y, offset.x)
                                    val stretchX = maxDragScale * abs(cos(angle) * offset.x / size.maxDimension) * (w / h).fastCoerceAtMost(1f)
                                    val stretchY = maxDragScale * abs(sin(angle) * offset.y / size.maxDimension) * (h / w).fastCoerceAtMost(1f)
                                    // Volume-preserving jelly feel: stretching along X subtly compresses Y
                                    scaleX = swell + stretchX - stretchY * 0.28f
                                    scaleY = swell + stretchY - stretchX * 0.28f
                                }
                            }
                        } else null,
                        onDrawSurface = {
                            drawRect(surfaceFill)
                        }
                    )
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = if (opaqueFallback) LocalIndication.current else null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .then(
                if (!opaqueFallback && enabled) {
                    Modifier
                        .then(interactiveHighlight.modifier)
                        .then(interactiveHighlight.gestureModifier)
                } else Modifier
            )
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = foregroundColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = label,
            color = foregroundColor,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Circular Liquid Glass button with jelly bounce, radial AGSL highlight, icon rotation/morph,
 * and seamless droplet cross-morphing when paired with [LiquidGlassMenuHostState].
 */
@Composable
internal fun LiquidMorphingIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    enabled: Boolean = true,
    slotIndex: Int = 1,
    menuId: String? = null,
    iconRotationWhenMenuOpen: Float = 0f,
    adaptiveLuminance: Boolean = false,
    onAnchorBoundsChanged: ((IntRect) -> Unit)? = null
) {
    val backdrop = rememberCurrentBackdrop()
    val theme = LocalIOS27Theme.current
    val mat = LocalAppleMaterials.current
    val settings = LocalAppSettings.current
    val motion = LocalAppMotion.current
    val isDark = LocalIsDarkTheme.current
    val menuHost = LocalLiquidGlassMenuHost.current
    val coordinator = LocalGlassContainerCoordinator.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (isPressed && enabled && !motion.reduced) 1f else 0f,
        animationSpec = if (motion.reduced) motion.tween(0) else motion.spring(dampingRatio = 0.48f, stiffness = 430f),
        label = "liquidIconJellyPress"
    )

    SideEffect {
        if (coordinator != null) {
            if (isPressed && enabled) {
                coordinator.activeIndex = slotIndex
                coordinator.activePressProgress = pressProgress
            } else if (coordinator.activeIndex == slotIndex && pressProgress <= 0.01f) {
                coordinator.activeIndex = -1
                coordinator.activePressProgress = 0f
            }
        }
    }

    val interactiveHighlight = remember(scope) {
        InteractiveHighlight(
            animationScope = scope,
            reduceMotion = { motion.reduced }
        )
    }

    val hostProgress = { if (menuId != null) menuHost?.progressForMenu(menuId) ?: 0f else 0f }
    fun morphAlpha() = (1f - (hostProgress() / 0.36f)).coerceIn(0f, 1f)
    fun morphScale() = lerp(1f, 0.86f, (hostProgress() / 0.45f).coerceIn(0f, 1f))

    val opaqueFallback = settings.reduceTransparency
    val adaptiveInk = rememberAdaptiveLiquidGlassInk(
        backdrop = backdrop,
        enabled = adaptiveLuminance && !opaqueFallback,
        fallbackColor = theme.textPrimary,
        fallbackIsLightBackground = !isDark
    )
    val resolvedTint = (if (adaptiveLuminance) adaptiveInk.color else theme.textPrimary)
        .copy(alpha = if (enabled) 1f else 0.42f)

    val glassAlpha = ((if (!isDark) 0.54f else 0.40f) * settings.glassOpacity).coerceIn(0.24f, 0.85f)
    val surfaceFill = if (opaqueFallback) {
        if (!isDark) Color(0xFFF2F2F7).copy(alpha = 0.94f) else Color(0xFF1C1C22).copy(alpha = 0.94f)
    } else {
        if (!isDark) Color.White.copy(alpha = glassAlpha) else Color(0xFF15151B).copy(alpha = glassAlpha)
    }

    var coordsRef by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(
        modifier = modifier
            .sizeIn(minWidth = 44.dp, minHeight = 44.dp)
            .then(if (adaptiveLuminance) adaptiveInk.samplingModifier else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = if (opaqueFallback) LocalIndication.current else null,
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    coordsRef?.takeIf { it.isAttached }?.let { coords ->
                        val p = coords.positionInRoot()
                        val sz = coords.size
                        onAnchorBoundsChanged?.invoke(
                            IntRect(
                                left = p.x.roundToInt(),
                                top = p.y.roundToInt(),
                                right = (p.x + sz.width).roundToInt(),
                                bottom = (p.y + sz.height).roundToInt()
                            )
                        )
                    }
                    if (!motion.reduced && menuId != null) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onClick()
                }
            )
            .then(
                if (!opaqueFallback && enabled) {
                    interactiveHighlight.gestureModifier
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .onGloballyPositioned { coords ->
                    coordsRef = coords
                    if (onAnchorBoundsChanged != null && coords.isAttached) {
                        val p = coords.positionInRoot()
                        val sz = coords.size
                        onAnchorBoundsChanged(
                            IntRect(
                                left = p.x.roundToInt(),
                                top = p.y.roundToInt(),
                                right = (p.x + sz.width).roundToInt(),
                                bottom = (p.y + sz.height).roundToInt()
                            )
                        )
                    }
                }
                .graphicsLayer {
                    alpha = morphAlpha()
                }
                .then(
                    if (opaqueFallback) {
                        Modifier
                            .graphicsLayer {
                                val s = lerp(1f, 1.08f, pressProgress) * morphScale()
                                scaleX = s
                                scaleY = s
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
                                        alpha = (if (!isDark) 0.84f else 0.68f) * morphAlpha()
                                    )
                                },
                                shadow = {
                                    Shadow(
                                        radius = 10.dp,
                                        offset = DpOffset(0.dp, 3.dp),
                                        color = Color.Black.copy(alpha = (if (!isDark) 0.14f else 0.32f) * morphAlpha())
                                    )
                                },
                                innerShadow = {
                                    InnerShadow(
                                        radius = 4.5.dp,
                                        alpha = lerp(if (!isDark) 0.16f else 0.28f, 0.42f, pressProgress) * morphAlpha()
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
                                    if (mat.edgeRefraction && settings.enableLensRefraction && this.size.isSpecified && this.size.width > 4f && this.size.height > 4f) {
                                        val mult = settings.glassIntensity.coerceIn(0.5f, 2.2f) * (1f + 0.28f * pressProgress)
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
                                            val swell = lerp(1f, 1.13f, pressProgress) * morphScale()
                                            val maxOffset = this.size.minDimension
                                            val offset = interactiveHighlight.offset
                                            val symNudge = if (coordinator != null && coordinator.activeIndex >= 0 && coordinator.activeIndex != slotIndex) {
                                                val dir = if (slotIndex > coordinator.activeIndex) 1f else -1f
                                                dir * 2.dp.toPx() * coordinator.activePressProgress
                                            } else 0f

                                            translationX = maxOffset * tanh(0.065f * offset.x / maxOffset) + symNudge
                                            translationY = maxOffset * tanh(0.065f * offset.y / maxOffset)

                                            val maxDragScale = 6.5f.dp.toPx() / this.size.height
                                            val angle = atan2(offset.y, offset.x)
                                            val stretchX = maxDragScale * abs(cos(angle) * offset.x / this.size.maxDimension)
                                            val stretchY = maxDragScale * abs(sin(angle) * offset.y / this.size.maxDimension)
                                            scaleX = swell + stretchX - stretchY * 0.32f
                                            scaleY = swell + stretchY - stretchX * 0.32f
                                        }
                                    }
                                } else null,
                                onDrawSurface = {
                                    drawRect(surfaceFill)
                                }
                            )
                            .clip(ContinuousCapsule)
                            .then(if (enabled) interactiveHighlight.modifier else Modifier)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = resolvedTint,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        if (!motion.reduced) {
                            val mp = hostProgress().coerceIn(0f, 1f)
                            rotationZ = mp * iconRotationWhenMenuOpen
                            val iconScale = lerp(1f, 0.72f, mp) * lerp(1f, 1.08f, pressProgress)
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                    }
            )
        }
    }
}
