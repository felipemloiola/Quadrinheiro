package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalAppMotion
import kotlin.math.roundToInt

/** Circular glass control that expands into the root menu from its own bounds. */
@Composable
fun <T> AppleIconMenuButton(
    options: List<SegmentOption<T>>,
    value: T,
    onChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = LucideIcons.OverflowDots,
    contentDescription: String = "Opções de ordenação",
    buttonSize: Dp = 48.dp,
    iconSize: Dp = 22.dp
) {
    val host = LocalLiquidGlassMenuHost.current
    val motion = LocalAppMotion.current
    val menuId = remember { "apple_icon_menu_${System.nanoTime()}" }
    val active = host?.currentRequest?.id == menuId
    val selection = options.firstOrNull { it.value == value } ?: options.firstOrNull()
    val currentOnChange by rememberUpdatedState(onChange)
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val items = remember(options, value) {
        options.map { option ->
            LiquidGlassMenuItemData(
                label = option.label,
                icon = option.icon,
                selected = option.value == value,
                onClick = { currentOnChange(option.value) }
            )
        }
    }

    fun anchorBounds(coords: LayoutCoordinates): IntRect {
        val position = coords.positionInRoot()
        val left = position.x.roundToInt()
        val top = position.y.roundToInt()
        return IntRect(left, top, left + coords.size.width, top + coords.size.height)
    }

    DisposableEffect(menuId, host) {
        onDispose { host?.dismissMenu(menuId) }
    }

    LiquidGlassCircleButton(
        icon = icon,
        contentDescription = selection?.let { "$contentDescription: ${it.label}" } ?: contentDescription,
        onClick = {
            val anchor = coordinates?.takeIf { it.isAttached }?.let(::anchorBounds)
            if (host != null && anchor != null) {
                if (host.currentRequest?.id == menuId) {
                    host.dismissMenu(menuId)
                } else {
                    host.showMenu(
                        LiquidGlassMenuRequest(
                            id = menuId,
                            anchorBounds = anchor,
                            triggerLabel = contentDescription,
                            triggerIcon = icon,
                            items = items,
                            onDismiss = {}
                        )
                    )
                }
            }
        },
        modifier = modifier
            .onGloballyPositioned { coords ->
                coordinates = coords
                host?.updateAnchorBounds(menuId, anchorBounds(coords))
            }
            .semantics { if (active) stateDescription = "Menu aberto" }
            .graphicsLayer {
                val progress = host?.progressForMenu(menuId) ?: 0f
                alpha = if (motion.reduced) {
                    if (active) 0f else 1f
                } else {
                    (1f - progress / 0.4f).coerceIn(0f, 1f)
                }
            },
        size = buttonSize,
        iconSize = iconSize,
        enabled = host != null && items.isNotEmpty()
    )
}
