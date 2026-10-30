package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.liquidGlass
import com.example.ui.theme.LocalIOS27Theme

/** Uses the app's Kyant Backdrop pipeline; a single clickable drives the spring. */
@Composable
private fun AuthCircleAction(
    label: String,
    tag: String,
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 1.075f else 1f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 420f),
        label = "authCirclePress"
    )
    Box(
        modifier = Modifier
            .size(54.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.45f }
            .liquidGlass(shape = CircleShape, elevation = 4.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, interactionSource = interactions, indication = null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
internal fun AuthAlternativeActions(
    enabled: Boolean,
    showGuest: Boolean,
    onGoogle: () -> Unit,
    onGuest: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AuthCircleAction("Continuar com Google", "auth_google_button", enabled, onGoogle) {
            Image(painterResource(R.drawable.ic_google_g), contentDescription = null, modifier = Modifier.size(24.dp))
        }
        if (showGuest) AuthCircleAction("Explorar como convidado", "auth_guest_button", enabled, onGuest) {
            Icon(Icons.Outlined.PersonOutline, contentDescription = null, tint = LocalIOS27Theme.current.textPrimary, modifier = Modifier.size(25.dp))
        }
    }
}

@Composable
internal fun AuthGlassEntrance(content: @Composable () -> Unit) {
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visible,
        enter = fadeIn(tween(260)) + scaleIn(initialScale = 0.965f, animationSpec = spring(dampingRatio = 0.88f, stiffness = 300f))
    ) { content() }
}
