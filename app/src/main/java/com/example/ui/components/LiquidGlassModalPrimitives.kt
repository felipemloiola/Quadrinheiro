package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.example.ui.theme.LocalAppMotion
import com.example.ui.theme.LocalAppSettings
import com.example.ui.theme.LocalAppleMaterials
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.contrastRatio
import com.example.ui.theme.readableForeground
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.catalog.utils.InteractiveHighlight
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.ContinuousCapsule
import com.kyant.shapes.ContinuousRoundedRectangle
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Entrada de overlay in-window para sheets e diálogos Liquid Glass com refração GPU real sobre o fundo vivo.
 */
@Stable
class LiquidGlassOverlayEntry(
    val id: String,
    initialBackdrop: Backdrop?,
    initialOnDismiss: () -> Unit,
    initialContent: @Composable BoxScope.(Backdrop) -> Unit
) {
    var backdropOverride by mutableStateOf(initialBackdrop)
    var onDismiss by mutableStateOf(initialOnDismiss)
    var content by mutableStateOf(initialContent)
}

/**
 * Host raiz in-window para sheets (`CurationSheet`) e diálogos (`LiquidGlassDialog`).
 * Renderizar na mesma janela principal (irmão do `mainContentBackdrop`) permite refração óptica real
 * das capas, banners e perfil que estão atrás do modal — idêntico ao iOS 26.
 */
@Stable
class LiquidGlassOverlayHostState {
    internal val entries = mutableStateListOf<LiquidGlassOverlayEntry>()

    val hasActiveOverlay: Boolean
        get() = entries.isNotEmpty()

    fun upsert(
        id: String,
        backdropOverride: Backdrop?,
        onDismiss: () -> Unit,
        content: @Composable BoxScope.(Backdrop) -> Unit
    ) {
        val existing = entries.firstOrNull { it.id == id }
        if (existing != null) {
            existing.backdropOverride = backdropOverride
            existing.onDismiss = onDismiss
            existing.content = content
        } else {
            entries.add(LiquidGlassOverlayEntry(id, backdropOverride, onDismiss, content))
        }
    }

    fun remove(id: String) {
        entries.removeAll { it.id == id }
    }
}

val LocalLiquidGlassOverlayHost = staticCompositionLocalOf<LiquidGlassOverlayHostState?> { null }

/**
 * Backdrop opcional fornecido por telas modais próprias (ex.: `ComicDetailScreen`) para que
 * `CurationSheet`s abertos sobre elas refratem a própria tela de detalhes em vez de uma camada antiga.
 */
val LocalLiquidGlassOverlayBackdrop = staticCompositionLocalOf<Backdrop?> { null }

@Composable
fun LiquidGlassOverlayHost(
    state: LiquidGlassOverlayHostState,
    defaultBackdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val topEntry = state.entries.lastOrNull()
    BackHandler(enabled = topEntry != null) {
        topEntry?.onDismiss?.invoke()
    }
    if (state.entries.isEmpty()) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(38f)
    ) {
        state.entries.forEach { entry ->
            val effectiveBackdrop = entry.backdropOverride ?: defaultBackdrop
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(entry.id) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent()
                            }
                        }
                    }
            ) {
                CompositionLocalProvider(LocalLiquidGlassBackdrop provides effectiveBackdrop) {
                    entry.content(this, effectiveBackdrop)
                }
            }
        }
    }
}

/**
 * Portal reutilizável: quando dentro da janela principal do app, projeta o conteúdo no
 * `LiquidGlassOverlayHost` com refração GPU viva sobre o conteúdo real da tela; quando usado em
 * previews ou testes isolados sem host raiz, faz fallback automático para `Dialog`.
 */
