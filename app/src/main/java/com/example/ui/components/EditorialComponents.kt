package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ComicReadingPresentation
import com.example.ui.theme.LocalIOS27Theme

@Composable
internal fun EditorialAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val t = LocalIOS27Theme.current
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(50), colors = ButtonDefaults.outlinedButtonColors(contentColor = t.textPrimary)) {
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun ComicReadingSummary(presentation: ComicReadingPresentation, onEdit: () -> Unit) {
    val t = LocalIOS27Theme.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).contentMaterialSurface(RoundedCornerShape(18.dp))
        .clickable(onClickLabel = presentation.action, onClick = onEdit).padding(16.dp)
        .semantics(mergeDescendants = true) { stateDescription = listOf(presentation.title, presentation.detail).filter { it.isNotBlank() }.joinToString(" · ") },
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(when (presentation.title) { "Lido" -> Icons.Default.CheckCircle; "Lendo" -> Icons.Default.MenuBook; else -> Icons.Default.BookmarkBorder }, null, tint = t.textPrimary)
            Column(Modifier.weight(1f)) {
                Text(presentation.title, color = t.textPrimary, fontWeight = FontWeight.SemiBold)
                if (presentation.detail.isNotBlank()) Text(presentation.detail, color = t.textSecondary, fontSize = 13.sp)
            }
            Icon(Icons.Default.Edit, null, tint = t.textSecondary, modifier = Modifier.size(18.dp))
        }
        presentation.progress?.let { progress ->
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = t.textPrimary, trackColor = t.divider)
        }
    }
}
