package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.example.ui.theme.LocalAppMotion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

@Immutable
data class AppPage(val tab: String, val settings: String?, val parent: String?, val statistics: Boolean) {
    val backDestination: AppPage? get() = when {
        statistics -> copy(statistics = false)
        tab == "settings" && settings != null -> copy(settings = parent, parent = null)
        tab == "settings" || tab == "shelf" -> AppPage("profile", null, null, false)
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
internal val LocalBackPreview = compositionLocalOf { false }

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
    val parentIsBackPreview = LocalBackPreview.current
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
    val backProgress = remember { Animatable(0f) }
    val latestOnBack by rememberUpdatedState(onBack)
    val latestPage by rememberUpdatedState(page)
    val latestBackEnabled by rememberUpdatedState(backEnabled)
    val backSpring = motion.spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = .005f
    )
    // Register before the content: sheets and nested navigation can override this handler.
    PredictiveBackHandler(enabled = backEnabled && backPage != null && !parentIsBackPreview) { events ->
        val startedPage = page
        val commitBack = latestOnBack
        val target = backPage ?: return@PredictiveBackHandler
        if (motion.reduced) {
            events.collect {}
            if (latestPage == startedPage && latestBackEnabled) commitBack()
            return@PredictiveBackHandler
        }
        try {
            events.collect { event ->
                if (latestPage != startedPage || !latestBackEnabled) return@collect
                if (preview == null) {
                    outgoing = null
                    preview = target
                }
                // Hierarchical pop always slides the current page right, revealing the parent exclusively on the left.
                backProgress.snapTo(event.progress.coerceIn(0f, 1f))
            }
            if (latestPage != startedPage || !latestBackEnabled) {
                preview = null
                backProgress.snapTo(0f)
                return@PredictiveBackHandler
            }
            // Button/keyboard Back may not emit progress events.
            preview = target
            backProgress.animateTo(1f, backSpring)
            if (latestPage != startedPage || !latestBackEnabled) {
                preview = null
                backProgress.snapTo(0f)
                return@PredictiveBackHandler
            }
            committed = true
            commitBack()
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) {
                backProgress.animateTo(0f, backSpring)
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
        else progress.animateTo(1f, if (slide && direction < 0f) backSpring else motion.spring(1f, if (slide) 650f else 1200f))
        outgoing = null
    }
    val orderedPages = remember(preview, outgoing, current, slide, direction) {
        when {
            preview != null -> listOfNotNull(preview, current).distinct()
            outgoing != null && slide && direction < 0f -> listOfNotNull(current, outgoing).distinct()
            else -> listOfNotNull(outgoing, current).distinct()
        }
    }
    Box(Modifier.fillMaxSize().clipToBounds()) {
        orderedPages.forEach { destination -> key(destination) {
            val interactive = destination == current && preview == null
            val input = if (interactive) Modifier else Modifier.clearAndSetSemantics { }
                .pointerInput(Unit) { awaitEachGesture { do { val event = awaitPointerEvent(); event.changes.forEach { it.consume() } } while (event.changes.any { it.pressed }) } }
            Box(
                Modifier
                    .fillMaxSize()
                    .then(input)
                    .drawWithContent {
                        val p = progress.value.coerceIn(0f, 1f)
                        if (preview != null) {
                            val b = if (motion.reduced) 0f else backProgress.value.coerceIn(0f, 1f)
                            val splitX = (size.width * b).coerceIn(0f, size.width)
                            if (destination == preview) {
                                if (splitX > 0.5f) {
                                    clipRect(left = 0f, top = 0f, right = splitX, bottom = size.height) {
                                        this@drawWithContent.drawContent()
                                    }
                                    val shadowW = minOf(18.dp.toPx(), splitX)
                                    drawRect(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.12f * (1f - b))),
                                            startX = splitX - shadowW,
                                            endX = splitX
                                        ),
                                        topLeft = Offset(splitX - shadowW, 0f),
                                        size = Size(shadowW, size.height)
                                    )
                                }
                            } else {
                                drawContent()
                            }
                        } else if (outgoing != null && slide) {
                            val splitX = if (direction >= 0f) {
                                (size.width * (1f - p)).coerceIn(0f, size.width)
                            } else {
                                (size.width * p).coerceIn(0f, size.width)
                            }
                            val isLeftUnderlay = (direction >= 0f && destination == outgoing) ||
                                (direction < 0f && destination == current)
                            if (isLeftUnderlay) {
                                if (splitX > 0.5f) {
                                    clipRect(left = 0f, top = 0f, right = splitX, bottom = size.height) {
                                        this@drawWithContent.drawContent()
                                    }
                                }
                            } else {
                                drawContent()
                            }
                        } else {
                            drawContent()
                        }
                    }
                    .graphicsLayer {
                        scaleX = 1f
                        scaleY = 1f
                        clip = false
                        val p = progress.value.coerceIn(0f, 1f)
                        if (preview != null) {
                            val b = if (motion.reduced) 0f else backProgress.value.coerceIn(0f, 1f)
                            translationX = if (destination == current) {
                                size.width * b
                            } else {
                                -size.width * 0.18f * (1f - b)
                            }
                            alpha = 1f
                        } else if (outgoing != null && slide) {
                            if (direction >= 0f) {
                                if (destination == current) {
                                    translationX = size.width * (1f - p)
                                    alpha = 1f
                                } else {
                                    translationX = -size.width * 0.18f * p
                                    alpha = 1f
                                }
                            } else {
                                if (destination == current) {
                                    translationX = -size.width * 0.18f * (1f - p)
                                    alpha = 1f
                                } else {
                                    translationX = size.width * p
                                    alpha = 1f
                                }
                            }
                        } else if (destination == current) {
                            translationX = size.width * enterX * (1f - p)
                            alpha = (enterAlpha + (1f - enterAlpha) * p).coerceIn(0f, 1f)
                        } else {
                            translationX = size.width * (exitX + (-exitX) * p)
                            alpha = (exitAlpha * (1f - p)).coerceIn(0f, 1f)
                        }
                    }
            ) {
                CompositionLocalProvider(
                    LocalPageBackManaged provides backEnabled,
                    LocalBackPreview provides (parentIsBackPreview || destination == preview)
                ) { content(destination) }
            }
        } }
    }
}