@Composable
fun LiquidGlassOverlayPortal(
    onDismissRequest: () -> Unit,
    content: @Composable BoxScope.(Backdrop) -> Unit
) {
    val host = LocalLiquidGlassOverlayHost.current
    val overlayBackdropOverride = LocalLiquidGlassOverlayBackdrop.current
    val currentOnDismiss by rememberUpdatedState(onDismissRequest)
    val currentContent by rememberUpdatedState(content)

    if (host != null) {
        val entryId = remember { "lg_overlay_${System.nanoTime()}" }
        SideEffect {
            host.upsert(
                id = entryId,
                backdropOverride = overlayBackdropOverride,
                onDismiss = { currentOnDismiss() },
                content = { backdrop -> currentContent(backdrop) }
            )
        }
        DisposableEffect(host, entryId) {
            onDispose { host.remove(entryId) }
        }
    } else {
        val fallbackBackdrop = rememberLayerBackdrop()
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(fallbackBackdrop)
                )
                CompositionLocalProvider(LocalLiquidGlassBackdrop provides fallbackBackdrop) {
                    currentContent(fallbackBackdrop)
                }
            }
        }
    }
}

/**
 * Realce especular adaptativo de borda para superfícies Liquid Glass (iOS 26 HIG).
 * Ajusta suavemente a intensidade conforme modo claro/escuro e `reduceTransparency`.
 */
fun adaptiveGlassHighlight(
    isLight: Boolean,
    reduceTransparency: Boolean = false
): Highlight = Highlight(
    width = 0.75.dp,
    alpha = when {
        reduceTransparency -> if (isLight) 0.24f else 0.18f
        isLight -> 0.80f
        else -> 0.58f
    },
    style = HighlightStyle.Ambient
)

/**
 * Adapta a cor de tinta (texto/ícone) sobre superfícies translúcidas Liquid Glass garantindo
 * contraste legível mínimo conforme luminância do tema ativo.
 */
@Composable
fun rememberAdaptiveLiquidGlassInk(
    preferredColor: Color,
    fallbackSurface: Color? = null,
    minContrast: Float = 4.5f
): Color {
    val theme = LocalIOS27Theme.current
    val effectiveSurface = fallbackSurface ?: theme.bgSecondary
    return remember(preferredColor, effectiveSurface, minContrast) {
        readableForeground(preferredColor, effectiveSurface, minContrast)
    }
}

/**
 * Base óptica e elástica ("gelatina" equilibrada estilo iOS 26 / Tab Bar) compartilhada por todos os
 * botões Liquid Glass do app.
 *
 * Replica exatamente o pipeline da Tab Bar (`vibrancy() => blur() => lens()` + `Highlight` + `InnerShadow`
 * + `drawOpticalGlassMeniscus`) com escala e deformação direcional aplicadas EXCLUSIVAMENTE no `layerBlock`
 * para que o conteúdo refratado atrás do botão nunca distorça de escala junto com o toque.
 */
