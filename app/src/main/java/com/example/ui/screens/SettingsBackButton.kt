package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.iosLiquidGlassInteractiveSurface
import com.example.ui.components.rememberAppHaptics
import com.example.ui.theme.LocalAppMotion
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.theme.readableForeground
import com.kyant.shapes.ContinuousCapsule
import kotlin.math.roundToInt

@Composable
internal fun backButtonScrollProgress(
    firstVisibleItemIndex: () -> Int,
    firstVisibleItemScrollOffset: () -> Int
): () -> Float {
    val collapseDistance = with(LocalDensity.current) { 48.dp.toPx() }
    return remember(firstVisibleItemIndex, firstVisibleItemScrollOffset, collapseDistance) {
        {
            if (firstVisibleItemIndex() > 0) 1f
            else (firstVisibleItemScrollOffset() / collapseDistance).coerceIn(0f, 1f)
        }
    }
}

/**
 * Standardized iOS 26 Liquid Glass back button with Tab-Bar optical refraction,
 * balanced jelly spring press physics, 48dp touch target, and reduced-transparency fallback.
 */
@Composable
internal fun SettingsBackButton(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    collapsed: Boolean = false,
    label: String = "Voltar",
    testTag: String = "settings_back_button",
    collapseProgress: (() -> Float)? = null
) {
    val motion = LocalAppMotion.current
    val theme = LocalIOS27Theme.current
    val haptics = rememberAppHaptics()
    val interactions = remember { MutableInteractionSource() }
    val accessibleLabel = if (label.equals("Voltar", ignoreCase = true)) "Voltar" else "Voltar para $label"
    val fg = readableForeground(theme.textPrimary, theme.bgSecondary, 4.5f)
    val shape = ContinuousCapsule
    val animatedCollapse = animateFloatAsState(
        targetValue = if (collapsed) 1f else 0f,
        animationSpec = motion.tween(if (collapsed) 150 else 200),
        label = "backButtonCollapse"
    )
    val expansion = {
        val progress = collapseProgress?.invoke() ?: animatedCollapse.value
        if (motion.reduced) {
            if (progress >= 0.5f) 0f else 1f
        } else {
            1f - progress
        }
    }

    Row(
        modifier
            .heightIn(min = 48.dp)
            .widthIn(min = 48.dp)
            .iosLiquidGlassInteractiveSurface(
                shape = shape,
                interactionSource = interactions,
                elevation = 8.dp,
                maxJellyScaleBoostDp = 5f
            )
            .clip(shape)
            .focusable()
            .semantics(mergeDescendants = true) {
                contentDescription = accessibleLabel
            }
            .testTag(testTag)
            .clickable(
                interactionSource = interactions,
                indication = null,
                role = Role.Button,
                onClickLabel = accessibleLabel,
                onClick = {
                    haptics.impactLight()
                    onBack()
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(23.dp)
            )
        }
        Text(
            text = label,
            color = fg,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clipToBounds()
                .layout { measurable, constraints ->
                    val text = measurable.measure(constraints.copy(minWidth = 0))
                    // Only the label's reserved width changes. The arrow and touch target stay fixed.
                    layout((text.width * expansion()).roundToInt(), text.height) {
                        text.placeRelative(0, 0)
                    }
                }
                .graphicsLayer { alpha = expansion() }
                .clearAndSetSemantics {}
                .padding(end = 14.dp)
        )
    }
}
