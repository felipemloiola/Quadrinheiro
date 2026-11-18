package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ComicReadingPresentation
import com.example.ui.theme.LocalAppMotion
import com.example.ui.theme.LocalIOS27Theme
import com.kyant.shapes.ContinuousCapsule
import com.kyant.shapes.ContinuousRoundedRectangle

@Composable
internal fun EditorialAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    CurationGlassAction(label, onClick, modifier, enabled = enabled)
}

@Composable
internal fun ComicReadingSummary(presentation: ComicReadingPresentation, onEdit: () -> Unit) {
    val t = LocalIOS27Theme.current
    val motion = LocalAppMotion.current
    val cardShape = ContinuousRoundedRectangle(18.dp)
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp).contentMaterialSurface(cardShape)
            .clickable(onClickLabel = presentation.action, onClick = onEdit).padding(16.dp)
            .semantics(mergeDescendants = true) {
                stateDescription = listOf(presentation.title, presentation.detail).filter { it.isNotBlank() }.joinToString(" · ")
            },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                when (presentation.title) {
                    "Lido" -> Icons.Default.CheckCircle
                    "Lendo" -> Icons.AutoMirrored.Filled.MenuBook
                    else -> Icons.Default.BookmarkBorder
                },
                null,
                tint = t.textPrimary
            )
            Column(Modifier.weight(1f)) {
                Text(presentation.title, color = t.textPrimary, fontWeight = FontWeight.SemiBold)
                if (presentation.detail.isNotBlank()) Text(presentation.detail, color = t.textSecondary, fontSize = 13.sp)
            }
            Icon(Icons.Default.Edit, null, tint = t.textSecondary, modifier = Modifier.size(18.dp))
        }
        presentation.progress?.let { rawProgress ->
            val clamped = rawProgress.coerceIn(0f, 1f)
            val animatedProgress by animateFloatAsState(
                targetValue = clamped,
                animationSpec = if (motion.reduced) motion.tween(0) else motion.spring(stiffness = 480f),
                label = "readingSummaryProgress"
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(ContinuousCapsule)
                    .background(t.divider, ContinuousCapsule)
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                        .clip(ContinuousCapsule)
                        .background(t.accent, ContinuousCapsule)
                )
            }
        }
    }
}