fun Modifier.iosLiquidGlassInteractiveSurface(
    shape: Shape = ContinuousCapsule,
    interactionSource: MutableInteractionSource,
    tint: Color = Color.Unspecified,
    tintFillAlpha: Float = 0.56f,
    onDarkScrim: Boolean = false,
    elevation: Dp = 8.dp,
    maxJellyScaleBoostDp: Float = 4.5f,
    enablePointerDragStretch: Boolean = true,
    backdropOverride: Backdrop? = null
): Modifier = composed {
    val backdrop = backdropOverride ?: rememberCurrentBackdrop()
    val theme = LocalIOS27Theme.current
    val settings = LocalAppSettings.current
    val motion = LocalAppMotion.current
    val isExtremeFling = LocalLiquidGlassExtremeFling.current
    val isLight = !theme.isDark && !onDarkScrim
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressProgress by animateFloatAsState(
        targetValue = if (isPressed && !motion.reduced) 1f else 0f,
        animationSpec = motion.ios26JellyPressSpring(),
        label = "iosGlassJellyPress"
    )

    val animationScope = rememberCoroutineScope()
    val reduceMotion = com.example.ui.theme.rememberReducedMotion()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(
            animationScope = animationScope,
            reduceMotion = reduceMotion
        )
    }

    if (settings.reduceTransparency) {
        val solidBg = when {
            tint.isSpecified -> tint
            onDarkScrim -> Color(0xFF1F212B)
            else -> theme.bgSecondary
        }
        val solidBorder = when {
            tint.isSpecified -> Color.White.copy(alpha = if (theme.isDark) 0.24f else 0.34f)
            onDarkScrim -> Color.White.copy(alpha = 0.26f)
            else -> theme.divider
        }
        return@composed this
            .graphicsLayer {
                val scale = lerp(1f, 0.965f, pressProgress)
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(solidBg, shape)
            .border(0.85.dp, solidBorder, shape)
    }

    val edgeRefraction = settings.enableLensRefraction && !isExtremeFling
    val chromaticAberration = settings.enableChromaticAberration && !isExtremeFling
    val refractionMult = settings.glassIntensity.coerceIn(0.55f, 2.3f)
    val baseGlassAlpha = when {
        onDarkScrim -> (0.26f * settings.glassOpacity).coerceIn(0.16f, 0.52f)
        isLight -> (0.28f * settings.glassOpacity).coerceIn(0.14f, 0.62f)
        else -> (0.30f * settings.glassOpacity).coerceIn(0.16f, 0.66f)
    }
    val surfaceColor = when {
        onDarkScrim -> Color(0xFF12131A).copy(alpha = baseGlassAlpha)
        isLight -> Color.White.copy(alpha = baseGlassAlpha)
        else -> Color(0xFF16171F).copy(alpha = baseGlassAlpha)
    }

    this
        .drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            highlight = {
                Highlight.Default.copy(
                    alpha = lerp(
                        if (isLight) 0.78f else 0.60f,
                        if (isLight) 0.96f else 0.88f,
                        pressProgress
                    )
                )
            },
            shadow = if (elevation > 0.dp) {
                {
                    Shadow(
                        radius = elevation,
                        offset = DpOffset(0.dp, (elevation.value * 0.32f).dp),
                        color = Color.Black.copy(alpha = if (isLight) 0.13f else 0.30f)
                    )
                }
            } else {
                null
            },
            innerShadow = {
                InnerShadow(
                    radius = 5.dp,
                    alpha = lerp(if (isLight) 0.16f else 0.28f, 0.40f, pressProgress)
                )
            },
            effects = {
                vibrancy()
                val blurPx = (8f * settings.glassBlurIntensity).coerceIn(4f, 20f).dp.toPx()
                if (blurPx >= 1f && !blurPx.isNaN()) {
                    blur(blurPx)
                }
                if (edgeRefraction && size.isSpecified && size.width > 4f && size.height > 4f) {
                    val boost = 1f + 0.28f * pressProgress
                    val maxAllowedHeight = min(size.width, size.height) * 0.45f
                    val rHeight = ((14f * refractionMult * boost).dp.toPx()).coerceAtMost(maxAllowedHeight)
                    val rAmount = (28f * refractionMult * boost).dp.toPx()
                    if (rHeight >= 1f && rAmount >= 1f) {
                        lens(
                            refractionHeight = rHeight,
                            refractionAmount = rAmount,
                            depthEffect = true,
                            chromaticAberration = chromaticAberration
                        )
                    }
                }
            },
            layerBlock = if (!motion.reduced) {
                {
                    if (size.isSpecified && size.width > 0f && size.height > 0f) {
                        val width = size.width
                        val height = size.height
                        val activeProgress = maxOf(pressProgress, interactiveHighlight.pressProgress)
                        val scale = lerp(1f, 1f + maxJellyScaleBoostDp.dp.toPx() / height, activeProgress)

                        if (enablePointerDragStretch) {
                            val maxOffset = size.minDimension.coerceAtLeast(1f)
                            val offset = interactiveHighlight.offset
                            val initialDerivative = 0.055f
                            translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                            translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                            val maxDragScale = 5f.dp.toPx() / height
                            val offsetAngle = atan2(offset.y, offset.x)
                            val maxDim = size.maxDimension.coerceAtLeast(1f)
                            scaleX = scale +
                                maxDragScale * abs(cos(offsetAngle) * offset.x / maxDim) *
                                (width / height).fastCoerceAtMost(1f)
                            scaleY = scale +
                                maxDragScale * abs(sin(offsetAngle) * offset.y / maxDim) *
                                (height / width).fastCoerceAtMost(1f)
                        } else {
                            scaleX = scale
                            scaleY = scale
                        }
                    }
                }
            } else {
                null
            },
            onDrawSurface = {
                if (tint.isSpecified) {
                    drawRect(tint, blendMode = BlendMode.Hue)
                    drawRect(tint.copy(alpha = tintFillAlpha))
                } else {
                    drawRect(surfaceColor)
                }
                drawOpticalGlassMeniscus(
                    cornerRadiusPx = min(size.width, size.height) * 0.5f,
                    isLight = isLight && !tint.isSpecified,
                    edgeRefraction = edgeRefraction,
                    cyanDispersionStrength = if (tint.isSpecified) 0.42f else 0.34f
                )
            }
        )
        .then(if (enablePointerDragStretch && !motion.reduced) interactiveHighlight.modifier else Modifier)
        .then(if (enablePointerDragStretch && !motion.reduced) interactiveHighlight.gestureModifier else Modifier)
}

