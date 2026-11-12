package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.example.ui.theme.LocalAppMotion
import com.example.ui.theme.LocalIOS27Theme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlin.math.abs

@Immutable
data class AppPage(val tab: String, val settings: String?, val parent: String?, val statistics: Boolean) {
    val backDestination: AppPage? get() = when {
        statistics -> copy(statistics = false)
        tab == "settings" && settings != null -> copy(settings = parent, parent = null)
        tab == "settings" || tab == "shelf" -> AppPage("profile", null, null, false)
        tab != "home" -> AppPage("home", null, null, false)
        else -> null
    }
    val depth: Int get() = when {
        statistics -> 4
        tab == "settings" && settings != null -> 2 + if (parent != null) 1 else 0
        tab == "settings" || tab == "shelf" -> 1
        else -> 0
    }
}

/** Page navigation owns Back; modal handlers inside a page still take precedence. */
val LocalPageBackManaged = staticCompositionLocalOf { false }

/** At most two live pages, even when navigation is interrupted repeatedly. */
@Composable
fun AppPageTransition(
    page: AppPage,
    backEnabled: Boolean = false,
    backPage: AppPage? = page.backDestination,
    onBack: () -> Unit = {},
    content: @Composable (AppPage) -> Unit
) {
    val motion = LocalAppMotion.current
    val surface = LocalIOS27Theme.current.bgPrimary
    val backShape = remember { RoundedCornerShape(28.dp) }
    var current by remember { mutableStateOf(page) }
    var outgoing by remember { mutableStateOf<AppPage?>(null) }
    val progress = remember { Animatable(1f) }
    var slide by remember { mutableStateOf(false) }
    var direction by remember { mutableFloatStateOf(1f) }
    var enterX by remember { mutableFloatStateOf(0f) }
    var enterAlpha by remember { mutableFloatStateOf(1f) }
    var exitX by remember { mutableFloatStateOf(0f) }
    var exitAlpha by remember { mutableFloatStateOf(1f) }
    var preview by remember { mutableStateOf<AppPage?>(null) }
    var committed by remember { mutableStateOf(false) }
    var edge by remember { mutableFloatStateOf(1f) }
    val backProgress = remember { Animatable(0f) }
    val latestOnBack by rememberUpdatedState(onBack)
    val latestPage by rememberUpdatedState(page)
    val latestBackEnabled by rememberUpdatedState(backEnabled)
    // Register before the content: sheets and nested navigation can override this handler.
    PredictiveBackHandler(enabled = backEnabled && backPage != null) { events ->
        val startedPage = page
        val commitBack = latestOnBack
        val target = backPage ?: return@PredictiveBackHandler
        try {
            events.collect { event ->
                if (latestPage != startedPage || !latestBackEnabled) return@collect
                if (preview == null) {
                    outgoing = null
                    preview = target
                }
                edge = if (event.swipeEdge == BackEventCompat.EDGE_LEFT) 1f else -1f
                backProgress.snapTo(event.progress.coerceIn(0f, 1f))
            }
            if (latestPage != startedPage || !latestBackEnabled) {
                preview = null
                backProgress.snapTo(0f)
                return@PredictiveBackHandler
            }
            // Button/keyboard Back may not emit progress events.
            preview = target
            backProgress.animateTo(1f, motion.tween(150))
            if (latestPage != startedPage || !latestBackEnabled) {
                preview = null
                backProgress.snapTo(0f)
                return@PredictiveBackHandler
            }
            committed = true
            commitBack()
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) {
                backProgress.animateTo(0f, motion.tween(150))
                preview = null
            }
            throw cancelled
        }
    }
    LaunchedEffect(page) {
        if (page == current) return@LaunchedEffect
        if (committed && page == preview) {
            current = page
            outgoing = null
            preview = null
            committed = false
            enterX = 0f
            enterAlpha = 1f
            progress.snapTo(1f)
            backProgress.snapTo(0f)
            return@LaunchedEffect
        }
        preview = null
        committed = false
        backProgress.snapTo(0f)
        val p = progress.value
        val reversing = page == outgoing
        val oldCurrentX = enterX * (1f - p)
        val oldCurrentAlpha = enterAlpha + (1f - enterAlpha) * p
        val oldOutgoingX = exitX + ((if (slide) -direction * .25f else 0f) - exitX) * p
        val oldOutgoingAlpha = exitAlpha * (1f - p)
        val previousPage = current
        val nextDirection = if (page.depth >= current.depth) 1f else -1f
        val profileRoutes = setOf("profile", "shelf", "settings")
        val nextSlide = !motion.reduced && (page.tab == current.tab || (page.tab in profileRoutes && current.tab in profileRoutes))
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
        listOfNotNull(preview ?: outgoing, current).distinct().forEach { destination -> key(destination) {
            val interactive = destination == current && preview == null
            val input = if (interactive) Modifier else Modifier.clearAndSetSemantics { }
                .pointerInput(Unit) { awaitEachGesture { do { val event = awaitPointerEvent(); event.changes.forEach { it.consume() } } while (event.changes.any { it.pressed }) } }
            Box(Modifier.fillMaxSize().then(input).graphicsLayer {
                scaleX = 1f
                scaleY = 1f
                clip = false
                val p = progress.value
                if (preview != null) {
                    val b = if (motion.reduced) 0f else backProgress.value
                    translationX = size.width * edge * (if (destination == current) b else -.12f * (1f - b))
                    scaleX = if (destination == current) 1f - .04f * b else 1f
                    scaleY = scaleX
                    shape = backShape
                    clip = destination == current && b > 0f
                    alpha = if (destination == current) 1f else .85f + .15f * b
                } else if (destination == current) {
                    translationX = size.width * enterX * (1f - p)
                    alpha = (enterAlpha + (1f - enterAlpha) * p).coerceIn(0f, 1f)
                } else {
                    translationX = size.width * (exitX + ((if (slide) -direction * .25f else 0f) - exitX) * p)
                    alpha = (exitAlpha * (1f - p)).coerceIn(0f, 1f)
                }
            }.drawBehind {
                // Pages normally share the atmosphere; while overlapping, the front page
                // needs its own surface so text never shows through other text.
                if (preview != null && destination == current) drawRect(surface)
            }) {
                CompositionLocalProvider(LocalPageBackManaged provides backEnabled) { content(destination) }
            }
        } }
    }
}
