package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
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
    val textMeasurer=androidx.compose.ui.text.rememberTextMeasurer()
    val menuTextStyle=androidx.compose.material3.LocalTextStyle.current.copy(fontSize=14.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Medium)
    val motion by rememberUpdatedState(LocalAppMotion.current)
    val active = state.currentRequest
    var retained by remember { mutableStateOf<LiquidGlassMenuRequest?>(null) }
    val x = remember { Animatable(0f) }
    val y = remember { Animatable(0f) }
    var hovered by remember { mutableStateOf<Int?>(null) }
    var hostOrigin by remember { mutableStateOf(Offset.Zero) }
    DisposableEffect(state) {
        state.menuProgressProvider = { ((x.value + y.value) * 0.5f).coerceIn(0f, 1f) }
        onDispose { state.menuProgressProvider = null }
    }
    SideEffect { if (active != null) retained = active }
    LaunchedEffect(active?.id) {
        hovered = null
        if (active != null) {
            retained = active
            state.morphingMenuId = active.id
            launch { x.animateTo(1f, motion.spring(0.58f, 460f)) }
            y.animateTo(1f, motion.spring(0.62f, 420f))
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
    val dark = LocalIsDarkTheme.current
    // Text-bearing menus need a regular material even over white/black artwork.
    // Keep the opacity preference within a contrast-safe range for this surface.
    val surfaceAlpha=if(settings.reduceTransparency)1f else
        ((if(dark).80f else .86f)*settings.glassOpacity).coerceIn(if(dark).74f else .78f,.94f)
    val rowHeight = if (density.fontScale > 1.2f) (32f * density.fontScale + 16f).dp else 48.dp
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
        Box(Modifier.offset { IntOffset(left, top) }.size(menuWidth, menuHeight).testTag("retained_glass_menu")
            .graphicsLayer {
                transformOrigin = TransformOrigin(if (right) 1f else 0f, if (down) 0f else 1f)
                scaleX = (anchor.width.toFloat() / width + (1f - anchor.width.toFloat() / width) * x.value).coerceAtLeast(0.05f)
                scaleY = (anchor.height.toFloat() / height + (1f - anchor.height.toFloat() / height) * y.value).coerceAtLeast(0.05f)
                translationX = (if (right) anchor.right - left - width else anchor.left - left) * (1f - x.value)
                translationY = (if (down) anchor.top - top else anchor.bottom - top - height) * (1f - y.value)
                alpha = (minOf(x.value, y.value) * 5f).coerceIn(0f, 1f)
            }
            .drawBackdrop(backdrop, shape = { ContinuousRoundedRectangle(26.dp) },
                shadow = { Shadow(radius = 18.dp, alpha = 0.20f) },
                effects = {
                    if (!settings.reduceTransparency) { vibrancy(); blur((5.5f * settings.glassBlurIntensity).coerceIn(3f, 20f).dp.toPx()) }
                    if (!settings.reduceTransparency && settings.enableLensRefraction) lens((24f * settings.glassIntensity).dp.toPx(), (42f * settings.glassIntensity).dp.toPx(), depthEffect = true, chromaticAberration = settings.enableChromaticAberration)
                },
                onDrawSurface = { drawRect((if (dark) Color(0xFF16161D) else Color.White).copy(alpha = surfaceAlpha)) })
            .clip(RoundedCornerShape(26.dp))
            .then(if (!scrollable && active != null) Modifier.pointerInput(request.id, rowHeight) {
                awaitEachGesture {
                    val first = awaitFirstDown(pass = PointerEventPass.Initial)
                    val rowPx = rowHeight.toPx()
                    val stride = (rowHeight + .5.dp).toPx()
                    fun row(position: Offset): Int? {
                        val y = position.y - 7.dp.toPx()
                        if (position.x < 0f || position.x >= size.width || y < 0f) return null
                        val index = (y / stride).toInt()
                        return index.takeIf { it in currentRequest.items.indices && y - it * stride < rowPx }
                    }
                    hovered = row(first.position)
                    first.consume()
                    try {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == first.id } ?: break
                            if (change.isConsumed || event.changes.any { it.id != first.id && it.pressed }) break
                            hovered = row(change.position)
                            change.consume()
                            if (!change.pressed) {
                                hovered?.let(::choose)
                                break
                            }
                        }
                    } finally { hovered = null }
                }
            } else Modifier)) {
            val scrollModifier = if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier
            Column(scrollModifier.padding(vertical = 7.dp)) {
                request.items.forEachIndexed { index, item ->
                    Row(Modifier.fillMaxWidth().height(rowHeight).padding(horizontal = 7.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(when {
                            hovered == index -> if (dark) Color.White.copy(alpha = .13f) else Color.Black.copy(alpha = .075f)
                            item.selected && hovered == null -> palette.accent.copy(alpha = .09f)
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
                        Box(Modifier.width(18.dp)) { if (item.selected) Icon(Icons.Default.Check, null, tint = palette.accent, modifier = Modifier.size(18.dp)) }
                        Text(item.label, modifier = Modifier.weight(1f), fontSize = 14.sp, lineHeight = 20.sp,
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
