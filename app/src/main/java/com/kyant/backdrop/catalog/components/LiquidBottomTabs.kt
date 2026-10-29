package com.kyant.backdrop.catalog.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.catalog.utils.DampedDragAnimation
import com.kyant.backdrop.catalog.utils.InteractiveHighlight
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.launch

@Composable
fun LiquidBottomTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    collapsedFraction: () -> Float = { 0f },
    isLightTheme: Boolean = !isSystemInDarkTheme(),
    accentColor: Color = if (isLightTheme) Color(0xFF0088FF) else Color(0xFF0091FF),
    unselectedContentColor: Color = if (isLightTheme) Color(0xFF1A1A1A) else Color.White,
    containerColor: Color = if (isLightTheme) Color(0xFFFAFAFA).copy(0.4f) else Color(0xFF121212).copy(0.4f),
    blurRadius: Dp = 8f.dp,
    refractionMultiplier: Float = 1f,
    chromaticAberrationEnabled: Boolean = true,
    depthEffectEnabled: Boolean = true,
    vibrancyEnabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val tabsBackdrop = rememberLayerBackdrop()

    val collapsed = collapsedFraction().fastCoerceIn(0f, 1f)
    val containerHeight = lerp(64f.dp, 54f.dp, collapsed)
    val indicatorHeight = lerp(56f.dp, 46f.dp, collapsed)

    val currentSelectedIndex = selectedTabIndex()
    val isIndicatorVisible = currentSelectedIndex in 0 until tabsCount
    var lastValidTabIndex by remember {
        mutableIntStateOf(currentSelectedIndex.coerceIn(0, tabsCount - 1))
    }
    if (isIndicatorVisible) {
        lastValidTabIndex = currentSelectedIndex
    }

    val currentOnTabSelected by rememberUpdatedState(onTabSelected)
    val currentSelectedTabIndexLambda by rememberUpdatedState(selectedTabIndex)

    val effectiveRefraction = refractionMultiplier.coerceIn(0.2f, 2.4f)
    val effectiveBlur = blurRadius.coerceIn(2f.dp, 24f.dp)

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val estimatedInitialWidthPx = remember(density, configuration.screenWidthDp) {
        with(density) { (configuration.screenWidthDp.dp - 106.dp).toPx().coerceAtLeast(180f) }
    }
    var containerWidthPx by remember { mutableIntStateOf(estimatedInitialWidthPx.fastRoundToInt()) }

    val tabWidth = remember(containerWidthPx, density, tabsCount) {
        with(density) {
            ((containerWidthPx.toFloat() - 8f.dp.toPx()) / tabsCount.coerceAtLeast(1)).coerceAtLeast(1f)
        }
    }
    val currentTabWidth by rememberUpdatedState(tabWidth)

    Box(
        modifier
            .height(containerHeight)
            .onSizeChanged { sz ->
                if (sz.width > 0 && sz.width != containerWidthPx) {
                    containerWidthPx = sz.width
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val offsetAnimation = remember { Animatable(0f) }
        val panelOffset by remember(density) {
            derivedStateOf {
                val maxW = containerWidthPx.toFloat().coerceAtLeast(1f)
                val fraction = (offsetAnimation.value / maxW).fastCoerceIn(-1f, 1f)
                with(density) {
                    4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var didDrag by remember { mutableStateOf(false) }
        var totalDragDistance by remember { mutableStateOf(0f) }
        val dampedDragAnimation = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = lastValidTabIndex.toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 78f / 56f,
                onDragStarted = {
                    didDrag = false
                    totalDragDistance = 0f
                },
                onDragStopped = {
                    val targetIndex = targetValue.fastRoundToInt().coerceIn(0, tabsCount - 1)
                    val tapSlopPx = with(density) { 18.dp.toPx() }
                    if (didDrag) {
                        didDrag = false
                        if (currentSelectedTabIndexLambda() != targetIndex || totalDragDistance <= tapSlopPx) {
                            currentOnTabSelected(targetIndex)
                        }
                    } else {
                        currentOnTabSelected(targetIndex)
                    }
                    totalDragDistance = 0f
                    animateToValue(targetIndex.toFloat())
                    animationScope.launch {
                        offsetAnimation.animateTo(
                            0f,
                            spring(1f, 300f, 0.5f)
                        )
                    }
                },
                onDrag = { _, dragAmount ->
                    if (!didDrag) {
                        didDrag = dragAmount.x != 0f
                    }
                    totalDragDistance += abs(dragAmount.x)
                    val width = currentTabWidth.coerceAtLeast(1f)
                    updateValue(
                        (targetValue + dragAmount.x / width * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            )
        }

        val indicatorAlpha by animateFloatAsState(
            targetValue = if (isIndicatorVisible) 1f else 0f,
            animationSpec = tween(durationMillis = 180, easing = EaseOut),
            label = "IndicatorAlpha"
        )

        var wasIndicatorVisible by remember { mutableStateOf(isIndicatorVisible) }
        LaunchedEffect(currentSelectedIndex) {
            val nowVisible = currentSelectedIndex in 0 until tabsCount
            if (nowVisible) {
                if (!wasIndicatorVisible) {
                    dampedDragAnimation.snapToValue(currentSelectedIndex.toFloat())
                } else if (dampedDragAnimation.targetValue != currentSelectedIndex.toFloat()) {
                    dampedDragAnimation.animateToValue(currentSelectedIndex.toFloat())
                }
            }
            wasIndicatorVisible = nowVisible
        }

        // Importante: NÃO colocar tabWidth como chave do remember para não recriar InteractiveHighlight a cada frame ao encolher!
        val interactiveHighlight = remember(animationScope) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, _ ->
                    val tw = currentTabWidth
                    Offset(
                        if (isLtr) (dampedDragAnimation.value + 0.5f) * tw + panelOffset
                        else size.width - (dampedDragAnimation.value + 0.5f) * tw + panelOffset,
                        size.height / 2f
                    )
                }
            )
        }

        val combinedTabsBackdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop)

        // Camada 1: Barra principal de fundo (64.dp -> 54.dp), com refração e aberração cromática reais
        Row(
            Modifier
                .graphicsLayer {
                    translationX = panelOffset
                }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousCapsule },
                    highlight = {
                        Highlight.Default.copy(alpha = if (isLightTheme) 0.5f else 0.35f)
                    },
                    effects = {
                        if (vibrancyEnabled) vibrancy()
                        blur(effectiveBlur.toPx())
                        if (effectiveRefraction > 0f) {
                            lens(
                                refractionHeight = 24f.dp.toPx() * effectiveRefraction,
                                refractionAmount = 32f.dp.toPx() * effectiveRefraction,
                                depthEffect = depthEffectEnabled,
                                chromaticAberration = chromaticAberrationEnabled
                            )
                        }
                    },
                    layerBlock = {
                        val progress = dampedDragAnimation.pressProgress
                        val scale = lerp(1f, 1f + 16f.dp.toPx() / size.width.coerceAtLeast(1f), progress)
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = { drawRect(containerColor) }
                )
                .then(interactiveHighlight.modifier)
                .height(containerHeight)
                .fillMaxWidth()
                .padding(4f.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )

        // Camada 2: tabsBackdrop (56.dp -> 46.dp)
        val innerTabColor = if (isIndicatorVisible || dampedDragAnimation.pressProgress > 0.01f) {
            accentColor
        } else {
            unselectedContentColor
        }
        CompositionLocalProvider(
            LocalLiquidBottomTabScale provides {
                lerp(1f, 1.2f, dampedDragAnimation.pressProgress)
            },
            LocalLiquidBottomTabColor provides innerTabColor
        ) {
            Row(
                Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .graphicsLayer {
                        translationX = panelOffset
                    }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousCapsule },
                        effects = {
                            val progress = dampedDragAnimation.pressProgress
                            if (vibrancyEnabled) vibrancy()
                            blur(effectiveBlur.toPx())
                            if (effectiveRefraction > 0f) {
                                lens(
                                    refractionHeight = 24f.dp.toPx() * progress * effectiveRefraction,
                                    refractionAmount = 32f.dp.toPx() * progress * effectiveRefraction,
                                    depthEffect = depthEffectEnabled,
                                    chromaticAberration = chromaticAberrationEnabled
                                )
                            }
                        },
                        highlight = {
                            val progress = dampedDragAnimation.pressProgress
                            Highlight.Default.copy(alpha = progress)
                        },
                        onDrawSurface = { drawRect(containerColor) }
                    )
                    .then(interactiveHighlight.modifier)
                    .height(indicatorHeight)
                    .fillMaxWidth()
                    .padding(horizontal = 4f.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }

        // Camada 3: Lupa / Indicador (56.dp -> 46.dp escalado por dampedDragAnimation.scaleX/scaleY = 78/56)
        val effectiveDisplayIndex = if (!wasIndicatorVisible && isIndicatorVisible) {
            currentSelectedIndex.toFloat()
        } else {
            dampedDragAnimation.value
        }
        Box(
            Modifier
                .padding(horizontal = 4f.dp)
                .graphicsLayer {
                    val tw = currentTabWidth
                    translationX =
                        if (isLtr) effectiveDisplayIndex * tw + panelOffset
                        else size.width - (effectiveDisplayIndex + 1f) * tw + panelOffset
                    alpha = indicatorAlpha
                }
                .then(if (isIndicatorVisible) interactiveHighlight.gestureModifier else Modifier)
                .then(if (isIndicatorVisible) dampedDragAnimation.modifier else Modifier)
                .drawBackdrop(
                    backdrop = combinedTabsBackdrop,
                    shape = { ContinuousCapsule },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        if (effectiveRefraction > 0f) {
                            lens(
                                refractionHeight = lerp(4f.dp.toPx(), 12f.dp.toPx(), progress) * effectiveRefraction,
                                refractionAmount = lerp(6f.dp.toPx(), 18f.dp.toPx(), progress) * effectiveRefraction,
                                depthEffect = depthEffectEnabled,
                                chromaticAberration = chromaticAberrationEnabled
                            )
                        }
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Default.copy(alpha = lerp(0.22f, 1f, progress))
                    },
                    shadow = {
                        val progress = dampedDragAnimation.pressProgress
                        Shadow(alpha = progress)
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = 8f.dp * progress,
                            alpha = progress
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(
                            if (isLightTheme) Color.Black.copy(0.1f)
                            else Color.White.copy(0.1f),
                            alpha = 1f - progress
                        )
                        drawRect(Color.Black.copy(alpha = 0.03f * progress))
                    }
                )
                .height(indicatorHeight)
                .fillMaxWidth(1f / tabsCount)
        )
    }
}

