package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.*
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.ContinuousCapsule
import com.kyant.shapes.ContinuousRoundedRectangle
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tanh

/** The menu is measured once at its final size. Springs only transform its render layer. */
@Composable
internal fun RetainedGlassMenu(state: LiquidGlassMenuHostState, backdrop: Backdrop, modifier: Modifier) {
    val textMeasurer=androidx.compose.ui.text.rememberTextMeasurer()
    val menuTextStyle=androidx.compose.material3.LocalTextStyle.current.copy(fontSize=14.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Medium)
    val motion by rememberUpdatedState(LocalAppMotion.current)
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
    LaunchedEffect(active?.id) {
        hovered = null
        drag = Offset.Zero
        if (active != null) {
            retained = active
            state.morphingMenuId = active.id
            if (motion.reduced) {
                x.snapTo(1f)
                y.snapTo(1f)
            } else {
                // Expressive jelly droplet expansion (underdamped spring with volume-preserving overshoot)
                launch { x.animateTo(1f, motion.spring(0.55f, 430f)) }
                y.animateTo(1f, motion.spring(0.50f, 375f))
            }
        } else if (retained != null) {
            if (motion.reduced) {
                x.snapTo(0f)
                y.snapTo(0f)
            } else {
                kotlinx.coroutines.coroutineScope {
                    launch { x.animateTo(0f, motion.spring(0.84f, 740f)) }
                    y.animateTo(0f, motion.spring(0.84f, 740f))
                }
            }
            if (state.currentRequest == null) { retained = null; state.morphingMenuId = null }
        }
    }
    val request = active ?: retained ?: return
    if (request.items.isEmpty()) return
    val currentRequest by rememberUpdatedState(request)
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val palette = LocalIOS27Theme.current
    val settings = LocalAppSettings.current
    val dark = LocalIsDarkTheme.current
    // Text-bearing menus need a regular material even over white/black artwork.
    // Keep the opacity preference within a contrast-safe range for this surface.
    val surfaceAlpha=if(settings.reduceTransparency)1f else
        ((if(dark).80f else .86f)*settings.glassOpacity).coerceIn(if(dark).74f else .78f,.94f)
    val rowHeight = if (density.fontScale > 1.2f) (32f * density.fontScale + 16f).dp else 48.dp
    val highlightY by animateFloatAsState(
        with(density) { (rowHeight + .5.dp).toPx() } * (hovered ?: 0),
        motion.spring(.62f, 560f), label = "menuHighlightPosition"
    )
    var previousHovered by remember(request.id) { mutableStateOf<Int?>(null) }
    LaunchedEffect(hovered) {
        if (hovered != null && hovered != previousHovered && !motion.reduced) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        previousHovered = hovered
    }
    val firstItemFocus = remember(request.id) { FocusRequester() }
    val focusIndex = request.items.indexOfFirst { it.selected }.coerceAtLeast(0)
    LaunchedEffect(active?.id) {
        if (active != null) firstItemFocus.requestFocus()
    }
    BackHandler(active != null) { state.dismissMenu() }
    fun choose(index: Int) {
        if (state.currentRequest?.id != currentRequest.id) return
        val item = currentRequest.items.getOrNull(index) ?: return
        item.onClick()
        state.dismissMenu(currentRequest.id)
    }
    BoxWithConstraints(modifier.fillMaxSize().safeDrawingPadding().onGloballyPositioned { hostOrigin = it.positionInRoot() }) {
        val widest=remember(request.items.map {it.label},density.fontScale,menuTextStyle){request.items.maxOfOrNull {textMeasurer.measure(androidx.compose.ui.text.AnnotatedString(it.label),menuTextStyle).size.width} ?: 0}
        // 14 outer + 24 inner padding, 18 check, 18 icon, 20 gaps; leave room for selected weight.
        val menuWidth = minOf(maxOf(with(density){widest.toDp()}+104.dp,with(density){request.anchorBounds.width.toDp()},140.dp), maxWidth - 28.dp)
        val fullMenuHeight = rowHeight * request.items.size + .5.dp * (request.items.size - 1) + 14.dp
        val menuHeight = minOf(fullMenuHeight, maxHeight - 28.dp).coerceAtLeast(1.dp)
        val scrollable = menuHeight < fullMenuHeight
        val width = with(density) { menuWidth.roundToPx() }
        val height = with(density) { menuHeight.roundToPx() }
        val margin = with(density) { 14.dp.roundToPx() }
        val source = request.anchorBounds
        val anchor = IntRect(source.left - hostOrigin.x.roundToInt(), source.top - hostOrigin.y.roundToInt(),
            source.right - hostOrigin.x.roundToInt(), source.bottom - hostOrigin.y.roundToInt())
        val right = anchor.center.x > constraints.maxWidth / 2
        val down = constraints.maxHeight - anchor.top - with(density) { 92.dp.roundToPx() } >= height || anchor.top < height
        val left = (if (right) anchor.right - width else anchor.left).coerceIn(margin, (constraints.maxWidth - width - margin).coerceAtLeast(margin))
        val top = (if (down) anchor.top else anchor.bottom - height).coerceIn(margin, (constraints.maxHeight - height - margin).coerceAtLeast(margin))
        if (active != null) Box(Modifier.fillMaxSize().pointerInput(request.id) {
            awaitEachGesture { awaitFirstDown(false); state.dismissMenu() }
        })
        val safeAnchorW = anchor.width.coerceAtLeast(with(density) { 36.dp.roundToPx() }).toFloat()
        val safeAnchorH = anchor.height.coerceAtLeast(with(density) { 36.dp.roundToPx() }).toFloat()
        val anchorCornerDp = with(density) { (minOf(safeAnchorW, safeAnchorH) * 0.5f).toDp() }
        Box(
            Modifier
                .offset { IntOffset(left, top) }
                .size(menuWidth, menuHeight)
                .testTag("retained_glass_menu")
                .then(if (!scrollable && active != null) Modifier.pointerInput(request.id, rowHeight) {
                    awaitEachGesture {
                        val first = awaitFirstDown(pass = PointerEventPass.Initial)
                        val rowPx = rowHeight.toPx()
                        val stride = (rowHeight + .5.dp).toPx()
                        fun row(position: Offset): Int? {
                            val yPos = position.y - 7.dp.toPx()
                            if (position.x < 0f || position.x >= size.width || yPos < 0f) return null
                            val index = (yPos / stride).toInt()
                            return index.takeIf { it in currentRequest.items.indices && yPos - it * stride < rowPx }
                        }
                        hovered = row(first.position)
                        first.consume()
                        try {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == first.id } ?: break
                                if (change.isConsumed || event.changes.any { it.id != first.id && it.pressed }) break
                                drag = change.position - first.position
                                hovered = row(change.position)
                                change.consume()
                                if (!change.pressed) {
                                    hovered?.let(::choose)
                                    break
                                }
                            }
                        } finally { hovered = null; drag = Offset.Zero }
                    }
                } else Modifier)
        ) {
            // 1. Liquid Glass Droplet Shell: morphs smoothly from the circular/capsule trigger into a 26.dp G2 card
            // with volume-preserving jelly overshoot and tanh finger-drag elasticity (never squashing text glyphs).
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val xv = x.value
                        val yv = y.value
                        val clamped = ((xv + yv) * 0.5f).coerceIn(0f, 1f)
                        val baseRatioX = (safeAnchorW / width.coerceAtLeast(1)).coerceIn(0.08f, 1f)
                        val baseRatioY = (safeAnchorH / height.coerceAtLeast(1)).coerceIn(0.08f, 1f)
                        val overY = (yv - 1f).coerceAtLeast(0f)
                        val overX = (xv - 1f).coerceAtLeast(0f)
                        val dragStretchX = if (motion.reduced) 0f else (abs(drag.x).coerceAtMost(220f) / 220f) * 0.045f
                        val dragStretchY = if (motion.reduced) 0f else (abs(drag.y).coerceAtMost(220f) / 220f) * 0.045f
                        val rubberX = if (motion.reduced) 0f else 14f * tanh(drag.x / 160f)
                        val rubberY = if (motion.reduced) 0f else 14f * tanh(drag.y / 160f)

                        transformOrigin = TransformOrigin(if (right) 1f else 0f, if (down) 0f else 1f)
                        // Volume-conserving jelly deformation: vertical overshoot subtly narrows horizontal waist
                        scaleX = (baseRatioX + (1f - baseRatioX) * xv - overY * 0.14f + dragStretchX - dragStretchY * 0.30f).coerceAtLeast(0.06f)
                        scaleY = (baseRatioY + (1f - baseRatioY) * yv - overX * 0.10f + dragStretchY - dragStretchX * 0.30f).coerceAtLeast(0.06f)
                        translationX = (if (right) anchor.right - left - width else anchor.left - left) * (1f - clamped) + rubberX
                        translationY = (if (down) anchor.top - top else anchor.bottom - top - height) * (1f - clamped) + rubberY
                        alpha = (minOf(xv, yv) * 4.2f).coerceIn(0f, 1f)
                    }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = {
                            val clamped = ((x.value + y.value) * 0.5f).coerceIn(0f, 1f)
                            val radiusDp = androidx.compose.ui.unit.lerp(anchorCornerDp, 26.dp, clamped)
                            ContinuousRoundedRectangle(radiusDp)
                        },
                        highlight = {
                            if (settings.reduceTransparency) null
                            else adaptiveGlassHighlight(
                                isLight = !dark,
                                alpha = if (dark) 0.58f else 0.78f
                            )
                        },
                        shadow = { Shadow(radius = 20.dp, alpha = 0.24f) },
                        innerShadow = {
                            if (settings.reduceTransparency) null else InnerShadow(radius = 5.dp, alpha = if (dark) 0.26f else 0.16f)
                        },
                        effects = {
                            if (!settings.reduceTransparency) {
                                appleGlassColorControls(
                                    isLight = !dark,
                                    isFrosted = settings.glassVariant == "frosted",
                                    reduceTransparency = settings.reduceTransparency
                                )
                                blur((8.5f * settings.glassBlurIntensity).coerceIn(4f, 22f).dp.toPx())
                            }
                            if (!settings.reduceTransparency &&
                                settings.enableLensRefraction &&
                                this.size.isSpecified &&
                                this.size.width > 4f &&
                                this.size.height > 4f
                            ) {
                                val clamped = ((x.value + y.value) * 0.5f).coerceIn(0f, 1f)
                                val morphPulse = 1f + 0.28f * sin(clamped * Math.PI).toFloat()
                                val maxH = min(this.size.width, this.size.height) * 0.46f
                                val rHeight = (24f * settings.glassIntensity).dp.toPx().coerceAtMost(maxH)
                                val rAmount = (42f * settings.glassIntensity * morphPulse).dp.toPx()
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
                        onDrawSurface = { drawRect((if (dark) Color(0xFF16161D) else Color.White).copy(alpha = surfaceAlpha)) }
                    )
            )

            // 2. Crisp Menu Content Layer: uniform aspect-preserving scale + progressive droplet clip (zero text distortion!)
            Box(
                Modifier
                    .matchParentSize()
                    .drawWithContent {
                        val xv = x.value.coerceIn(0f, 1.15f)
                        val yv = y.value.coerceIn(0f, 1.15f)
                        if (xv >= 0.99f && yv >= 0.99f) {
                            drawContent()
                        } else {
                            val wClip = (size.width * xv.coerceIn(0f, 1f))
                            val hClip = (size.height * yv.coerceIn(0f, 1f))
                            val cLeft = if (right) size.width - wClip else 0f
                            val cTop = if (down) 0f else size.height - hClip
                            clipRect(left = cLeft, top = cTop, right = cLeft + wClip, bottom = cTop + hClip) {
                                this@drawWithContent.drawContent()
                            }
                        }
                    }
                    .graphicsLayer {
                        val contentProgress = (((x.value + y.value) * 0.5f - 0.16f) / 0.84f).coerceIn(0f, 1f)
                        val uniformScale = if (motion.reduced) 1f else (0.90f + 0.10f * contentProgress)
                        val rubberX = if (motion.reduced) 0f else 14f * tanh(drag.x / 160f)
                        val rubberY = if (motion.reduced) 0f else 14f * tanh(drag.y / 160f)
                        transformOrigin = TransformOrigin(if (right) 1f else 0f, if (down) 0f else 1f)
                        scaleX = uniformScale
                        scaleY = uniformScale
                        translationX = rubberX
                        translationY = (if (motion.reduced) 0f else (if (down) -8.dp.toPx() else 8.dp.toPx()) * (1f - contentProgress)) + rubberY
                        alpha = if (motion.reduced) 1f else (contentProgress * 1.25f).coerceIn(0f, 1f)
                    }
            ) {
                if (!scrollable && hovered != null) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(rowHeight)
                            .padding(horizontal = 7.dp)
                            .graphicsLayer { translationY = 7.dp.toPx() + highlightY }
                            .background(
                                if (dark) Color.White.copy(alpha = .15f) else Color.Black.copy(alpha = .085f),
                                ContinuousRoundedRectangle(19.dp)
                            )
                    )
                }
                val scrollModifier = if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier
                Column(scrollModifier.padding(vertical = 7.dp)) {
                    request.items.forEachIndexed { index, item ->
                        Row(Modifier.fillMaxWidth().height(rowHeight).padding(horizontal = 7.dp)
                            .clip(ContinuousRoundedRectangle(19.dp))
                            .background(when {
                                item.selected && hovered == null -> palette.accent.copy(alpha = .10f)
                                else -> Color.Transparent
                            })
                            .then(if (active != null && index == focusIndex) Modifier.focusRequester(firstItemFocus) else Modifier)
                            .semantics { if (request.selectionMenu) selected = item.selected }
                            .clickable(
                                enabled = active != null,
                                onClickLabel = if (request.selectionMenu) "Selecionar ${item.label}" else item.label,
                                role = if (request.selectionMenu) Role.RadioButton else Role.Button,
                                onClick = { choose(index) }
                            )
                            .padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (request.selectionMenu) Box(Modifier.width(18.dp)) {
                                if (item.selected) Icon(Icons.Default.Check, null, tint = palette.accent, modifier = Modifier.size(18.dp))
                            }
                            Text(item.label, modifier = Modifier.weight(1f), fontSize = 14.sp, lineHeight = 20.sp,
                                style = menuTextStyle,
                                fontWeight = if (item.selected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Medium,
                                color = if (item.selected) palette.accent else palette.textPrimary, maxLines = 2)
                            item.icon?.let { Icon(it, null, tint = palette.textSecondary, modifier = Modifier.size(18.dp)) }
                        }
                        if (index < request.items.lastIndex) Box(Modifier.fillMaxWidth().padding(horizontal = 13.dp)
                            .height(.5.dp).testTag("menu-divider-$index")
                            .background(if (dark) Color.White.copy(alpha = .11f) else Color.Black.copy(alpha = .08f)))
                    }
                }
            }
        }
    }
}
