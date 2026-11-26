package com.example.ui.components

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalAppMotion
import com.example.ui.theme.LocalAppSettings
import com.example.ui.theme.LocalIOS27Theme
import com.kyant.shapes.ContinuousRoundedRectangle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

/**
 * Shared depth coordinator for iOS 26 receding-background transitions.
 * Read exclusively inside draw/graphicsLayer phases on the root content layer so opening
 * Comic Detail or Liquid Glass sheets never triggers main-screen recomposition.
 */
@Stable
class RootDepthHost {
    private val depths = mutableStateMapOf<String, Float>()
    val depth: Float
        get() = depths.values.maxOrNull()?.coerceIn(0f, 1f) ?: 0f

    fun update(id: String, value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        if (clamped <= 0.0005f) {
            depths.remove(id)
        } else {
            depths[id] = clamped
        }
    }

    fun remove(id: String) {
        depths.remove(id)
    }
}

val LocalRootDepthHost = staticCompositionLocalOf<RootDepthHost?> { null }

@Immutable
data class AppPage(val tab: String, val settings: String?, val parent: String?, val statistics: Boolean) {
    val depth: Int get() = when {
        statistics -> 4
        tab == "settings" && settings != null -> 2 + if (parent != null) 1 else 0
        tab == "settings" || tab == "shelf" -> 1
        else -> 0
    }
}

@Immutable
internal data class PredictiveBackLayerTransform(
    val foregroundTranslationX: Float,
    val foregroundScale: Float,
    val foregroundAlpha: Float,
    val backgroundTranslationX: Float,
    val backgroundScrimAlpha: Float
)

/**
 * Pure transform for predictive back preview with subtle iOS-style parallax on the previous route.
 * When [reducedMotion] is true, lateral parallax and scaling are suppressed in favor of a gentle fade.
 */
internal fun computePredictiveBackTransform(
    progress: Float,
    widthPx: Float,
    reducedMotion: Boolean,
    swipeEdge: Int = BackEventCompat.EDGE_LEFT
): PredictiveBackLayerTransform {
    val p = progress.coerceIn(0f, 1f)
    if (reducedMotion || p <= 0f) {
        return PredictiveBackLayerTransform(
            foregroundTranslationX = 0f,
            foregroundScale = 1f,
            foregroundAlpha = if (reducedMotion) (1f - 0.18f * p).coerceIn(0.82f, 1f) else 1f,
            backgroundTranslationX = 0f,
            backgroundScrimAlpha = if (p > 0f) 0.10f * (1f - p) else 0f
        )
    }
    val dir = if (swipeEdge == BackEventCompat.EDGE_RIGHT) -1f else 1f
    val fgShift = widthPx * 0.88f * p * dir
    val bgParallax = -widthPx * 0.22f * (1f - p) * dir
    return PredictiveBackLayerTransform(
        foregroundTranslationX = fgShift,
        foregroundScale = 1f - 0.02f * p,
        foregroundAlpha = 1f,
        backgroundTranslationX = bgParallax,
        backgroundScrimAlpha = (0.16f * (1f - p)).coerceIn(0f, 0.16f)
    )
}

/**
 * Pure transform for root-level predictive back exit feedback (e.g. closing ComicDetail root or CatalogBrowser).
 */
internal fun computePredictiveRootExitTransform(
    progress: Float,
    widthPx: Float,
    reducedMotion: Boolean,
    swipeEdge: Int = BackEventCompat.EDGE_LEFT
): PredictiveBackLayerTransform {
    val p = progress.coerceIn(0f, 1f)
    if (reducedMotion || p <= 0f) {
        return PredictiveBackLayerTransform(
            foregroundTranslationX = 0f,
            foregroundScale = 1f,
            foregroundAlpha = if (reducedMotion) (1f - 0.22f * p).coerceIn(0.78f, 1f) else 1f,
            backgroundTranslationX = 0f,
            backgroundScrimAlpha = 0f
        )
    }
    val dir = if (swipeEdge == BackEventCompat.EDGE_RIGHT) -1f else 1f
    return PredictiveBackLayerTransform(
        foregroundTranslationX = widthPx * 0.12f * p * dir,
        foregroundScale = 1f - 0.06f * p,
        foregroundAlpha = (1f - 0.12f * p).coerceIn(0.88f, 1f),
        backgroundTranslationX = 0f,
        backgroundScrimAlpha = 0f
    )
}

/**
 * Hosts a foreground route with live Android Predictive Back preview of [previousContent] underneath.
 * - On progress: reveals [previousContent] with authentic iOS left-edge parallax, subtle lateral shadow,
 *   and continuous squircle corner rounding (or fade-only when reduced motion is active).
 * - On commit: smoothly finishes the exit glide with a non-bouncy spring before invoking [onBackCommit]
 *   so there are no abrupt jumps or dark flashes.
 * - On cancel: settles foreground cleanly back to rest using [Spring.DampingRatioNoBouncy] and
 *   [Spring.StiffnessMediumLow].
 */
