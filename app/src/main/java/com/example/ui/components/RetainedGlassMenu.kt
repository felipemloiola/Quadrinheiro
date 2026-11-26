package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.theme.*
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.*
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.ContinuousRoundedRectangle
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.tanh

/**
 * Floating iOS 26 / LiquidGlassKMP-inspired retained glass menu.
 * Measures once at final size; balanced jelly spring physics animate only the GPU render layer
 * while refracting the live backdrop behind the menu (`vibrancy -> blur -> lens`).
 */
@Composable
internal fun RetainedGlassMenu(state: LiquidGlassMenuHostState, backdrop: Backdrop, modifier: Modifier) {
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val menuTextStyle = androidx.compose.material3.LocalTextStyle.current.copy(
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium
    )
    val motion by rememberUpdatedState(LocalAppMotion.current)
    val haptics = rememberAppHaptics()
    val active = state.currentRequest
    var retained by remember { mutableStateOf<LiquidGlassMenuRequest?>(null) }
    val x = remember { Animatable(0f) }
    val y = remember { Animatable(0f) }
    var hovered by remember { mutableStateOf<Int?>(null) }
    var drag by remember { mutableStateOf(Offset.Zero) }
    var hostOrigin by remember { mutableStateOf(Offset.Zero) }

    DisposableEffect(state) {
        state.menuProgressProvider = { ((x.value + y.value) * 0.5f).coerceIn(0f, 1f) }
        onDispose { state.menuProgressProvider = null }
    }
    SideEffect { if (active != null) retained = active }

    LaunchedEffect(active?.id, active?.openNonce) {
        hovered = null
        drag = Offset.Zero
        if (active != null) {
            retained = active
            state.morphingMenuId = active.id
            if (motion.reduced) {
                x.snapTo(1f)
                y.snapTo(1f)
            } else {
                // Global iOS 26 floating menu morph: subtle X/Y phase offset for organic liquid expansion
                launch { x.animateTo(1f, motion.ios26FloatingMenuSpring(axis = 0)) }
                y.animateTo(1f, motion.ios26FloatingMenuSpring(axis = 1))
            }
        } else if (retained != null) {
            if (motion.reduced) {
                x.snapTo(0f)
                y.snapTo(0f)
            } else {
                launch { x.animateTo(0f, motion.ios26FloatingMenuCollapseSpring()) }
                y.animateTo(0f, motion.ios26FloatingMenuCollapseSpring())
            }
            if (state.currentRequest == null) {
                retained = null
                state.morphingMenuId = null
            }
        }
    }

    val request = active ?: retained ?: return
    if (request.items.isEmpty()) return
    val currentRequest by rememberUpdatedState(request)
    val density = LocalDensity.current
    val palette = LocalIOS27Theme.current
    val settings = LocalAppSettings.current
    val dark = LocalIsDarkTheme.current
    val isLight = !dark
    val hasAnyIcon = remember(request.items) { request.items.any { it.icon != null } }
    val menuShape = remember { ContinuousRoundedRectangle(26.dp) }
    val hoverPillShape = remember { ContinuousRoundedRectangle(16.dp) }

    val rowHeight = if (density.fontScale > 1.2f) (34f * density.fontScale + 16f).dp else 48.dp
    val hoverOffset by animateFloatAsState(
        targetValue = with(density) { (rowHeight + 0.5.dp).toPx() } * (hovered ?: 0),
        animationSpec = motion.spring(dampingRatio = 0.62f, stiffness = 480f),
        label = "menuHoverPosition"
    )
    val hoverAlpha by animateFloatAsState(
        targetValue = if (hovered == null) 0f else 1f,
        animationSpec = motion.tween(110),
        label = "menuHoverAlpha"
    )
    val hoverScale by animateFloatAsState(
        targetValue = if (hovered != null && !motion.reduced) 1.018f else 1f,
        animationSpec = motion.spring(dampingRatio = 0.58f, stiffness = 440f),
        label = "menuHoverScale"
    )

    BackHandler(active != null) { state.dismissMenu() }

    fun choose(index: Int) {
        val item = currentRequest.items.getOrNull(index) ?: return
        haptics.impactLight()
        item.onClick()
        state.dismissMenu(currentRequest.id)
    }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .zIndex(44f)
            .onGloballyPositioned { hostOrigin = it.positionInRoot() }
    ) {
        val widest = remember(request.items.map { it.label }, density.fontScale, menuTextStyle) {
            request.items.maxOfOrNull {
                textMeasurer.measure(androidx.compose.ui.text.AnnotatedString(it.label), menuTextStyle).size.width
            } ?: 0
        }
        val extraWidth = if (hasAnyIcon) 98.dp else 82.dp
        val menuWidth = minOf(
            maxOf(
                with(density) { widest.toDp() } + extraWidth,
                with(density) { request.anchorBounds.width.toDp() },
                192.dp
            ),
            maxWidth - 28.dp
        )
        val menuHeight = minOf(
            rowHeight * request.items.size + 0.5.dp * (request.items.size - 1) + 14.dp,
            maxHeight - 28.dp
        )
        val width = with(density) { menuWidth.roundToPx() }
        val height = with(density) { menuHeight.roundToPx() }
        val margin = with(density) { 14.dp.roundToPx() }
        val source = request.anchorBounds
        val anchor = IntRect(
            source.left - hostOrigin.x.roundToInt(),
            source.top - hostOrigin.y.roundToInt(),
            source.right - hostOrigin.x.roundToInt(),
            source.bottom - hostOrigin.y.roundToInt()
        )
        val right = anchor.center.x > constraints.maxWidth / 2
        val down = constraints.maxHeight - anchor.top - with(density) { 92.dp.roundToPx() } >= height || anchor.top < height
        val left = (if (right) anchor.right - width else anchor.left)
            .coerceIn(margin, (constraints.maxWidth - width - margin).coerceAtLeast(margin))
        val top = (if (down) anchor.top else anchor.bottom - height)
            .coerceIn(margin, (constraints.maxHeight - height - margin).coerceAtLeast(margin))

        if (active != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .testTag("retained_glass_menu_scrim")
                    .pointerInput(request.id, request.openNonce) {
                        awaitEachGesture {
                            val downEvent = awaitFirstDown(false)
                            downEvent.consume()
                            state.dismissMenu(request.id)
                        }
                    }
            )
        }

        val edgeRefraction = !settings.reduceTransparency && settings.enableLensRefraction
        val chromaticAberration = !settings.reduceTransparency && settings.enableChromaticAberration
        val refractionMult = settings.glassIntensity.coerceIn(0.6f, 2.2f)

        Box(
            Modifier
                .offset { IntOffset(left, top) }
                .size(menuWidth, menuHeight)
                .testTag("retained_glass_menu")
                .graphicsLayer {
                    transformOrigin = TransformOrigin(if (right) 1f else 0f, if (down) 0f else 1f)
                    val dragLimitX = 14f
                    val dragLimitY = 14f
                    val jellyDragX = if (motion.reduced) 0f else dragLimitX * tanh(drag.x / 220f)
                    val jellyDragY = if (motion.reduced) 0f else dragLimitY * tanh(drag.y / 220f)
                    val startScaleX = (anchor.width.toFloat() / width.coerceAtLeast(1)).coerceIn(0.18f, 1f)
                    val startScaleY = (anchor.height.toFloat() / height.coerceAtLeast(1)).coerceIn(0.18f, 1f)
                    scaleX = (startScaleX + (1f - startScaleX) * x.value).coerceAtLeast(0.05f)
                    scaleY = (startScaleY + (1f - startScaleY) * y.value).coerceAtLeast(0.05f)
                    translationX = ((if (right) anchor.right - left - width else anchor.left - left) * (1f - x.value)) + jellyDragX
                    translationY = ((if (down) anchor.top - top else anchor.bottom - top - height) * (1f - y.value)) + jellyDragY
                    alpha = (minOf(x.value, y.value) * 4.5f).coerceIn(0f, 1f)
                }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { menuShape },
                    highlight = {
                        Highlight.Default.copy(alpha = if (isLight) 0.84f else 0.58f)
                    },
                    shadow = {
                        Shadow(
                            radius = 22.dp,
                            offset = DpOffset(0.dp, 8.dp),
                            color = Color.Black.copy(alpha = if (isLight) 0.16f else 0.36f)
                        )
                    },
                    innerShadow = {
                        InnerShadow(
                            radius = 6.dp,
                            alpha = if (isLight) 0.14f else 0.26f
                        )
                    },
                    effects = {
                        if (!settings.reduceTransparency) {
                            vibrancy()
                            val blurPx = (10f * settings.glassBlurIntensity).coerceIn(5f, 22f).dp.toPx()
                            if (blurPx >= 1f) blur(blurPx)
                        }
                        if (edgeRefraction && size.isSpecified && size.width > 4f && size.height > 4f) {
                            val rHeight = ((20f * refractionMult).dp.toPx())
                                .coerceAtMost(min(size.width, size.height) * 0.36f)
                            val rAmount = (36f * refractionMult).dp.toPx()
                            if (rHeight >= 1f && rAmount >= 1f) {
                                lens(
                                    refractionHeight = rHeight,
                                    refractionAmount = rAmount,
                                    depthEffect = true,
                                    chromaticAberration = chromaticAberration
                                )
                            }
                        }
                    },
                    onDrawSurface = {
                        val baseColor = if (dark) Color(0xFF171820) else Color.White
                        val surfaceAlpha = if (settings.reduceTransparency) {
                            1f
                        } else {
                            ((if (dark) 0.36f else 0.38f) * settings.glassOpacity).coerceIn(0.20f, 0.84f)
                        }
                        drawRect(baseColor.copy(alpha = surfaceAlpha))
                        if (!settings.reduceTransparency) {
                            drawOpticalGlassMeniscus(
                                cornerRadiusPx = 26.dp.toPx(),
                                isLight = isLight,
                                edgeRefraction = edgeRefraction,
                                cyanDispersionStrength = 0.28f
                            )
                        }
                    }
                )
                .clip(menuShape)
                .pointerInput(request.id) {
                    awaitEachGesture {
                        val first = awaitFirstDown(false)
                        fun row(position: Offset): Int? =
                            if (position.x in 0f..size.width.toFloat() && position.y in 0f..size.height.toFloat()) {
                                ((position.y - 7.dp.toPx()) / (rowHeight + 0.5.dp).toPx())
                                    .toInt()
                                    .coerceIn(0, currentRequest.items.lastIndex)
                            } else null
                        hovered = row(first.position)
                        if (hovered != null) haptics.selection()
                        var released = false
                        try {
                            while (true) {
                                val change = awaitPointerEvent().changes.firstOrNull { it.id == first.id } ?: break
                                if (!change.pressed) {
                                    released = true
                                    break
                                }
                                row(change.position)?.let { nextRow ->
                                    if (nextRow != hovered) {
                                        hovered = nextRow
                                        haptics.selection()
                                    }
                                }
                                drag = change.position - first.position
                                change.consume()
                            }
                            if (released) hovered?.let { choose(it) }
                        } finally {
                            hovered = null
                            drag = Offset.Zero
                        }
                    }
                }
                .padding(vertical = 7.dp)
        ) {
            // Sliding liquid glass hover capsule (tracks touch/drag smoothly like iOS 26 / LiquidGlassKMP)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .padding(horizontal = 7.dp)
                    .graphicsLayer {
                        translationY = hoverOffset
                        alpha = hoverAlpha
                        scaleX = hoverScale
                        scaleY = hoverScale
                    }
                    .clip(hoverPillShape)
                    .background(
                        if (dark) Color.White.copy(alpha = 0.14f)
                        else Color.Black.copy(alpha = 0.065f)
                    )
                    .border(
                        width = 0.75.dp,
                        color = if (dark) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.55f),
                        shape = hoverPillShape
                    )
            )

            Column {
                request.items.forEachIndexed { index, item ->
                    val isRowHovered = hovered == index
                    val contentColor = if (item.selected) palette.accent else palette.textPrimary
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(rowHeight)
                            .padding(horizontal = 7.dp)
                            .clip(hoverPillShape)
                            .background(
                                if (item.selected && hovered == null) palette.accent.copy(alpha = if (dark) 0.12f else 0.08f)
                                else Color.Transparent
                            )
                            .semantics(mergeDescendants = true) {
                                selected = item.selected
                                role = Role.RadioButton
                                onClick("Selecionar ${item.label}") { choose(index); true }
                            }
                            .focusable()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (hasAnyIcon) {
                            // iOS 26 / LiquidGlassKMP layout: crisp leading icon on the left, trailing checkmark on the right
                            Box(
                                modifier = Modifier.width(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                item.icon?.let { iconVector ->
                                    Icon(
                                        imageVector = iconVector,
                                        contentDescription = null,
                                        tint = if (item.selected || isRowHovered) contentColor else palette.textPrimary.copy(alpha = 0.82f),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                            Text(
                                text = item.label,
                                modifier = Modifier.weight(1f),
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                                fontWeight = if (item.selected) FontWeight.SemiBold else FontWeight.Medium,
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (item.selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        } else {
                            // Compact text-only picker layout: aligned leading checkmark slot
                            Box(
                                modifier = Modifier.width(18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.selected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = palette.accent,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                            Text(
                                text = item.label,
                                modifier = Modifier.weight(1f),
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                                fontWeight = if (item.selected) FontWeight.SemiBold else FontWeight.Medium,
                                color = contentColor,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (index < request.items.lastIndex) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(0.5.dp)
                                .testTag("menu-divider-$index")
                                .background(
                                    if (dark) Color.White.copy(alpha = 0.10f)
                                    else Color.Black.copy(alpha = 0.08f)
                                )
                        )
                    }
                }
            }
        }
    }
}
