package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import com.example.ui.theme.LocalAppMotion
import kotlin.math.abs

@Immutable
data class AppPage(val tab: String, val settings: String?, val parent: String?, val statistics: Boolean) {
    val depth: Int get() = if (statistics) 3 else if (settings != null) 1 + if (parent != null) 1 else 0 else 0
}

/** At most two live pages, even when navigation is interrupted repeatedly. */
@Composable
fun AppPageTransition(page: AppPage, content: @Composable (AppPage) -> Unit) {
    val motion = LocalAppMotion.current
    var current by remember { mutableStateOf(page) }
    var outgoing by remember { mutableStateOf<AppPage?>(null) }
    val progress = remember { Animatable(1f) }
    var slide by remember { mutableStateOf(false) }
    var direction by remember { mutableFloatStateOf(1f) }
    var enterX by remember { mutableFloatStateOf(0f) }
    var enterAlpha by remember { mutableFloatStateOf(1f) }
    var exitX by remember { mutableFloatStateOf(0f) }
    var exitAlpha by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(page) {
        if (page == current) return@LaunchedEffect
        val p = progress.value
        val reversing = page == outgoing
        val oldCurrentX = enterX * (1f - p)
        val oldCurrentAlpha = enterAlpha + (1f - enterAlpha) * p
        val oldOutgoingX = exitX + ((if (slide) -direction * .25f else 0f) - exitX) * p
        val oldOutgoingAlpha = exitAlpha * (1f - p)
        val previousPage = current
        val nextDirection = if (page.depth >= current.depth) 1f else -1f
        val nextSlide = !motion.reduced && page.tab == current.tab
        outgoing = previousPage
        current = page
        exitX = oldCurrentX
        exitAlpha = oldCurrentAlpha
        enterX = if (reversing) oldOutgoingX else if (nextSlide) nextDirection else 0f
        enterAlpha = if (reversing) oldOutgoingAlpha else 0f
        slide = nextSlide
        direction = nextDirection
        progress.snapTo(0f)
        if (motion.reduced) progress.snapTo(1f)
        else progress.animateTo(1f, motion.spring(1f, if (slide) 650f else 1200f))
        outgoing = null
    }
    Box(Modifier.fillMaxSize().clipToBounds()) {
        listOfNotNull(outgoing, current).forEach { destination -> key(destination) {
            Box(Modifier.fillMaxSize().graphicsLayer {
                val p = progress.value
                if (destination == current) {
                    translationX = size.width * enterX * (1f - p)
                    alpha = (enterAlpha + (1f - enterAlpha) * p).coerceIn(0f, 1f)
                } else {
                    translationX = size.width * (exitX + ((if (slide) -direction * .25f else 0f) - exitX) * p)
                    alpha = (exitAlpha * (1f - p)).coerceIn(0f, 1f)
                }
            }) { content(destination) }
        } }
    }
}
