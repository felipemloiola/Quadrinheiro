package com.example.updates

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.example.R
import com.example.ui.components.LiquidGlassDialog
import com.example.ui.components.liquidGlassNavSurface
import com.example.ui.components.pressScaleFeedback
import com.example.ui.theme.*
import com.kyant.shapes.ContinuousCapsule

private val LocalUpdatePrimary = staticCompositionLocalOf { false }

@Suppress("UNUSED_PARAMETER")
@Composable
internal fun AppUpdateDialog(
    onDismissRequest: () -> Unit,
    containerColor: Color,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit
) {
    val theme = LocalIOS27Theme.current
    LiquidGlassDialog(
        onDismissRequest = onDismissRequest,
        maxWidth = 420.dp
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
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
    val settings = LocalAppSettings.current
    val primary = LocalUpdatePrimary.current
    val background = if (primary) theme.accent else theme.bgTertiary
    val foreground = if (primary) {
        if (contrastRatio(Color.White, background) >= contrastRatio(Color.Black, background)) Color.White else Color.Black
    } else readableForeground(theme.textPrimary, theme.bgSecondary)
    val interactions = remember { MutableInteractionSource() }
    val shape = ContinuousCapsule
    val surfaceModifier = when {
        primary -> Modifier
            .clip(shape)
            .background(background, shape)
            .border(0.75.dp, Color.White.copy(alpha = if (theme.isDark) 0.24f else 0.34f), shape)
        settings.reduceTransparency -> Modifier
            .clip(shape)
            .background(background, shape)
            .border(0.85.dp, theme.divider, shape)
        else -> Modifier
            .clip(shape)
            .liquidGlassNavSurface(shape)
    }

    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .pressScaleFeedback(interactions, targetScale = 0.97f)
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .then(surfaceModifier)
            .clickable(interactions, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            LocalContentColor provides foreground,
            LocalTextStyle provides MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
        ) { content() }
    }
}
