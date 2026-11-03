package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.ContinuousRoundedRectangle
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** The menu is measured once at its final size. Springs only transform its render layer. */
@Composable
internal fun RetainedGlassMenu(state: LiquidGlassMenuHostState, backdrop: Backdrop, modifier: Modifier) {
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
            launch { x.animateTo(1f, motion.spring(0.85f, 700f)) }
            y.animateTo(1f, motion.spring(0.88f, 650f))
        } else if (retained != null) {
            launch { x.animateTo(0f, motion.spring(1f, 850f)) }
            y.animateTo(0f, motion.spring(1f, 850f))
            if (state.currentRequest == null) { retained = null; state.morphingMenuId = null }
        }
    }
    val request = active ?: retained ?: return
    if (request.items.isEmpty()) return
    val currentRequest by rememberUpdatedState(request)
    val density = LocalDensity.current
    val palette = LocalIOS27Theme.current
    val settings = LocalAppSettings.current
    val rowHeight = if (density.fontScale > 1.2f) (44f * density.fontScale + 16f).dp else 48.dp
    BackHandler(active != null) { state.dismissMenu() }
    fun choose(index: Int) {
        val item = currentRequest.items.getOrNull(index) ?: return
        item.onClick()
        state.dismissMenu(currentRequest.id)
    }
    BoxWithConstraints(modifier.fillMaxSize().onGloballyPositioned { hostOrigin = it.positionInRoot() }) {
        val menuWidth = minOf(260.dp, maxWidth - 28.dp)
        val menuHeight = minOf(rowHeight * request.items.size + 14.dp, maxHeight - 28.dp)
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
        Box(Modifier.offset { IntOffset(left, top) }.size(menuWidth, menuHeight)
            .graphicsLayer {
                transformOrigin = TransformOrigin(if (right) 1f else 0f, if (down) 0f else 1f)
                scaleX = (anchor.width.toFloat() / width + (1f - anchor.width.toFloat() / width) * x.value).coerceAtLeast(0.05f)
                scaleY = (anchor.height.toFloat() / height + (1f - anchor.height.toFloat() / height) * y.value).coerceAtLeast(0.05f)
                translationX = ((if (right) anchor.right - left - width else anchor.left - left) * (1f - x.value)) + drag.x * 0.035f
                translationY = ((if (down) anchor.top - top else anchor.bottom - top - height) * (1f - y.value)) + drag.y * 0.035f
                alpha = (minOf(x.value, y.value) * 5f).coerceIn(0f, 1f)
            }
            .drawBackdrop(backdrop, shape = { ContinuousRoundedRectangle(26.dp) },
                shadow = { Shadow(radius = 18.dp, alpha = 0.20f) },
                effects = {
                    if (!settings.reduceTransparency) { vibrancy(); blur(12.dp.toPx()) }
                    if (!settings.reduceTransparency && settings.enableLensRefraction) lens(12.dp.toPx(), 18.dp.toPx())
                },
                onDrawSurface = { drawRect(palette.bgSecondary.copy(alpha = if (settings.reduceTransparency) 1f else 0.94f)) })
            .clip(RoundedCornerShape(26.dp))
            .pointerInput(request.id) {
                awaitEachGesture {
                    val first = awaitFirstDown(false)
                    fun row(position: Offset): Int? = if (position.x in 0f..size.width.toFloat() && position.y in 0f..size.height.toFloat())
                        ((position.y - 7.dp.toPx()) / rowHeight.toPx()).toInt().coerceIn(0, currentRequest.items.lastIndex) else null
                    hovered = row(first.position)
                    var released = false
                    try {
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == first.id } ?: break
                            if (!change.pressed) { released = true; break }
                            row(change.position)?.let { hovered = it }
                            drag = change.position - first.position
                            change.consume()
                        }
                        if (released) hovered?.let { choose(it) }
                    } finally { hovered = null; drag = Offset.Zero }
                }
            }.padding(vertical = 7.dp)) {
            Column {
                request.items.forEachIndexed { index, item ->
                    Row(Modifier.fillMaxWidth().height(rowHeight).padding(horizontal = 7.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (hovered == index) palette.accent.copy(alpha = 0.16f) else Color.Transparent)
                        .semantics(mergeDescendants = true) {
                            selected = item.selected; role = Role.RadioButton
                            onClick("Selecionar ${item.label}") { choose(index); true }
                        }.focusable().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.width(18.dp)) { if (item.selected) Icon(Icons.Default.Check, null, tint = palette.accent, modifier = Modifier.size(18.dp)) }
                        Text(item.label, modifier = Modifier.weight(1f), fontSize = 17.sp, lineHeight = 22.sp,
                            color = palette.textPrimary, maxLines = 2)
                        item.icon?.let { Icon(it, null, tint = palette.textSecondary, modifier = Modifier.size(18.dp)) }
                    }
                }
            }
        }
    }
}
