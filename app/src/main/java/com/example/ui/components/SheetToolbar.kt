package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAppSettings
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.theme.readableForeground

/** Equal 48dp touch targets with optical glass controls and solid reduced-transparency fallback. */
@Composable
internal fun SheetToolbar(
    title: String,
    closeTag: String,
    shareTag: String,
    onClose: () -> Unit,
    onShare: () -> Unit,
    onSave: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val theme = LocalIOS27Theme.current
    val settings = LocalAppSettings.current
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SheetToolbarActionButton(
            icon = Icons.Default.Close,
            label = "Fechar",
            tint = readableForeground(theme.textPrimary, theme.bgSecondary, 4.5f),
            tag = closeTag,
            reduceTransparency = settings.reduceTransparency,
            onClick = onClose
        )
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
                .semantics { heading() },
            color = theme.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        SheetToolbarActionButton(
            icon = LucideIcons.ShareIos,
            label = "Compartilhar",
            tint = readableForeground(theme.accent, theme.bgSecondary, 4.5f),
            tag = shareTag,
            reduceTransparency = settings.reduceTransparency,
            onClick = onShare
        )
        onSave?.let { save ->
            SheetToolbarActionButton(
                icon = Icons.Default.Check,
                label = "Salvar",
                tint = readableForeground(theme.accent, theme.bgSecondary, 4.5f),
                tag = "modal_header_save_button",
                reduceTransparency = settings.reduceTransparency,
                onClick = save
            )
        }
    }
}

@Composable
internal fun SheetToolbarActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    tag: String,
    reduceTransparency: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassCircleButton(
        icon = icon,
        contentDescription = label,
        onClick = onClick,
        modifier = modifier,
        size = 48.dp,
        iconSize = 20.dp,
        tint = tint,
        tag = tag
    )
}
