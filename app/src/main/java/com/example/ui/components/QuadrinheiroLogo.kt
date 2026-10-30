package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.theme.PlusJakartaSansFamily

enum class QuadrinheiroLogoVariant { Primary, OnDark, OnLight, Badge }

@Composable
fun QuadrinheiroLogo(
    variant: QuadrinheiroLogoVariant = QuadrinheiroLogoVariant.Primary,
    modifier: Modifier = Modifier,
    titleVisible: Boolean = true,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null
) {
    val theme = LocalIOS27Theme.current
    val original = variant == QuadrinheiroLogoVariant.Primary
    val paths = remember {
        listOf(
            "M44.8,26.3 L28.9,32.9 L25,54 L31.6,75.1 L39.5,69.8 L42.8,60.6 L40.1,52.7 L43.4,39.5 Z",
            "M49.4,26.3 L67.2,26.3 L80.4,35.5 L80.4,50.7 L63.9,50.7 L61.9,44.8 L53.3,38.8 L48.1,39.5 Z",
            "M63.9,55.3 L79.1,55.3 L76.4,63.9 L85.7,77.8 L83.7,83.7 L69.2,78.4 L55.3,80.4 L40.1,77.1 L46.1,65.9 L54,63.2 Z",
            "M44.8,26.3 L28.9,32.9 L25,54 L40.1,52.7 L43.4,39.5 Z",
            "M63.9,55.3 L79.1,55.3 L76.4,63.9 L85.7,77.8 L83.7,83.7 L54,63.2 Z"
        ).map { PathParser().parsePathString(it).toPath() }
    }
    val accent = theme.accent
    val brushes = remember(original, accent) {
        if (original) listOf(
            Brush.linearGradient(listOf(Color(0xFFFF1493), Color(0xFFE11D48), Color(0xFF881337)), Offset(25f,26.3f), Offset(44.8f,75.1f)),
            Brush.linearGradient(listOf(Color(0xFFFFD000), Color(0xFFFF9100), Color(0xFFFF3D00)), Offset(49.4f,26.3f), Offset(80.4f,50.7f)),
            Brush.linearGradient(listOf(Color(0xFF1E3A8A), Color(0xFF2563EB), Color(0xFF00B4D8), Color(0xFF00F5FF)), Offset(40.1f,63.2f), Offset(85.7f,83.7f))
        ) else listOf(
            Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.65f))),
            Brush.linearGradient(listOf(accent.copy(alpha = 0.75f), accent)),
            Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.8f)))
        )
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val badgeModifier = if (variant == QuadrinheiroLogoVariant.Badge) {
            Modifier.size(40.dp).background(theme.bgPrimary, RoundedCornerShape(12.dp)).padding(4.dp)
        } else Modifier.size(40.dp)
        Box(badgeModifier, contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(32.dp)) {
                scale(size.width / 108f, size.height / 108f, pivot = Offset.Zero) {
                    for (i in 0..2) drawPath(paths[i], brushes[i])
                    drawPath(paths[3], Brush.linearGradient(listOf(Color.White.copy(alpha = 0.24f), Color.Transparent), Offset(28.9f,32.9f), Offset(40.1f,52.7f)))
                    drawPath(paths[4], Brush.linearGradient(listOf(Color.White.copy(alpha = 0.2f), Color.Transparent), Offset(63.9f,55.3f), Offset(85.7f,83.7f)))
                }
            }
        }
        if (titleVisible || !subtitle.isNullOrBlank() || leadingIcon != null) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (leadingIcon != null) {
                        Icon(leadingIcon, contentDescription = null, tint = theme.textPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    if (titleVisible) Text("Quadrinheiro", fontFamily = PlusJakartaSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = theme.textPrimary)
                }
                if (!subtitle.isNullOrBlank()) Text(subtitle, fontFamily = PlusJakartaSansFamily, fontSize = 12.sp, color = theme.textSecondary)
            }
        }
    }
}
