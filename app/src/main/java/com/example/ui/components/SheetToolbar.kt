package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalIOS27Theme

/** Equal touch targets and one baseline; content scrolls in a separate clipped region. */
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
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(44.dp).testTag(closeTag).clickable(role = Role.Button, onClickLabel = "Fechar", onClick = onClose), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Close, "Fechar", tint = theme.textSecondary, modifier = Modifier.size(21.dp))
        }
        Text(title, Modifier.weight(1f).padding(horizontal = 8.dp), color = theme.textPrimary, fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(Modifier.size(44.dp).testTag(shareTag).clickable(role = Role.Button, onClickLabel = "Compartilhar", onClick = onShare), contentAlignment = Alignment.Center) {
            Icon(LucideIcons.ShareIos, "Compartilhar", tint = theme.accent, modifier = Modifier.size(21.dp))
        }
        onSave?.let { save ->
            Box(Modifier.size(44.dp).testTag("modal_header_save_button").clickable(role = Role.Button, onClickLabel = "Salvar", onClick = save), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, "Salvar", tint = theme.accent, modifier = Modifier.size(23.dp))
            }
        }
    }
}
