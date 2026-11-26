package com.example.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
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
    testTag: String = "settings_back_button"
) {
    val motion = LocalAppMotion.current
    val theme = LocalIOS27Theme.current
    val haptics = rememberAppHaptics()
    val interactions = remember { MutableInteractionSource() }
    val accessibleLabel = if (label.equals("Voltar", ignoreCase = true)) "Voltar" else "Voltar para $label"
    val fg = readableForeground(theme.textPrimary, theme.bgSecondary, 4.5f)
    val shape = ContinuousCapsule

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
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(23.dp)
        )
        AnimatedVisibility(
            visible = !collapsed,
            enter = expandHorizontally(motion.spring(0.82f, 520f)) + fadeIn(motion.tween(180)),
            exit = shrinkHorizontally(motion.spring(0.88f, 560f)) + fadeOut(motion.tween(120))
        ) {
            Text(
                text = label,
                color = fg,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 6.dp)
            )
        }
    }
}