@Composable
internal fun PredictiveBackRouteContainer(
    canNavigateBack: Boolean,
    enabled: Boolean = true,
    onBackCommit: () -> Unit,
    previousContent: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
    currentContent: @Composable () -> Unit
) {
    val motion by rememberUpdatedState(LocalAppMotion.current)
    val settings = LocalAppSettings.current
    val theme = LocalIOS27Theme.current
    val currentOnCommit by rememberUpdatedState(onBackCommit)
    val gestureProgress = remember { Animatable(0f) }
    var swipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }
    var gestureActive by remember { mutableStateOf(false) }

    LaunchedEffect(canNavigateBack) {
        if (!canNavigateBack && (gestureActive || gestureProgress.value > 0f)) {
            gestureActive = false
            gestureProgress.snapTo(0f)
        }
    }

    PredictiveBackHandler(enabled = canNavigateBack && enabled) { backEvents: Flow<BackEventCompat> ->
        gestureActive = true
        try {
            backEvents.collect { event ->
                swipeEdge = event.swipeEdge
                gestureProgress.snapTo(event.progress.coerceIn(0f, 1f))
            }
            if (!motion.reduced && gestureProgress.value in 0.02f..0.96f) {
                gestureProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = motion.spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            currentOnCommit()
            gestureProgress.snapTo(0f)
        } catch (cancel: CancellationException) {
            if (motion.reduced) {
                gestureProgress.snapTo(0f)
            } else {
                gestureProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = motion.spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        } finally {
            gestureActive = false
        }
    }

    val p = gestureProgress.value
    val showPreview = (gestureActive || p > 0.001f) && canNavigateBack && previousContent != null

    Box(
        modifier
            .fillMaxSize()
            .background(theme.bgPrimary)
            .clipToBounds()
    ) {
        if (showPreview) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clearAndSetSemantics {}
                    .graphicsLayer {
                        val t = computePredictiveBackTransform(p, size.width, motion.reduced, swipeEdge)
                        translationX = t.backgroundTranslationX
                    }
            ) {
                previousContent()
                val scrimAlpha = if (settings.reduceTransparency) {
                    (0.10f * (1f - p)).coerceIn(0f, 0.10f)
                } else {
                    (0.16f * (1f - p)).coerceIn(0f, 0.16f)
                }
                if (scrimAlpha > 0.002f) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrimAlpha)))
                }
            }
        }

        val cornerDp = if (p > 0.001f && !motion.reduced) (22f * p.coerceIn(0f, 1f)).dp else 0.dp
        val isRightEdge = swipeEdge == BackEventCompat.EDGE_RIGHT
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val t = computePredictiveBackTransform(p, size.width, motion.reduced, swipeEdge)
                    translationX = t.foregroundTranslationX
                    scaleX = t.foregroundScale
                    scaleY = t.foregroundScale
                    alpha = t.foregroundAlpha
                    if (cornerDp > 0.dp) {
                        shape = ContinuousRoundedRectangle(cornerDp)
                        clip = true
                        shadowElevation = 18.dp.toPx() * (1f - 0.35f * p)
                    }
                }
        ) {
            currentContent()
            // Subtle iOS lateral edge shadow cast on the revealed page border during swipe
            if (p > 0.005f && !motion.reduced) {
                val shadowAlpha = (0.18f * (1f - p * 0.45f)).coerceIn(0f, 0.18f)
                Box(
                    modifier = Modifier
                        .align(if (isRightEdge) Alignment.CenterEnd else Alignment.CenterStart)
                        .offset(x = if (isRightEdge) 14.dp else (-14).dp)
                        .width(14.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                colors = if (isRightEdge) {
                                    listOf(Color.Black.copy(alpha = shadowAlpha), Color.Transparent)
                                } else {
                                    listOf(Color.Transparent, Color.Black.copy(alpha = shadowAlpha))
                                }
                            )
                        )
                )
            }
        }
    }
}

/**
 * Root-level Predictive Back exit gesture feedback for full-screen overlays (e.g. ComicDetail root, CatalogBrowser).
 */
