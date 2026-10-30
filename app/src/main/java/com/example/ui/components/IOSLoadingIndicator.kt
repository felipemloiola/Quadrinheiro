package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalIOS27Theme

/** Compose approximation of a twelve-spoke activity indicator, not an Apple native control. */
@Composable
fun IOSLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = LocalIOS27Theme.current.textSecondary,
    strokeWidth: Dp = 2.dp
) {
    val transition = rememberInfiniteTransition(label = "iosActivity")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "activityPhase"
    )
    Canvas(modifier.size(24.dp).progressSemantics()) {
        val radius = size.minDimension / 2f
        repeat(12) { index ->
            val age = (index - phase.toInt() + 24) % 12
            val opacity = 0.18f + 0.82f * age / 11f
            rotate(index * 30f, pivot = center) {
                drawLine(
                    color = color.copy(alpha = color.alpha * opacity),
                    start = Offset(center.x, center.y - radius * 0.45f),
                    end = Offset(center.x, center.y - radius * 0.85f),
                    strokeWidth = strokeWidth.toPx().coerceAtMost(radius * 0.22f),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