/**
 * Botão pílula nativo estilo iOS 26 Liquid Glass (`ContinuousCapsule`), com alvo mínimo de 48.dp,
 * refração real + física de gelatina equilibrada da Tab Bar e fallback sólido automático quando
 * `reduceTransparency` está ativo.
 *
 * - `emphasized = true`: Tinted Liquid Glass (vidro refrativo colorido via `BlendMode.Hue` + película translúcida)
 * - `destructive = true`: Destructive Tinted Liquid Glass (vidro refrativo avermelhado iOS)
 * - Padrão: Clear Liquid Glass (vidro transparente refrativo)
 */
@Composable
fun LiquidGlassPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasized: Boolean = false,
    destructive: Boolean = false,
    leadingIcon: ImageVector? = null,
    loading: Boolean = false,
    contentTint: Color? = null,
    onDarkScrim: Boolean = false,
    minHeight: Dp = 48.dp,
    horizontalPadding: Dp = 18.dp,
    verticalPadding: Dp = 12.dp,
    backdropOverride: Backdrop? = null
) {
    val theme = LocalIOS27Theme.current
    val settings = LocalAppSettings.current
    val haptics = rememberAppHaptics()
    val interactions = remember { MutableInteractionSource() }
    val shape = ContinuousCapsule

    val destructiveColor = if (theme.isDark || onDarkScrim) Color(0xFFFF453A) else Color(0xFFD9383A)
    val glassTint = when {
        emphasized -> theme.accent
        destructive && !settings.reduceTransparency -> destructiveColor
        else -> Color.Unspecified
    }
    val glassTintAlpha = when {
        emphasized -> if (theme.isDark) 0.58f else 0.64f
        destructive -> if (theme.isDark) 0.24f else 0.16f
        else -> 0f
    }

    val resolvedTint = contentTint ?: when {
        emphasized -> {
            if (contrastRatio(Color.White, theme.accent) >= contrastRatio(Color.Black, theme.accent)) Color.White else Color.Black
        }
        destructive -> readableForeground(destructiveColor, if (onDarkScrim) Color(0xFF14161D) else theme.bgSecondary, 4.5f)
        onDarkScrim -> Color.White
        else -> rememberAdaptiveLiquidGlassInk(theme.textPrimary, theme.bgSecondary, 4.5f)
    }

    Row(
        modifier = modifier
            .heightIn(min = minHeight)
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .iosLiquidGlassInteractiveSurface(
                shape = shape,
                interactionSource = interactions,
                tint = glassTint,
                tintFillAlpha = glassTintAlpha,
                onDarkScrim = onDarkScrim,
                elevation = if (emphasized) 10.dp else 6.dp,
                backdropOverride = backdropOverride
            )
            .clip(shape)
            .focusable(enabled = enabled)
            .clickable(
                enabled = enabled && !loading,
                interactionSource = interactions,
                indication = null,
                role = Role.Button,
                onClickLabel = text,
                onClick = {
                    if (destructive) haptics.impactMedium() else haptics.impactLight()
                    onClick()
                }
            )
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            IOSLoadingIndicator(
                modifier = Modifier.size(18.dp),
                color = resolvedTint
            )
        } else if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = resolvedTint,
                modifier = Modifier.size(17.dp)
            )
        }
        Text(
            text = text,
            color = resolvedTint,
            fontSize = 14.sp,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Botão circular padronizado iOS 26 Liquid Glass (Compartilhar, Fechar, Ajustes, Alternar Layout, etc.)
 * com refração viva real, física elástica ("gelatina") equilibrada e centralização óptica rigorosa do ícone.
 */
@Composable
fun LiquidGlassCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 20.dp,
    tint: Color? = null,
    glassTint: Color = Color.Unspecified,
    onDarkScrim: Boolean = false,
    enabled: Boolean = true,
    tag: String? = null,
    backdropOverride: Backdrop? = null
) {
    val theme = LocalIOS27Theme.current
    val haptics = rememberAppHaptics()
    val interactions = remember { MutableInteractionSource() }
    val resolvedTint = tint ?: when {
        glassTint.isSpecified -> Color.White
        onDarkScrim -> Color.White
        else -> rememberAdaptiveLiquidGlassInk(theme.textPrimary, theme.bgSecondary, 4.5f)
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .iosLiquidGlassInteractiveSurface(
                shape = CircleShape,
                interactionSource = interactions,
                tint = glassTint,
                tintFillAlpha = if (glassTint.isSpecified) 0.56f else 0f,
                onDarkScrim = onDarkScrim,
                elevation = 8.dp,
                maxJellyScaleBoostDp = 5.5f,
                backdropOverride = backdropOverride
            )
            .clip(CircleShape)
            .focusable(enabled = enabled)
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
            }
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .clickable(
                enabled = enabled,
                interactionSource = interactions,
                indication = null,
                role = Role.Button,
                onClickLabel = contentDescription,
                onClick = {
                    haptics.impactLight()
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = resolvedTint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Feedback tátil instantâneo no pointer-down para cards de lista, grade e painel dashboard
 * (Emil Kowalski: `scale 0.97` em 120ms com `EaseOutQuint` estritamente em `graphicsLayer`,
 * sem bounce lento nem relayout, preservando 120fps em listas longas).
 */
fun Modifier.tactileCardPress(
    interactionSource: MutableInteractionSource,
    targetScale: Float = 0.97f
): Modifier = composed {
    val motion = LocalAppMotion.current
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !motion.reduced) targetScale else 1f,
        animationSpec = motion.pressFeedbackTween(),
        label = "tactileCardPress"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Painel de vidro translúcido iOS 26 (equivalente nativo GPU de `backdrop-filter: blur(24px) saturate(150%)`)
 * ancorado a fundos de opacidade reduzida para que a atmosfera e o conteúdo atrás refratem com nitidez,
 * com fallback sólido automático para `reduceTransparency` e `isExtremeFling`.
 */
fun Modifier.liquidGlassPanelSurface(
    shape: Shape = ContinuousRoundedRectangle(26.dp),
    elevation: Dp = 6.dp,
    backdropOverride: Backdrop? = null
): Modifier = composed {
    val backdrop = backdropOverride ?: rememberCurrentBackdrop()
    val mat = LocalAppleMaterials.current
    val theme = LocalIOS27Theme.current
    val settings = LocalAppSettings.current
    val isExtremeFling = LocalLiquidGlassExtremeFling.current
    val isLight = !theme.isDark

    if (settings.reduceTransparency) {
        return@composed this
            .clip(shape)
            .background(theme.bgSecondary, shape)
            .border(0.85.dp, theme.divider, shape)
    }

    if (isExtremeFling) {
        val flingFill = if (isLight) Color.White.copy(alpha = 0.62f) else Color(0xFF16171F).copy(alpha = 0.64f)
        val flingBorder = if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.12f)
        return@composed this
            .clip(shape)
            .background(flingFill, shape)
            .border(0.75.dp, flingBorder, shape)
    }

    val panelAlpha = ((if (isLight) 0.42f else 0.36f) * settings.glassOpacity).coerceIn(0.22f, 0.72f)
    val baseSurface = if (isLight) Color.White.copy(alpha = panelAlpha) else Color(0xFF14151D).copy(alpha = panelAlpha)
    val edgeRefraction = settings.enableLensRefraction

    this
        .drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            highlight = { adaptiveGlassHighlight(isLight = isLight, reduceTransparency = false) },
            shadow = if (elevation > 0.dp) {
                {
                    Shadow(
                        radius = elevation,
                        offset = DpOffset(0.dp, (elevation.value * 0.35f).dp),
                        color = Color.Black.copy(alpha = if (isLight) 0.10f else 0.26f)
                    )
                }
            } else {
                null
            },
            innerShadow = {
                InnerShadow(
                    radius = 5.dp,
                    alpha = if (isLight) 0.12f else 0.20f
                )
            },
            effects = {
                vibrancy()
                val blurPx = (16f * settings.glassBlurIntensity).coerceIn(8f, 24f).dp.toPx()
                if (blurPx >= 1f && !blurPx.isNaN()) {
                    blur(blurPx)
                }
                if (edgeRefraction && size.isSpecified && size.width > 8f && size.height > 8f) {
                    val rHeight = ((12f * settings.glassIntensity).dp.toPx())
                        .coerceAtMost(min(size.width, size.height) * 0.28f)
                    val rAmount = (18f * settings.glassIntensity).dp.toPx()
                    if (rHeight >= 1f && rAmount >= 1f) {
                        lens(
                            refractionHeight = rHeight,
                            refractionAmount = rAmount,
                            depthEffect = true,
                            chromaticAberration = false
                        )
                    }
                }
            },
            onDrawSurface = {
                drawRect(baseSurface)
                drawOpticalGlassMeniscus(
                    cornerRadiusPx = 26.dp.toPx(),
                    isLight = isLight,
                    edgeRefraction = edgeRefraction,
                    cyanDispersionStrength = 0.18f
                )
            }
        )
        .clip(shape)
}

/**
 * Overload do `AppleTextField` com formato configurável (ex.: `ContinuousCapsule` para barras de busca iOS
 * com lupa, fundo translúcido `inputWellSurface` e botão limpar circular de 48.dp).
 */
@Composable
fun AppleTextField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    shape: Shape,
    leadingIcon: ImageVector? = null,
    useContentMaterial: Boolean = false,
    modifier: Modifier = Modifier
) {
    val mat = LocalAppleMaterials.current
    val theme = LocalIOS27Theme.current
    val settings = LocalAppSettings.current
    var focused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                when {
                    settings.reduceTransparency -> Modifier
                        .clip(shape)
                        .background(theme.bgPrimary, shape)
                        .border(1.dp, theme.divider, shape)
                    useContentMaterial -> Modifier.contentMaterialSurface(shape = shape, elevation = 4.dp)
                    else -> Modifier.inputWellSurface(shape = shape)
                }
            )
            .then(
                if (focused) Modifier.border(1.75.dp, mat.scheme.accentColor, shape)
                else Modifier
            )
            .heightIn(min = 48.dp)
            .padding(start = 16.dp, end = if (value.isNotEmpty()) 4.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (focused) mat.scheme.accentColor else mat.scheme.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    fontSize = 14.sp,
                    color = mat.scheme.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = PlusJakartaSansFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = mat.scheme.textPrimary
                ),
                cursorBrush = SolidColor(mat.scheme.accentColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .onFocusChanged { focused = it.isFocused }
            )
        }

        if (value.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClickLabel = "Limpar") { onChange("") },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(mat.scheme.textSecondary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Limpar",
                        tint = mat.scheme.textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Contêiner de diálogo flutuante estilo Apple iOS 26 Liquid Glass projetado na janela principal
 * (`LiquidGlassOverlayPortal`) para refratar o conteúdo vivo real atrás do modal, com curvatura contínua
 * squircle (`ContinuousRoundedRectangle`), realce especular, entrada elástica suave e fallback sólido
 * quando `AppSettings.reduceTransparency` está ativo.
 */
@Composable
fun LiquidGlassDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 440.dp,
    dismissOnScrimClick: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val theme = LocalIOS27Theme.current
    val settings = LocalAppSettings.current
    val motion = LocalAppMotion.current
    val isLight = !theme.isDark
    val dialogShape = remember { ContinuousRoundedRectangle(30.dp) }
    val entranceProgress = remember { Animatable(if (motion.reduced) 1f else 0f) }

    LaunchedEffect(motion.reduced) {
        if (motion.reduced) {
            entranceProgress.snapTo(1f)
        } else {
            entranceProgress.animateTo(
                targetValue = 1f,
                animationSpec = motion.ios26ModalSpring()
            )
        }
    }

    LiquidGlassOverlayPortal(onDismissRequest = onDismissRequest) { liveBackdrop ->
        val p = entranceProgress.value
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Color.Black.copy(
                            alpha = (if (settings.reduceTransparency) {
                                if (isLight) 0.42f else 0.70f
                            } else {
                                if (isLight) 0.18f else 0.42f
                            }) * p.coerceIn(0f, 1f)
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = dismissOnScrimClick,
                        onClick = onDismissRequest
                    )
            )
            val cardModifier = if (settings.reduceTransparency) {
                Modifier
                    .clip(dialogShape)
                    .background(theme.bgSecondary, dialogShape)
                    .border(1.dp, theme.divider, dialogShape)
            } else {
                Modifier
                    .drawBackdrop(
                        backdrop = liveBackdrop,
                        shape = { dialogShape },
                        highlight = { adaptiveGlassHighlight(isLight = isLight, reduceTransparency = false) },
                        shadow = {
                            Shadow(
                                radius = 26.dp,
                                offset = DpOffset(0.dp, 12.dp),
                                color = Color.Black.copy(alpha = if (isLight) 0.16f else 0.40f)
                            )
                        },
                        innerShadow = {
                            InnerShadow(
                                radius = 6.dp,
                                alpha = if (isLight) 0.14f else 0.24f
                            )
                        },
                        effects = {
                            vibrancy()
                            val blurPx = (18f * settings.glassBlurIntensity).coerceIn(8f, 26f).dp.toPx()
                            if (blurPx >= 1f) blur(blurPx)
                            if (settings.enableLensRefraction && size.isSpecified && size.width > 4f && size.height > 4f) {
                                val rHeight = ((18f * settings.glassIntensity).dp.toPx())
                                    .coerceAtMost(min(size.width, size.height) * 0.40f)
                                val rAmount = (30f * settings.glassIntensity).dp.toPx()
                                if (rHeight >= 1f && rAmount >= 1f) {
                                    lens(
                                        refractionHeight = rHeight,
                                        refractionAmount = rAmount,
                                        depthEffect = true,
                                        chromaticAberration = settings.enableChromaticAberration
                                    )
                                }
                            }
                        },
                        onDrawSurface = {
                            val base = if (isLight) Color.White else Color(0xFF15161D)
                            val alpha = ((if (isLight) 0.56f else 0.52f) * settings.glassOpacity).coerceIn(0.36f, 0.82f)
                            drawRect(base.copy(alpha = alpha))
                            drawOpticalGlassMeniscus(
                                cornerRadiusPx = 30.dp.toPx(),
                                isLight = isLight,
                                edgeRefraction = settings.enableLensRefraction,
                                cyanDispersionStrength = 0.22f
                            )
                        }
                    )
                    .clip(dialogShape)
            }

            Box(
                modifier = modifier
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .fillMaxWidth()
                    .widthIn(max = maxWidth)
                    .graphicsLayer {
                        val s = if (motion.reduced) 1f else lerp(0.93f, 1f, p)
                        scaleX = s
                        scaleY = s
                        alpha = p.coerceIn(0f, 1f)
                    }
                    .then(cardModifier)
                    .pointerInput(Unit) { detectTapGestures {} }
            ) {
                CompositionLocalProvider(LocalLiquidGlassBackdrop provides liveBackdrop) {
                    content()
                }
            }
        }
    }
}
