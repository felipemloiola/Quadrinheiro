package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/** A small visual capsule inside an independent, accessible touch target. */
@Composable
fun CompactReadingAction(label: String, prominent: Boolean = false, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val theme = LocalIOS27Theme.current
    val motion = LocalAppMotion.current
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, motion.spring(1f, 800f), label = "catalogActionPress")
    val surface = if (prominent) theme.accent else theme.bgTertiary
    val foreground = if (prominent) {
        if (contrastRatio(Color.White, surface) > contrastRatio(Color.Black, surface)) Color.White else Color.Black
    } else theme.textPrimary
    Box(modifier.heightIn(min = 48.dp).widthIn(min = 44.dp)
        .clickable(interactions, indication = null, role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Box(Modifier.graphicsLayer { scaleX = scale; scaleY = scale }.clip(RoundedCornerShape(16.dp))
            .background(surface).heightIn(min = 30.dp).testTag("catalog-action-face-$label")
            .padding(horizontal = 9.dp, vertical = 5.dp), contentAlignment = Alignment.Center) {
            Text(label, color = foreground, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
