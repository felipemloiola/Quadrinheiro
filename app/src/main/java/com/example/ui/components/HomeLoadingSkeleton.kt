package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalAppMotion
import com.example.ui.theme.LocalIOS27Theme

@Composable
fun HomeLoadingSkeleton() {
    val theme = LocalIOS27Theme.current
    val reduced = LocalAppMotion.current.reduced
    val pulse = if (reduced) remember { mutableFloatStateOf(0.65f) } else {
        rememberInfiniteTransition(label = "collectionLoading").animateFloat(
            0.4f, 0.8f,
            infiniteRepeatable(tween(850, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "skeletonPulse"
        )
    }
    Column(
        Modifier.fillMaxSize().background(theme.bgPrimary).statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 28.dp)
            .semantics { contentDescription = "Preparando sua coleção" },
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        @Composable fun Placeholder(modifier: Modifier) {
            Box(modifier.graphicsLayer { alpha = pulse.value }
                .background(theme.textSecondary.copy(alpha = 0.14f), RoundedCornerShape(14.dp)))
        }
        Placeholder(Modifier.width(108.dp).height(16.dp))
        Placeholder(Modifier.width(210.dp).height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Placeholder(Modifier.width(138.dp).height(204.dp))
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Placeholder(Modifier.fillMaxWidth().height(24.dp))
                Placeholder(Modifier.fillMaxWidth(0.7f).height(16.dp))
                Spacer(Modifier.height(72.dp))
                Placeholder(Modifier.fillMaxWidth().height(40.dp))
            }
        }
        Placeholder(Modifier.fillMaxWidth().height(48.dp))
        Placeholder(Modifier.width(150.dp).height(20.dp))
        repeat(2) { Placeholder(Modifier.fillMaxWidth().height(68.dp)) }
    }
}
