package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.theme.LocalAppMotion
import com.example.ui.theme.LocalAppSettings
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.theme.readableForeground
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.ContinuousRoundedRectangle
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.tanh

/** A retained, anchored glass surface with separately drawn, undistorted labels. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun RetainedGlassMenu(state: LiquidGlassMenuHostState, backdrop: Backdrop, modifier: Modifier) {
    val motion = LocalAppMotion.current
    val settings = LocalAppSettings.current
    val palette = LocalIOS27Theme.current
    val dark = palette.isDark
    val haptics = rememberAppHaptics()
    val active = state.currentRequest
    var retained by remember(state) { mutableStateOf<LiquidGlassMenuRequest?>(null) }
    val x = remember(state) { Animatable(0f, visibilityThreshold = 0.015f) }
    val y = remember(state) { Animatable(0f, visibilityThreshold = 0.015f) }
    var hovered by remember { mutableStateOf<Int?>(null) }
    var drag by remember { mutableStateOf(Offset.Zero) }
    var hostOrigin by remember { mutableStateOf(Offset.Zero) }

    DisposableEffect(state) {
        state.menuProgressProvider = { ((x.value + y.value) * 0.5f).coerceIn(0f, 1f) }
        onDispose { state.menuProgressProvider = null }
    }
    SideEffect { if (active != null) retained = active }

    LaunchedEffect(state, active?.id, active?.openNonce, motion.reduced) {
        hovered = null
        drag = Offset.Zero
        if (active != null) {
            // A different trigger starts at its own bounds. A reopen at the same trigger
            // reverses the collapse from the frame that is already on screen.
            if (state.morphingMenuId != active.id) {
                x.snapTo(0f)
                y.snapTo(0f)
            }
            state.morphingMenuId = active.id
            coroutineScope {
                launch { x.animateTo(1f, motion.spring(dampingRatio = 0.81f, stiffness = 760f)) }
                y.animateTo(1f, motion.spring(dampingRatio = 0.78f, stiffness = 690f))
            }
        } else {
            val closing = retained ?: return@LaunchedEffect
            coroutineScope {
                launch { x.animateTo(0f, motion.tween(150)) }
                y.animateTo(0f, motion.tween(150))
            }
            if (state.currentRequest == null) {
                retained = null
                state.morphingMenuId = null
                state.completeMenuCollapse(closing)
            }
        }
    }

    val request = active ?: retained ?: return
    if (request.items.isEmpty()) return
    val interactive by remember {
        derivedStateOf { minOf(x.value, y.value) >= 0.82f }
    }
    val acceptsInput = active != null && (
        motion.reduced || (state.morphingMenuId == active.id && interactive)
    )
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val insets = WindowInsets.safeDrawing
    val textMeasurer = rememberTextMeasurer()
    val menuTextStyle = LocalTextStyle.current.copy(fontSize = 15.sp, lineHeight = 20.sp)
    val rowHeight = with(density) { maxOf(48.dp, 40.sp.toDp() + 8.dp) }
    val rowPx = with(density) { rowHeight.roundToPx() }
    val dividerPx = with(density) { 0.5.dp.roundToPx() }
    val rowPaddingPx = with(density) { 7.dp.roundToPx() }
    val menuShape = remember { ContinuousRoundedRectangle(26.dp) }
    val rowShape = remember { ContinuousRoundedRectangle(16.dp) }
    val hasIcons = request.items.any { it.icon != null }
    val hasSelection = request.items.any { it.selected }
    val focusIndex = request.items.indexOfFirst { it.selected }.coerceAtLeast(0)
    val rowFocus = remember(request.id, request.openNonce) { FocusRequester() }
    val inputMode = LocalInputModeManager.current
    val windowInfo = LocalWindowInfo.current

    val hoverOffset = animateFloatAsState(
        targetValue = ((rowPx + dividerPx) * (hovered ?: 0)).toFloat(),
        animationSpec = motion.spring(dampingRatio = 0.75f, stiffness = 550f),
        label = "menuHoverPosition"
    )
    val hoverAlpha = animateFloatAsState(
        targetValue = if (hovered == null) 0f else 1f,
        animationSpec = motion.tween(90),
        label = "menuHoverAlpha"
    )
    val hoverColor = if (dark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.07f)
    val selectedColor = if (dark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.045f)
    val surfaceBase = if (dark) Color(0xFF171820) else Color.White
    val surfaceAlpha = when {
        settings.reduceTransparency -> 1f
        settings.increaseContrast -> 0.90f
        else -> (0.76f + 0.10f * (settings.glassOpacity - 1f)).coerceIn(0.74f, 0.94f)
    }
    val surface = surfaceBase.copy(alpha = surfaceAlpha)
    val contrastBackground = hoverColor.compositeOver(surface.compositeOver(if (dark) Color.White else Color.Black))
    val minContrast = if (settings.increaseContrast) 7f else 4.5f
    val ink = remember(palette.textPrimary, contrastBackground, minContrast) {
        readableForeground(palette.textPrimary, contrastBackground, minContrast)
    }
    val accent = remember(palette.accent, contrastBackground, minContrast) {
        readableForeground(palette.accent, contrastBackground, minContrast)
    }

    // Keep back and outside taps in this layer until the collapse finishes.
    BackHandler {
        if (state.currentRequest != null) state.dismissMenu(request.id)
    }

    fun choose(displayed: LiquidGlassMenuRequest, index: Int) {
        if (state.selectMenuItem(displayed, index, waitForCollapse = !motion.reduced)) {
            haptics.impactLight()
        }
    }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .zIndex(44f)
            .onGloballyPositioned { hostOrigin = it.positionInRoot() }
    ) {
        val margin = with(density) { 14.dp.roundToPx() }
        val leftLimit = margin + insets.getLeft(density, layoutDirection)
        val rightLimit = constraints.maxWidth - margin - insets.getRight(density, layoutDirection)
        val topLimit = margin + insets.getTop(density)
        val bottomLimit = constraints.maxHeight - margin - insets.getBottom(density)
        if (rightLimit <= leftLimit || bottomLimit <= topLimit) return@BoxWithConstraints

        Box(
            Modifier
                .fillMaxSize()
                .testTag("retained_glass_menu_scrim")
                .pointerInput(state, request.id, request.openNonce) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        down.consume()
                        val live = state.currentRequest
                        if (live?.id == request.id && live.openNonce == request.openNonce) {
                            state.dismissMenu(request.id)
                        }
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                    }
                }
        )

        val widestLabel = remember(request.items, textMeasurer, menuTextStyle) {
            request.items.maxOf { item ->
                textMeasurer.measure(
                    AnnotatedString(item.label),
                    menuTextStyle.copy(fontWeight = if (item.selected) FontWeight.SemiBold else FontWeight.Medium)
                ).size.width
            }
        }
        val rowChrome = 42.dp + (if (hasIcons) 30.dp else 0.dp) + (if (hasSelection) 28.dp else 0.dp)
        val source = request.anchorBounds
        val origin = IntOffset(hostOrigin.x.roundToInt(), hostOrigin.y.roundToInt())
        val anchor = IntRect(source.left - origin.x, source.top - origin.y, source.right - origin.x, source.bottom - origin.y)
        val width = with(density) {
            maxOf(widestLabel + rowChrome.roundToPx(), anchor.width, 158.dp.roundToPx())
        }.coerceAtMost(rightLimit - leftLimit)
        val desiredHeight = rowPx * request.items.size +
            dividerPx * (request.items.size - 1) + 2 * rowPaddingPx
        val above = anchor.bottom - topLimit
        val below = bottomLimit - anchor.top
        val down = desiredHeight <= below || (desiredHeight > above && below >= above)
        val minHeight = (rowPx + 2 * rowPaddingPx).coerceAtMost(bottomLimit - topLimit)
        val availableHeight = (if (down) below else above).coerceIn(minHeight, bottomLimit - topLimit)
        val height = desiredHeight.coerceAtMost(availableHeight)
        val scrollable = height < desiredHeight
        val right = anchor.center.x > (leftLimit + rightLimit) / 2
        val left = (if (right) anchor.right - width else anchor.left).coerceIn(leftLimit, rightLimit - width)
        val top = (if (down) anchor.top else anchor.bottom - height).coerceIn(topLimit, bottomLimit - height)
        val pivot = TransformOrigin(if (right) 1f else 0f, if (down) 0f else 1f)
        val sourceOffsetX = if (right) anchor.right - left - width else anchor.left - left
        val sourceOffsetY = if (down) anchor.top - top else anchor.bottom - top - height

        fun reveal() = when {
            motion.reduced -> if (active == null) 0f else 1f
            state.morphingMenuId != request.id -> 0f
            else -> minOf(x.value, y.value).coerceIn(0f, 1f)
        }

        Box(
            Modifier
                .offset { IntOffset(left, top) }
                .size(with(density) { width.toDp() }, with(density) { height.toDp() })
                .testTag("retained_glass_menu")
                .semantics {
                    if (acceptsInput) {
                        isTraversalGroup = true
                        paneTitle = request.triggerLabel.ifBlank { "Opções" }
                    } else {
                        invisibleToUser()
                    }
                }
                .then(if (scrollable || !acceptsInput) Modifier else Modifier.pointerInput(
                    request.id, request.openNonce, request.items.size, rowPx, dividerPx, rowPaddingPx
                ) {
                    val stride = rowPx + dividerPx
                    fun rowAt(point: Offset): Int? {
                        val yFromFirstRow = point.y - rowPaddingPx
                        if (point.x < rowPaddingPx || point.x >= size.width - rowPaddingPx || yFromFirstRow < 0f) return null
                        val index = (yFromFirstRow / stride).toInt()
                        return index.takeIf {
                            it in request.items.indices && yFromFirstRow - it * stride < rowPx
                        }
                    }
                    awaitEachGesture {
                        val first = awaitFirstDown(requireUnconsumed = true, pass = PointerEventPass.Initial)
                        first.consume()
                        hovered = rowAt(first.position)
                        if (hovered != null) haptics.selection()
                        try {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == first.id } ?: break
                                if (change.isConsumed || event.changes.any { it.id != first.id && it.pressed }) break
                                val row = rowAt(change.position)
                                if (row != null && hovered != row) {
                                    hovered = row
                                    haptics.selection()
                                }
                                change.consume()
                                if (!change.pressed) {
                                    // Sliding outside retains the last valid row from the same gesture.
                                    hovered?.let { choose(request, it) }
                                    break
                                }
                                drag = change.position - first.position
                            }
                        } finally {
                            hovered = null
                            drag = Offset.Zero
                        }
                    }
                })
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val progress = reveal()
                        val startX = (anchor.width.toFloat() / width).coerceIn(0.15f, 1f)
                        val startY = (anchor.height.toFloat() / height).coerceIn(0.15f, 1f)
                        val xProgress = if (motion.reduced) progress else x.value.coerceIn(0f, 1.025f)
                        val yProgress = if (motion.reduced) progress else y.value.coerceIn(0f, 1.025f)
                        transformOrigin = pivot
                        scaleX = startX + (1f - startX) * xProgress
                        scaleY = startY + (1f - startY) * yProgress
                        translationX = sourceOffsetX * (1f - progress)
                        translationY = sourceOffsetY * (1f - progress)
                        if (!motion.reduced) {
                            translationX += 6.dp.toPx() * tanh(drag.x / 86.dp.toPx())
                            translationY += 6.dp.toPx() * tanh(drag.y / 86.dp.toPx())
                        }
                        alpha = (progress / 0.4f).coerceIn(0f, 1f)
                    }
                    .then(if (settings.reduceTransparency) {
                        Modifier.background(surface, menuShape).border(0.8.dp, palette.divider, menuShape)
                    } else {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { menuShape },
                            highlight = { adaptiveGlassHighlight(isLight = !dark) },
                            shadow = {
                                Shadow(
                                    radius = 20.dp,
                                    offset = DpOffset(0.dp, 7.dp),
                                    color = Color.Black.copy(alpha = if (dark) 0.36f else 0.16f)
                                )
                            },
                            innerShadow = { InnerShadow(radius = 5.dp, alpha = if (dark) 0.23f else 0.14f) },
                            effects = {
                                vibrancy()
                                blur((10f * settings.glassBlurIntensity).coerceIn(5f, 20f).dp.toPx())
                                if (settings.enableLensRefraction && size.isSpecified && size.minDimension > 4f) {
                                    val intensity = settings.glassIntensity.coerceIn(0.6f, 2.2f)
                                    lens(
                                        refractionHeight = min((20f * intensity).dp.toPx(), size.minDimension * 0.36f),
                                        refractionAmount = (36f * intensity).dp.toPx(),
                                        depthEffect = true,
                                        chromaticAberration = settings.enableChromaticAberration
                                    )
                                }
                            },
                            onDrawSurface = { drawRect(surface) }
                        )
                    })
            )

            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val progress = ((reveal() - 0.63f) / 0.37f).coerceIn(0f, 1f)
                        transformOrigin = pivot
                        scaleX = 0.96f + 0.04f * progress
                        scaleY = scaleX
                        alpha = progress
                        if (!motion.reduced) {
                            translationX = 6.dp.toPx() * tanh(drag.x / 86.dp.toPx())
                            translationY = 6.dp.toPx() * tanh(drag.y / 86.dp.toPx())
                        }
                    }
                    .clip(menuShape)
            ) {
                if (!scrollable) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(rowHeight)
                            .padding(horizontal = 7.dp)
                            .graphicsLayer {
                                translationY = rowPaddingPx + hoverOffset.value
                                alpha = hoverAlpha.value
                            }
                            .background(hoverColor, rowShape)
                    )
                }

                val scroll = if (scrollable) {
                    val scrollState = key(request.id, request.openNonce) { rememberScrollState() }
                    Modifier.verticalScroll(scrollState, enabled = acceptsInput)
                } else {
                    Modifier
                }
                Column(scroll.padding(vertical = 7.dp)) {
                    request.items.forEachIndexed { index, item ->
                        if (index == focusIndex) {
                            LaunchedEffect(request.id, request.openNonce, acceptsInput) {
                                if (acceptsInput && inputMode.inputMode == InputMode.Keyboard) {
                                    snapshotFlow { windowInfo.isWindowFocused }.first { it }
                                    withFrameNanos { }
                                    rowFocus.requestFocus()
                                }
                            }
                        }
                        val contentColor = if (item.selected) accent else ink
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(rowHeight)
                                .padding(horizontal = 7.dp)
                                .clip(rowShape)
                                .background(if (item.selected) selectedColor else Color.Transparent)
                                .then(if (index == focusIndex) Modifier.focusRequester(rowFocus) else Modifier)
                                .semantics { if (hasSelection) selected = item.selected }
                                .clickable(
                                    enabled = acceptsInput,
                                    role = if (hasSelection) Role.RadioButton else Role.Button,
                                    onClickLabel = if (hasSelection) "Selecionar ${item.label}" else item.label,
                                    onClick = { choose(request, index) }
                                )
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (hasIcons) {
                                Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                                    item.icon?.let { Icon(it, null, tint = contentColor, modifier = Modifier.size(19.dp)) }
                                }
                            }
                            Text(
                                text = item.label,
                                modifier = Modifier.weight(1f),
                                style = menuTextStyle,
                                fontWeight = if (item.selected) FontWeight.SemiBold else FontWeight.Medium,
                                color = contentColor,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (hasSelection) {
                                Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                                    if (item.selected) Icon(Icons.Default.Check, null, tint = accent)
                                }
                            }
                        }
                        if (index < request.items.lastIndex) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(0.5.dp)
                                    .testTag("menu-divider-$index")
                                    .background(ink.copy(alpha = 0.10f))
                            )
                        }
                    }
                }
            }
        }
    }
}
