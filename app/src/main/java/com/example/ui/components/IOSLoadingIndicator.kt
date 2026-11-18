package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.theme.LocalAppMotion

@Composable
internal fun LoadingLine(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    val status = modifier.semantics { liveRegion = LiveRegionMode.Polite }
    if (LocalAppMotion.current.reduced) {
        Text("Carregando…", status.progressSemantics(), color = LocalIOS27Theme.current.textSecondary)
    } else {
        LinearProgressIndicator(status.semantics { stateDescription = "Carregando" }, color = color)
    }
}

@Composable
internal fun LoadingSpinner(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 4.dp
) {
    val status = modifier.semantics {
        liveRegion = LiveRegionMode.Polite
        stateDescription = "Carregando"
    }
    IOSLoadingIndicator(status, color, strokeWidth)
}

/** Compose approximation of a twelve-spoke activity indicator, not an Apple native control. */
@Composable
fun IOSLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = LocalIOS27Theme.current.textSecondary,
    strokeWidth: Dp = 2.dp
) {
    val motion by androidx.compose.runtime.rememberUpdatedState(com.example.ui.theme.LocalAppMotion.current)
    val phase = if (motion.reduced) null else rememberInfiniteTransition(label = "iosActivity").animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(motion.tween(900, easing = LinearEasing)),
        label = "activityPhase"
    )
    Canvas(modifier.size(24.dp).progressSemantics()) {
        val radius = size.minDimension / 2f
        val rotationStep = phase?.value?.toInt() ?: 0
        repeat(12) { index ->
            val age = (index - rotationStep + 24) % 12
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
