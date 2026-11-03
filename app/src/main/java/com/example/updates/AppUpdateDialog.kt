package com.example.updates

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.*

private val LocalUpdatePrimary = staticCompositionLocalOf { false }

@Composable
internal fun AppUpdateDialog(onDismissRequest: () -> Unit, containerColor: Color,
    title: @Composable () -> Unit, text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit, dismissButton: @Composable () -> Unit) {
    val theme = LocalIOS27Theme.current
    Dialog(onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.padding(24.dp).widthIn(max = 420.dp).fillMaxWidth()
            .clip(RoundedCornerShape(28.dp)).background(theme.bgSecondary)
            .border(0.75.dp, theme.divider, RoundedCornerShape(28.dp))
            .verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(42.dp))
                Text("QUADRINHEIRO", style = MaterialTheme.typography.labelMedium, color = theme.textSecondary)
            }
            CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.headlineMedium.copy(color = theme.textPrimary)) { title() }
            CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 21.sp)) { text() }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CompositionLocalProvider(LocalUpdatePrimary provides true) { confirmButton() }
                dismissButton()
            }
        }
    }
}

@Composable
internal fun UpdateDialogAction(onClick: () -> Unit, enabled: Boolean = true, content: @Composable () -> Unit) {
    val theme = LocalIOS27Theme.current
    val primary = LocalUpdatePrimary.current
    val background = if (primary) theme.accent else theme.bgTertiary
    val foreground = if (primary) {
        if (contrastRatio(Color.White, background) >= contrastRatio(Color.Black, background)) Color.White else Color.Black
    } else readableForeground(theme.textPrimary, background)
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).graphicsLayer {
        scaleX = if (pressed) 0.98f else 1f; scaleY = scaleX; alpha = if (enabled) 1f else 0.45f
    }.clip(RoundedCornerShape(24.dp)).background(background)
        .clickable(interactions, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(horizontal = 18.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
        CompositionLocalProvider(LocalContentColor provides foreground,
            LocalTextStyle provides MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)) { content() }
    }
}
