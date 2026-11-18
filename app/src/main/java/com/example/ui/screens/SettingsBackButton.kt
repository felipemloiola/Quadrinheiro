package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.ui.components.liquidGlassNavSurface
import com.example.ui.theme.LocalAppMotion
import com.example.ui.theme.LocalIOS27Theme

/** Navigation stays available during scrolling and always keeps the same shape. */
@Composable
internal fun SettingsBackButton(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    collapsed: Boolean = false,
    label: String = "Voltar"
) {
    val motion = LocalAppMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (isPressed && !motion.reduced) 1f else 0f,
        animationSpec = if (motion.reduced) motion.tween(0) else motion.spring(dampingRatio = 0.54f, stiffness = 460f),
        label = "backButtonJelly"
    )
    Row(
        modifier
            .height(44.dp)
            .testTag("settings_back_button")
            .graphicsLayer {
                if (!motion.reduced && pressProgress > 0.001f) {
                    val s = lerp(1f, 1.06f, pressProgress)
                    scaleX = s
                    scaleY = s
                }
            }
            .liquidGlassNavSurface(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onBack
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            "Voltar",
            tint = LocalIOS27Theme.current.textPrimary,
            modifier = Modifier.size(23.dp)
        )
        AnimatedVisibility(
            !collapsed,
            enter = expandHorizontally(motion.spring(1f, 700f)) + fadeIn(motion.tween(180)),
            exit = shrinkHorizontally(motion.spring(1f, 700f)) + fadeOut(motion.tween(120))
        ) {
            Text(
                label,
                color = LocalIOS27Theme.current.textPrimary,
                fontSize = 15.sp,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(end = 6.dp)
            )
        }
    }
}