@Composable
internal fun PredictiveBackRootExitContainer(
    enabled: Boolean,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    onProgressChanged: ((Float) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val motion by rememberUpdatedState(LocalAppMotion.current)
    val currentOnExit by rememberUpdatedState(onExit)
    val currentOnProgress by rememberUpdatedState(onProgressChanged)
    val gestureProgress = remember { Animatable(0f) }
    var swipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }

    LaunchedEffect(gestureProgress) {
        snapshotFlow { gestureProgress.value }.collect { progress ->
            currentOnProgress?.invoke(progress.coerceIn(0f, 1f))
        }
    }

    PredictiveBackHandler(enabled = enabled) { backEvents: Flow<BackEventCompat> ->
        try {
            backEvents.collect { event ->
                swipeEdge = event.swipeEdge
                gestureProgress.snapTo(event.progress.coerceIn(0f, 1f))
            }
            gestureProgress.snapTo(0f)
            currentOnExit()
        } catch (cancel: CancellationException) {
            if (motion.reduced) {
                gestureProgress.snapTo(0f)
            } else {
                gestureProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = motion.spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }
    }

    val p = gestureProgress.value
    val cornerDp = if (p > 0.001f && !motion.reduced) (26f * p.coerceIn(0f, 1f)).dp else 0.dp
    Box(
        modifier
            .fillMaxSize()
            .graphicsLayer {
                val t = computePredictiveRootExitTransform(p, size.width, motion.reduced, swipeEdge)
                translationX = t.foregroundTranslationX
                scaleX = t.foregroundScale
                scaleY = t.foregroundScale
                alpha = t.foregroundAlpha
                if (cornerDp > 0.dp) {
                    shape = ContinuousRoundedRectangle(cornerDp)
                    clip = true
                    shadowElevation = 16.dp.toPx() * p
                }
            }
    ) {
        content()
    }
}

/**
 * At most two live pages, even when navigation is interrupted repeatedly.
 * Uses authentic iOS push/pop layering with [Spring.DampingRatioNoBouncy] and [Spring.StiffnessMediumLow]:
 * - Push (`direction > 0`): incoming page slides cleanly from right over outgoing page with subtle parallax + scrim.
 * - Pop (`direction < 0`): outgoing page slides off to the right ON TOP of the incoming previous page emerging with
 *   subtle left parallax + fading scrim, keeping full opacity on both pages to eliminate black flashes.
 */
@Composable
fun AppPageTransition(page: AppPage, content: @Composable (AppPage) -> Unit) {
    val motion = LocalAppMotion.current
    val theme = LocalIOS27Theme.current
    var current by remember { mutableStateOf(page) }
    var outgoing by remember { mutableStateOf<AppPage?>(null) }
    val progress = remember { Animatable(1f) }
    var slide by remember { mutableStateOf(false) }
    var direction by remember { mutableFloatStateOf(1f) }
    var enterStartX by remember { mutableFloatStateOf(0f) }
    var exitStartX by remember { mutableFloatStateOf(0f) }
    var enterStartAlpha by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(page) {
        if (page == current) return@LaunchedEffect
        val p = progress.value
        val reversing = page == outgoing
        val prevEnterTargetX = 0f
        val prevExitTargetX = if (slide) {
            if (direction > 0f) -0.24f else 1f
        } else 0f
        val oldCurrentX = enterStartX + (prevEnterTargetX - enterStartX) * p
        val oldOutgoingX = exitStartX + (prevExitTargetX - exitStartX) * p
        val oldCurrentAlpha = enterStartAlpha + (1f - enterStartAlpha) * p

        val previousPage = current
        val nextDirection = if (page.depth >= current.depth) 1f else -1f
        val profileRoutes = setOf("profile", "shelf", "settings")
        val nextSlide = !motion.reduced && (page.tab == current.tab || (page.tab in profileRoutes && current.tab in profileRoutes))

        outgoing = previousPage
        current = page
        slide = nextSlide
        direction = nextDirection
        exitStartX = oldCurrentX
        enterStartX = when {
            !nextSlide -> 0f
            reversing -> oldOutgoingX
            nextDirection > 0f -> 1f
            else -> -0.24f
        }
        enterStartAlpha = when {
            nextSlide -> 1f
            reversing -> (1f - oldCurrentAlpha).coerceIn(0f, 1f)
            else -> 0f
        }

        progress.snapTo(0f)
        if (motion.reduced) {
            progress.snapTo(1f)
        } else if (nextSlide) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = motion.ios26DetailZoomSpring()
            )
        } else {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = motion.tween(180)
            )
        }
        outgoing = null
    }

    val isPoppingSlide = slide && direction < 0f && outgoing != null
    val orderedPages = if (isPoppingSlide) {
        listOfNotNull(current, outgoing)
    } else {
        listOfNotNull(outgoing, current)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(theme.bgPrimary)
            .clipToBounds()
    ) {
        orderedPages.forEach { destination ->
            key(destination) {
                val isCurrent = destination == current
                val isTopSlidingLayer = slide && outgoing != null && ((direction > 0f && isCurrent) || (direction < 0f && !isCurrent))
                val isUnderlayParallaxLayer = slide && outgoing != null && !isTopSlidingLayer

                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val p = progress.value
                            if (slide) {
                                val targetX = if (isCurrent) 0f else if (direction > 0f) -0.24f else 1f
                                val startX = if (isCurrent) enterStartX else exitStartX
                                translationX = size.width * (startX + (targetX - startX) * p)
                                alpha = 1f
                                if (isTopSlidingLayer && p > 0.002f && p < 0.998f) {
                                    shadowElevation = 16.dp.toPx()
                                }
                            } else {
                                translationX = 0f
                                // Keep outgoing opaque underneath while incoming fades smoothly to avoid dark dip
                                alpha = if (isCurrent) (enterStartAlpha + (1f - enterStartAlpha) * p).coerceIn(0f, 1f) else 1f
                            }
                        }
                ) {
                    content(destination)
                    if (isUnderlayParallaxLayer) {
                        val p = progress.value
                        val scrim = if (direction > 0f) 0.14f * p else 0.14f * (1f - p)
                        if (scrim > 0.002f) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = scrim.coerceIn(0f, 0.14f)))
                            )
                        }
                    }
                }
            }
        }
    }
}
