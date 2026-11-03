package com.example.ui.components

import androidx.compose.ui.platform.testTag

import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.kyant.backdrop.catalog.utils.inspectDragGestures

import androidx.compose.foundation.layout.heightIn

import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.catalog.legacy.DampedDragAnimation
import com.kyant.backdrop.catalog.utils.InteractiveHighlight
import androidx.compose.runtime.derivedStateOf
import androidx.compose.animation.core.EaseOut
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.tanh
import com.kyant.shapes.Capsule
import com.kyant.shapes.ContinuousCapsule
import com.kyant.shapes.RoundedRectangle
import com.kyant.shapes.ContinuousRoundedRectangle
import kotlin.math.min

import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.util.Base64
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.backdrop.shadow.shadow
import com.kyant.backdrop.shadow.InnerShadow
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.model.AppleAvatarPreset
import com.example.model.ReaderProfile
import com.example.ui.theme.LocalAppleMaterials
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.LocalThemeColors
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.SpaceGroteskTabularFamily
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * CompositionLocal que armazena o `Backdrop` principal da tela (`io.github.kyant0.backdrop.Backdrop`).
 */
val LocalLiquidGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * CompositionLocal que armazena a `GraphicsLayer` contendo o conteúdo de fundo (SculpturalWaveBackdrop + abas com capas de HQs).
 */
val LocalLiquidGlassBackdropLayer = staticCompositionLocalOf<GraphicsLayer?> { null }

/**
 * Posição global (Root) da origem da `LocalLiquidGlassBackdropLayer` ativa (padrão `Offset.Zero`).
 */
val LocalLiquidGlassBackdropOrigin = compositionLocalOf { Offset.Zero }

/**
 * Camada de iluminação ambiente opcional (mantida por compatibilidade).
 */
val LocalLiquidGlassAmbientBackdropLayer = staticCompositionLocalOf<GraphicsLayer?> { null }

/**
 * Estado de rolagem da lista ativa (`LazyListState`) lido EXCLUSIVAMENTE dentro da fase de Draw
 * para invalidar apenas a renderização na GPU (zero recomposições).
 */
val LocalLiquidGlassScrollState = compositionLocalOf<LazyListState?> { null }

/**
 * Contador opcional de invalidação (usa `compositionLocalOf` para nunca recompor a árvore inteira).
 */
val LocalLiquidGlassScrollTick = compositionLocalOf { 0 }

/**
 * Indica se a lista ativa está em inércia extrema de rolagem rápida (fling).
 * Quando ativo, congela o snapshot raster das camadas decorativas de fundo e desativa
 * temporariamente a refração em tempo real para manter 120 FPS cravados.
 */
val LocalLiquidGlassExtremeFling = compositionLocalOf { false }

/**
 * Guarda de escopo para impedir qualquer chamada aninhada a `GraphicsLayer.record` no mesmo frame,
 * prevenindo crashes nativos de RenderNode no HWUI do Android.
 */
object LiquidGlassBackdropScope {
    private var activeRecordingDepth: Int = 0

    fun beginRecording(layer: GraphicsLayer) {
        activeRecordingDepth++
    }

    fun endRecording(layer: GraphicsLayer) {
        activeRecordingDepth = (activeRecordingDepth - 1).coerceAtLeast(0)
    }

    fun isRecording(layer: GraphicsLayer): Boolean = activeRecordingDepth > 0

    fun isRecordingAny(): Boolean = activeRecordingDepth > 0
}


/**
 * Resolve o `Backdrop` ativo na árvore Compose (`LocalLiquidGlassBackdrop` ou fallback seguro `EmptyBackdrop`).
 */
@Composable
fun rememberCurrentBackdrop(): Backdrop {
    val explicitBackdrop = LocalLiquidGlassBackdrop.current
    return explicitBackdrop ?: emptyBackdrop()
}

/**
 * Cria o pipeline nativo de RenderEffect do Apple Liquid Glass para API 31+ seguindo estritamente:
 * 1. Color Filter (`vibrancy` / `colorControls`)
 * 2. Blur (`createBlurEffect`)
 * 3. Lens (`SdfShader` AGSL com `depthEffect` e `chromaticAberration`)
 */
@RequiresApi(Build.VERSION_CODES.S)
fun createAppleLiquidGlassRenderEffect(
    radiusX: Float,
    radiusY: Float,
    saturation: Float = 1.85f,
    width: Float = 0f,
    height: Float = 0f,
    cornerRadiusPx: Float = 0f,
    enableEdgeRefraction: Boolean = true
): AndroidRenderEffect {
    return SdfShader.createSdfLiquidGlassRenderEffect(
        radiusX = radiusX,
        radiusY = radiusY,
        saturation = saturation,
        width = width,
        height = height,
        cornerRadiusPx = cornerRadiusPx,
        refractionHeightPx = 20f,
        refractionAmountPx = 24f,
        chromaticAberration = 0.55f,
        lightAngleRad = (-Math.PI / 4.0).toFloat(),
        highlightStrength = 0.20f,
        enableEdgeRefraction = enableEdgeRefraction
    )
}

/**
 * Ponte `.realLiquidGlassBackdrop` que delega diretamente ao `.drawBackdrop` da biblioteca CMP Backdrop,
 * mantendo a sequência estrita: `vibrancy()` ⇒ `blur()` ⇒ `lens()`.
 */
@Composable
fun Modifier.realLiquidGlassBackdrop(
    shape: Shape = Capsule(),
    blurRadius: Float = 32f
): Modifier {
    return this.liquidGlass(
        radiusX = blurRadius,
        radiusY = blurRadius,
        shape = shape
    )
}

@Composable
fun Modifier.liquidGlass(
    radiusX: Float = 30f,
    radiusY: Float = 30f,
    shape: Shape = Capsule(),
    elevation: Dp = 0.dp,
    surfaceAlpha: Float? = null,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null
): Modifier {
    val backdrop = rememberCurrentBackdrop()
    val mat = LocalAppleMaterials.current
    val settings = com.example.ui.theme.LocalAppSettings.current
    val isExtremeFling = LocalLiquidGlassExtremeFling.current
    val isLight = mat.scheme.isLight
    val edgeRefraction = mat.edgeRefraction && !settings.reduceTransparency && settings.enableLensRefraction && !isExtremeFling
    val chromaticAberration = !settings.reduceTransparency && settings.enableChromaticAberration && !isExtremeFling
    val refractionMult = settings.glassIntensity.coerceIn(0.45f, 2.5f)
    val baseAlpha = surfaceAlpha ?: if (settings.reduceTransparency) {
        if (isLight) 0.78f else 0.80f
    } else {
        ((if (isLight) 0.22f else 0.28f) * settings.glassOpacity).coerceIn(0.08f, 0.72f)
    }
    val surfaceColor = if (isLight) {
        Color.White.copy(alpha = baseAlpha)
    } else {
        Color(0xFF141418).copy(alpha = baseAlpha)
    }

    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        highlight = {
            Highlight.Default.copy(alpha = if (isLight) 0.82f else 0.64f)
        },
        shadow = if (elevation > 0.dp) {
            {
                Shadow(
                    radius = elevation,
                    offset = DpOffset(0.dp, (elevation.value * 0.35f).dp),
                    color = Color.Black.copy(alpha = if (isLight) 0.14f else 0.32f)
                )
            }
        } else {
            null
        },
        innerShadow = {
            InnerShadow(
                radius = 5.dp,
                alpha = if (isLight) 0.18f else 0.30f
            )
        },
        effects = {
            if (!settings.reduceTransparency) {
                vibrancy()
            }
            val blurPx = if (settings.reduceTransparency) {
                24f.dp.toPx()
            } else {
                ((radiusX * 0.16f) * settings.glassBlurIntensity).coerceIn(2f, 14f).dp.toPx()
            }
            if (blurPx >= 1f && !blurPx.isNaN()) {
                blur(blurPx)
            }
            if (edgeRefraction && size.isSpecified && size.width > 4f && size.height > 4f) {
                val maxAllowedHeight = min(size.width, size.height) * 0.46f
                val rHeight = ((16f * refractionMult).dp.toPx()).coerceAtMost(maxAllowedHeight)
                val rAmount = (32f * refractionMult).dp.toPx()
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
        layerBlock = layerBlock,
        onDrawSurface = {
            drawRect(surfaceColor)
        }
    )
}

@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.liquidGlass(shape = shape, elevation = 6.dp),
        content = content
    )
}

@Composable
fun AppleIslandCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.liquidGlass(shape = shape, elevation = 6.dp),
        content = content
    )
}

@Composable
fun LiquidGlassChip(
    modifier: Modifier = Modifier,
    shape: Shape = Capsule(),
    content: @Composable BoxScope.() -> Unit
) {
    val motion by androidx.compose.runtime.rememberUpdatedState(com.example.ui.theme.LocalAppMotion.current)
    Box(
        modifier = modifier.liquidGlass(shape = shape, elevation = 4.dp),
        content = content
    )
}

/**
 * 2. COMPONENTE DA BARRA DE NAVEGAÇÃO INFERIOR INTERATIVA (`Interactive Glass Bottom Bar` do manual CMP Backdrop):
 * - Contêiner configurado com `.drawBackdrop` e a forma contínua `com.kyant.shapes.Capsule()`.
 * - Animação elástica ao toque ('Press to Scale') via `Animatable(0f)` controlada por `motion.spring(0.5f, 300f, 0.001f)`.
 * - Escala aplicada EXCLUSIVAMENTE dentro do `layerBlock` do `drawBackdrop` para impedir que o fundo mude de escala junto com o botão.
 * - Filtro de legibilidade: `onDrawSurface = { drawRect(Color.White.copy(alpha = 0.5f)) }`.
 */
@Composable
fun Modifier.interactiveGlassBottomBarSurface(
    backdrop: Backdrop = rememberCurrentBackdrop(),
    pressProgress: Float = 0f,
    maxScale: Float = 1.06f
): Modifier {
    val motion by androidx.compose.runtime.rememberUpdatedState(com.example.ui.theme.LocalAppMotion.current)
    val mat = LocalAppleMaterials.current
    val isLight = mat.scheme.isLight
    val edgeRefraction = mat.edgeRefraction

    return this
        .drawBackdrop(
            backdrop = backdrop,
            shape = { Capsule() },
            effects = {
                // ESTRITAMENTE: 1. vibrancy => 2. blur => 3. lens
                vibrancy()
                blur(12f.dp.toPx())
                lens(
                    refractionHeight = 16f.dp.toPx(),
                    refractionAmount = 32f.dp.toPx(),
                    depthEffect = true,
                    chromaticAberration = edgeRefraction
                )
            },
            layerBlock = {
                val scale = lerp(1f, maxScale, pressProgress)
                scaleX = scale
                scaleY = scale
            },
            onDrawSurface = {
                if (isLight) {
                    drawRect(Color.White.copy(alpha = 0.5f))
                } else {
                    drawRect(Color.White.copy(alpha = 0.5f), blendMode = BlendMode.Softlight)
                    drawRect(mat.liquidGlassNavFill.copy(alpha = 0.42f))
                }
                drawOpticalGlassMeniscus(
                    cornerRadiusPx = size.height * 0.5f,
                    isLight = isLight,
                    edgeRefraction = edgeRefraction,
                    cyanDispersionStrength = 0.45f
                )
            }
        )
}

/**
 * 3. BOTÃO DA LUPA AZUL MATIZADO (`Tinted Liquid Button` do manual CMP Backdrop):
 * - Usa `BlendMode.Hue` combinado com o preenchimento translúcido de azul `Color(0xFF0088FF)` no `onDrawSurface`
 *   para fundir o matiz do vidro organicamente ao conteúdo das capas de quadrinhos que passarem atrás.
 * - Escala elástica ('Press to Scale') aplicada EXCLUSIVAMENTE no `layerBlock` via `Animatable(0f)` com `motion.spring(0.5f, 300f, 0.001f)`.
 */
@Composable
fun Modifier.tintedLiquidBlueButton(
    backdrop: Backdrop = rememberCurrentBackdrop(),
    pressProgress: Float = 0f,
    isActive: Boolean = false
): Modifier {
    return this
        .drawBackdrop(
            backdrop = backdrop,
            shape = { Capsule() },
            effects = {
                // ESTRITAMENTE: 1. vibrancy => 2. blur => 3. lens
                vibrancy()
                blur(8f.dp.toPx())
                lens(
                    refractionHeight = 14f.dp.toPx(),
                    refractionAmount = 28f.dp.toPx(),
                    depthEffect = true,
                    chromaticAberration = true
                )
            },
            layerBlock = {
                val scale = lerp(1f, 1.12f, pressProgress)
                scaleX = scale
                scaleY = scale
            },
            onDrawSurface = {
                drawRect(Color(0xFF0088FF), blendMode = BlendMode.Hue)
                drawRect(Color(0xFF0088FF).copy(alpha = if (isActive) 0.58f else 0.42f))
                drawOpticalGlassMeniscus(
                    cornerRadiusPx = size.height * 0.5f,
                    isLight = false,
                    edgeRefraction = true,
                    cyanDispersionStrength = 0.52f
                )
            }
        )
}

/**
 * 5. COMPONENTE DO MENU DE REGISTRO DE QUADRINHOS (`Glass Registration Sheet`):
 * - Aplica `.drawBackdrop` / `.liquidGlass` com formato contínuo G2 `com.kyant.shapes.RoundedRectangle(24f.dp)`.
 * - Configura `onDrawSurface` com opacidade mais densa (`drawRect(Color.White.copy(alpha = 0.65f))` no tema claro,
 *   ou a cor correspondente do tema escuro com `alpha = 0.65f`) para garantir contraste perfeito na digitação.
 * - Sem `.background()` opacos nem elevações redundantes.
 */
@Composable
fun Modifier.liquidGlassRegistrationSheet(
    backdrop: Backdrop = rememberCurrentBackdrop(),
    cornerRadiusDp: Float = 24f
): Modifier {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme
    val isLight = s.isLight
    val sheetShape = remember(cornerRadiusDp) { RoundedRectangle(cornerRadiusDp.dp) }
    val density = LocalDensity.current

    return this
        .drawBackdrop(
            backdrop = backdrop,
            shape = { sheetShape },
            effects = {
                vibrancy()
                with(density) {
                    val blurPx = 16f.dp.toPx()
                    val rHeight = 12f.dp.toPx()
                    val rAmount = 22f.dp.toPx()
                    if (blurPx >= 1f) {
                        blur(blurPx)
                    }
                    if (rHeight >= 1f && rAmount >= 1f) {
                        lens(
                            refractionHeight = rHeight,
                            refractionAmount = rAmount,
                            depthEffect = true,
                            chromaticAberration = mat.edgeRefraction
                        )
                    }
                }
            },
            onDrawSurface = {
                if (isLight) {
                    drawRect(Color.White.copy(alpha = 0.58f))
                } else {
                    drawRect(Color(0xFF0E0F14).copy(alpha = 0.52f))
                }
            }
        )
}

/**
 * Camada de substrato de desfoque dinâmico e saturação cromática (Vibrancy ⇒ Blur ⇒ Lens)
 * na árvore de renderização usando `.drawBackdrop`.
 */
@Composable
fun Modifier.liquidGlassBackdropBlurLayer(
    shape: Shape = Capsule(),
    radiusX: Float = 30f,
    radiusY: Float = 30f
): Modifier {
    val backdrop = rememberCurrentBackdrop()
    val mat = LocalAppleMaterials.current
    val edgeRefraction = mat.edgeRefraction
    val density = LocalDensity.current

    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            val bRadius = radiusX.coerceAtLeast(4f)
            if (bRadius >= 1f) {
                blur(bRadius)
            }
            with(density) {
                val rHeight = 16f.dp.toPx()
                val rAmount = 28f.dp.toPx()
                if (rHeight >= 1f && rAmount >= 1f) {
                    lens(
                        refractionHeight = rHeight,
                        refractionAmount = rAmount,
                        depthEffect = true,
                        chromaticAberration = edgeRefraction
                    )
                }
            }
        }
    )
}

/**
 * Desenha o menisco óptico especular de Liquid Glass com bordas suaves e delicadas,
 * simulando refração prismática sutil, relevo 2.5D e iluminação especular Fresnel
 * concentrada próxima ao ponto de toque (`touchOffset`) quando `touchGlowAlpha > 0f`.
 */
fun DrawScope.drawOpticalGlassMeniscus(
    cornerRadiusPx: Float,
    isLight: Boolean,
    edgeRefraction: Boolean = true,
    cyanDispersionStrength: Float = 0.45f,
    leftCausticColor: Color = Color.Transparent,
    touchOffset: Offset = Offset.Zero,
    touchGlowAlpha: Float = 0f
) {
    val w = size.width
    val h = size.height
    if (w <= 2f || h <= 2f) return
    val cr = min(cornerRadiusPx, min(w, h) / 2f)

    val reflexLight = if (isLight) 0.72f else 0.28f
    val reflexDark = if (isLight) 0.85f else 1.45f

    // 1. Reflexo especular vertical suave (nunca estoura em branco forte)
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = (0.14f * reflexLight).coerceIn(0f, 0.22f)),
                Color.Transparent,
                Color.Black.copy(alpha = (0.06f * reflexDark).coerceIn(0f, 0.14f)),
                Color.White.copy(alpha = (0.08f * reflexLight).coerceIn(0f, 0.14f))
            )
        ),
        topLeft = Offset.Zero,
        size = Size(w, h),
        cornerRadius = CornerRadius(cr, cr)
    )

    // 2. Cáustica lateral esquerda (para sliders e switches)
    if (leftCausticColor.alpha > 0.01f) {
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    leftCausticColor.copy(alpha = leftCausticColor.alpha * 0.44f),
                    leftCausticColor.copy(alpha = leftCausticColor.alpha * 0.12f),
                    Color.Transparent
                ),
                startX = 0f,
                endX = w * 0.48f
            ),
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = CornerRadius(cr, cr)
        )
    }

    // 3. Profundidade de menisco côncavo interno e dispersão cromática suave ciano/azul
    if (edgeRefraction && cyanDispersionStrength > 0.01f) {
        val prismInset = 1.2.dp.toPx()
        val prismW = (w - prismInset * 2f).coerceAtLeast(1f)
        val prismH = (h - prismInset * 2f).coerceAtLeast(1f)
        val prismCr = (cr - prismInset).coerceAtLeast(1f)

        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = (0.24f * reflexLight).coerceIn(0.05f, 0.28f)),
                    Color(0xFF38BDF8).copy(alpha = (cyanDispersionStrength * 0.24f).coerceIn(0f, 0.28f)),
                    Color.Transparent,
                    Color(0xFF34D399).copy(alpha = (cyanDispersionStrength * 0.16f).coerceIn(0f, 0.20f)),
                    Color(0xFF64D2FF).copy(alpha = (cyanDispersionStrength * 0.26f).coerceIn(0f, 0.30f))
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            ),
            topLeft = Offset(prismInset, prismInset),
            size = Size(prismW, prismH),
            cornerRadius = CornerRadius(prismCr, prismCr),
            style = Stroke(width = 1.1.dp.toPx())
        )
    }

    // 4. Borda Especular Fresnel 2.5D diagonal (Top-Left Color.White.copy(alpha = 0.30f) -> Bottom-Right Color.Transparent)
    val outerStroke = 1.2.dp.toPx()
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = if (isLight) 0.30f else 0.24f),
                Color.White.copy(alpha = if (isLight) 0.10f else 0.07f),
                Color.Transparent
            ),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        ),
        topLeft = Offset(outerStroke / 2f, outerStroke / 2f),
        size = Size((w - outerStroke).coerceAtLeast(1f), (h - outerStroke).coerceAtLeast(1f)),
        cornerRadius = CornerRadius(cr, cr),
        style = Stroke(width = outerStroke)
    )

    // 5. Iluminação especular na borda Fresnel próxima ao ponto de toque (touchOffset)
    if (touchGlowAlpha > 0.005f) {
        val highlightRadius = (maxOf(w, h) * 0.55f).coerceAtLeast(36f)
        val clampedCenter = Offset(
            x = touchOffset.x.coerceIn(0f, w),
            y = touchOffset.y.coerceIn(0f, h)
        )
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = (touchGlowAlpha * 2.1f).coerceIn(0f, 0.62f)),
                    Color(0xFF64D2FF).copy(alpha = (touchGlowAlpha * 0.95f).coerceIn(0f, 0.28f)),
                    Color.Transparent
                ),
                center = clampedCenter,
                radius = highlightRadius
            ),
            topLeft = Offset(outerStroke / 2f, outerStroke / 2f),
            size = Size((w - outerStroke).coerceAtLeast(1f), (h - outerStroke).coerceAtLeast(1f)),
            cornerRadius = CornerRadius(cr, cr),
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

/**
 * Superfície Liquid Glass para containers com conteúdo interno direto (botões de navegação, pílulas, drawers),
 * usando `.drawBackdrop` com pipeline `vibrancy() => blur() => lens()` e sem `.background()` opacos.
 */
@Composable
fun Modifier.liquidGlassNavSurface(
    shape: Shape = Capsule(),
    elevation: Dp = 0.dp
): Modifier {
    return this.liquidGlass(
        radiusX = 28f,
        radiusY = 28f,
        shape = shape,
        elevation = elevation
    )
}

/**
 * Content Layer Material modifier adaptado ao tema ativo (`LocalAppleMaterials.current`),
 * usando cantos suaves contínuos G2 (`com.kyant.shapes.RoundedRectangle`) e sem elevações redundantes.
 */
@Composable
fun Modifier.contentMaterialSurface(
    shape: Shape = RoundedRectangle(24.dp),
    elevation: Dp = 0.dp
): Modifier {
    val mat = LocalAppleMaterials.current
    val isLight = mat.scheme.isLight
    val baseFill = mat.contentMaterialFill

    return this
        .clip(shape)
        .background(baseFill, shape)
        .background(
            brush = Brush.verticalGradient(
                colors = if (isLight) {
                    listOf(
                        Color.White.copy(alpha = 0.10f),
                        Color.White.copy(alpha = 0.08f)
                    )
                } else {
                    listOf(
                        Color.White.copy(alpha = 0.025f),
                        Color.White.copy(alpha = 0.02f)
                    )
                }
            ),
            shape = shape
        )
        .border(
            width = 0.75.dp,
            brush = Brush.linearGradient(
                colors = if (isLight) {
                    listOf(
                        Color.Black.copy(alpha = 0.08f),
                        Color.Black.copy(alpha = 0.06f),
                        Color.White.copy(alpha = 0.25f)
                    )
                } else {
                    listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.06f),
                        Color.White.copy(alpha = 0.03f)
                    )
                }
            ),
            shape = shape
        )
}

/**
 * Input Well Material modifier adaptado ao tema ativo (translúcido estilo iOS para integrar a luz de fundo).
 */
@Composable
fun Modifier.inputWellSurface(
    shape: Shape = RoundedCornerShape(16.dp)
): Modifier {
    val mat = LocalAppleMaterials.current
    val isLight = mat.scheme.isLight
    return this
        .clip(shape)
        .background(
            if (isLight) Color.White.copy(alpha = 0.72f)
            else Color.White.copy(alpha = 0.08f),
            shape
        )
        .border(
            width = 0.6.dp,
            brush = Brush.linearGradient(
                colors = if (isLight) {
                    listOf(
                        Color.Black.copy(alpha = 0.12f),
                        Color.Black.copy(alpha = 0.05f)
                    )
                } else {
                    listOf(
                        Color.White.copy(alpha = 0.16f),
                        Color.White.copy(alpha = 0.05f)
                    )
                }
            ),
            shape = shape
        )
}

private val BookSpineGrooveBrush = Brush.horizontalGradient(
    0.00f to Color.White.copy(alpha = 0.42f),
    0.12f to Color.White.copy(alpha = 0.12f),
    0.13f to Color.White.copy(alpha = 0.08f),
    0.37f to Color.Black.copy(alpha = 0.18f),
    0.38f to Color.Black.copy(alpha = 0.48f),
    0.50f to Color.Black.copy(alpha = 0.28f),
    0.51f to Color.White.copy(alpha = 0.34f),
    0.70f to Color.White.copy(alpha = 0.05f),
    1.00f to Color.Transparent
)

private val BookCoverFallbackBackgroundBrush = Brush.linearGradient(
    colors = listOf(Color(0xFF262936), Color(0xFF15171F))
)

private val BookCoverFallbackSpineBrush = Brush.horizontalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.14f),
        Color.Black.copy(alpha = 0.42f),
        Color.White.copy(alpha = 0.10f)
    )
)

private val BookCoverGlossBrush = Brush.linearGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.10f),
        Color.Transparent,
        Color.Black.copy(alpha = 0.20f)
    ),
    start = Offset(0f, 0f),
    end = Offset(300f, 450f)
)

/**
 * 5. BookCover (Capa Editorial 2:3 com fallback tipográfico #1A1B20 e lombada 3D Apple Books).
 */
@Composable
fun BookCover(
    src: String,
    title: String,
    authors: String = "",
    publisher: String = "",
    year: Int? = null,
    spine3d: Boolean = true,
    modifier: Modifier = Modifier,
    overlayContent: @Composable BoxScope.() -> Unit = {}
) {
    if (LocalHomeSkeleton.current != null) { HomeSkeletonBlock(modifier); return }

    val motion by androidx.compose.runtime.rememberUpdatedState(com.example.ui.theme.LocalAppMotion.current)
    val cleanSrc = remember(src, title) {
        val raw = src.trim().replace("http://", "https://")
        if (raw.isEmpty() || raw.contains("covers.openlibrary.org")) {
            com.example.data.CuratedCatalog.findCoverUrlByTitle(title) ?: raw
        } else {
            raw
        }
    }
    var hasFailed by remember(cleanSrc) { mutableStateOf(false) }
    var isImageLoaded by remember(cleanSrc) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .shadow(3.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1B20))
            .border(0.5.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
    ) {
        // Fallback editorial cover with authentic hardcover spine & typography (skipped once image covers the surface)
        if (!isImageLoaded || hasFailed || cleanSrc.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BookCoverFallbackBackgroundBrush)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(12.dp)
                        .background(BookCoverFallbackSpineBrush)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 9.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = (publisher.ifBlank { "EDIÇÃO" }).uppercase(),
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.1.sp,
                        color = Color.White.copy(alpha = 0.48f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.92f),
                        lineHeight = 13.5.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(Color.White.copy(alpha = 0.32f))
                        )
                        if (authors.isNotBlank()) {
                            Text(
                                text = authors,
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.45f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            if (cleanSrc.isNotEmpty() && !hasFailed && !isImageLoaded) {
                CoverShimmerPlaceholder(modifier = Modifier.fillMaxSize())
            }
        }

        if (cleanSrc.isNotEmpty() && !hasFailed) {
            val context = LocalContext.current
            val imageRequest = remember(cleanSrc, context) {
                ImageRequest.Builder(context)
                    .data(cleanSrc)
                    .size(240, 360)
                    .precision(coil.size.Precision.INEXACT)
                    .crossfade(false)
                    .memoryCacheKey(cleanSrc)
                    .diskCacheKey(cleanSrc)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .allowHardware(true)
                    .allowRgb565(true)
                    .build()
            }
            val coverAlpha by animateFloatAsState(
                targetValue = if (isImageLoaded) 1f else 0f,
                animationSpec = motion.tween(durationMillis = 280, easing = EaseOut),
                label = "bookCoverSoftFadeIn"
            )
            AsyncImage(
                model = imageRequest,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                onSuccess = { isImageLoaded = true },
                onError = { hasFailed = true },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = coverAlpha }
            )
        }

        // Authentic Apple Books 3D Hardcover Spine Hinge & Groove (single-pass GPU cached brush)
        if (spine3d) {
            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(13.dp)
            ) {
                drawRect(
                    brush = BookSpineGrooveBrush,
                    size = size
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BookCoverGlossBrush)
        )

        overlayContent()
    }
}

/**
 * Shimmer translúcido com gradiente derivado da paleta do tema ativo e saturação suave,
 * exibido enquanto as capas de alta resolução são baixadas pela rede.
 */
@Composable
private fun CoverShimmerPlaceholder(
    modifier: Modifier = Modifier
) {
    val motion by androidx.compose.runtime.rememberUpdatedState(com.example.ui.theme.LocalAppMotion.current)
    val themeColors = com.example.ui.theme.LocalIOS27Theme.current
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "coverShimmerTransition")
    val shimmerProgress = if (motion.reduced) 0f else transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = motion.tween(
                durationMillis = 1350,
                easing = androidx.compose.animation.core.LinearEasing
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "coverShimmerSweep"
    ).value

    // Cores derivadas do tema com saturação suavizada (blend translúcido com branco editorial)
    val softPrimaryTint = remember(themeColors.accent) {
        androidx.compose.ui.graphics.lerp(themeColors.accent, Color.White, 0.42f).copy(alpha = 0.16f)
    }
    val softSecondaryTint = remember(themeColors.accentSecondary) {
        androidx.compose.ui.graphics.lerp(themeColors.accentSecondary, Color.White, 0.50f).copy(alpha = 0.24f)
    }
    val ambientBaseTint = remember(themeColors.accent) {
        themeColors.accent.copy(alpha = 0.05f)
    }

    Canvas(modifier = modifier) {
        val w = size.width.coerceAtLeast(1f)
        val h = size.height.coerceAtLeast(1f)
        val bandWidth = w * 0.85f
        val startX = shimmerProgress * w

        // Camada base translúcida com saturação suave do tema
        drawRect(color = ambientBaseTint)

        // Feixe diagonal translúcido derivado da paleta do tema
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    softPrimaryTint,
                    softSecondaryTint,
                    softPrimaryTint,
                    Color.Transparent
                ),
                start = Offset(startX - bandWidth, h * 0.15f),
                end = Offset(startX + bandWidth, h * 0.85f)
            )
        )
    }
}

/**
 * UserAvatar (Supports custom Base64/URL photo or 6 Apple cromáticas spheres).
 */
@Composable
fun UserAvatar(
    profile: ReaderProfile,
    sizeDp: Int = 40,
    modifier: Modifier = Modifier
) {
    if (LocalHomeSkeleton.current != null) { HomeSkeletonBlock(modifier.size(sizeDp.dp).clip(CircleShape)); return }

    val themeColors = LocalIOS27Theme.current
    val initial = (profile.name.trim().firstOrNull()?.uppercaseChar() ?: 'F').toString()

    val decodedBitmap = remember(profile.avatarUrl) {
        val raw = profile.avatarUrl.trim()
        if (raw.startsWith("data:image")) {
            try {
                val base64Part = raw.substringAfter("base64,", "")
                val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (_: Throwable) {
                null
            }
        } else null
    }

    val preset = AppleAvatarPreset.fromId(profile.avatarPreset)
    val startColor = if (preset == AppleAvatarPreset.THEME) themeColors.accent else preset.startColor
    val endColor = if (preset == AppleAvatarPreset.THEME) themeColors.accentSecondary else preset.endColor
    val avgLuminance = (
        (0.2126f * startColor.red + 0.7152f * startColor.green + 0.0722f * startColor.blue) +
            (0.2126f * endColor.red + 0.7152f * endColor.green + 0.0722f * endColor.blue)
        ) * 0.5f
    val isLightBadge = avgLuminance > 0.55f
    val letterColor = if (isLightBadge) {
        androidx.compose.ui.graphics.lerp(Color(0xFF0A0A0E), endColor, 0.22f)
    } else {
        androidx.compose.ui.graphics.lerp(Color.White, startColor, 0.14f)
    }
    val borderColor = if (isLightBadge) {
        Color.Black.copy(alpha = 0.18f)
    } else {
        Color.White.copy(alpha = 0.35f)
    }

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(startColor, endColor)
                )
            )
            .border(1.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (decodedBitmap != null) {
            Image(
                bitmap = decodedBitmap,
                contentDescription = profile.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (profile.avatarUrl.startsWith("http")) {
            AsyncImage(
                model = profile.avatarUrl,
                contentDescription = profile.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initial,
                fontSize = (sizeDp * 0.42f).sp,
                fontWeight = FontWeight.Bold,
                color = letterColor
            )
        }
    }
}

/**
 * 5.1 AppleToggle (Integrado com `DampedDragAnimation` e `InteractiveHighlight` do AndroidLiquidGlass-kmp):
 * - Quando clica na parte vazia da trilha: desliza suavemente com amortecimento elástico.
 * - Quando clica ou arrasta o botão (thumb): expande na lente de Liquid Glass refrativa com deformação
 *   elástica anisotrópica (`dampedDrag.scaleX`, `dampedDrag.scaleY`) e brilho especular interativo (`InteractiveHighlight`).
 */
@Composable
fun AppleToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit = {},
    onChange: (Boolean) -> Unit = onCheckedChange,
    modifier: Modifier = Modifier,
    activeColor: Color = LocalIOS27Theme.current.accent,
    size: String = "md",
    forceLensPreview: Boolean = false
) {
    val backdrop = LocalLiquidGlassBackdrop.current ?: rememberLayerBackdrop()
    com.kyant.backdrop.catalog.components.LiquidToggle(
        selected = { checked },
        onSelect = { v ->
            onChange(v)
        },
        backdrop = backdrop,
        modifier = modifier,
    )
}

@Composable
fun AppleToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val backdrop = LocalLiquidGlassBackdrop.current ?: rememberLayerBackdrop()
    val themeColors = LocalIOS27Theme.current
    com.kyant.backdrop.catalog.components.LiquidToggle(
        selected = { checked },
        onSelect = onCheckedChange,
        backdrop = backdrop,
        accentColor = themeColors.accent,
        modifier = modifier
    )
}

@Composable
fun AppleLiquidGlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    visibilityThreshold: Float = 0.005f
) {
    val backdrop = LocalLiquidGlassBackdrop.current ?: rememberLayerBackdrop()
    val themeColors = LocalIOS27Theme.current
    com.kyant.backdrop.catalog.components.LiquidSlider(
        value = { value },
        onValueChange = onValueChange,
        valueRange = valueRange,
        visibilityThreshold = visibilityThreshold,
        backdrop = backdrop,
        accentColor = themeColors.accent,
        modifier = modifier
    )
}

/**
 * 4. CONTROLE DE PROGRESSO EM VIDRO (GlassSlider com Lente Multi-Camada do AndroidLiquidGlass-kmp).
 */
@Composable
fun GlassSlider(
    backdrop: Backdrop = LocalLiquidGlassBackdrop.current ?: rememberLayerBackdrop(),
    modifier: Modifier = Modifier,
    value: Float = 0.5f,
    onValueChange: (Float) -> Unit = {},
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    activeTrackColor: Color = LocalThemeColors.current.accent
) {
    com.kyant.backdrop.catalog.components.LiquidSlider(
        value = { value },
        onValueChange = onValueChange,
        valueRange = valueRange,
        visibilityThreshold = (valueRange.endInclusive - valueRange.start) * 0.005f,
        backdrop = backdrop,
        modifier = modifier,
        accentColor = activeTrackColor
    )
}

/**
 * 5.2 AppleSlider (Delegado diretamente para o LiquidSlider oficial do AndroidLiquidGlass-kmp).
 */
@Composable
fun AppleSlider(
    value: Float,
    onValueChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
    activeTrackColor: Color = LocalThemeColors.current.accent,
    min: Float = valueRange.start,
    max: Float = valueRange.endInclusive,
    step: Float = 1f,
    ticks: List<Float> = emptyList(),
    onChange: (Float) -> Unit = onValueChange,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    accentColor: Color = activeTrackColor,
    forceLensPreview: Boolean = false,
    lightSurface: Boolean = false,
    backdrop: Backdrop? = null
) {
    val baseRange = if (min != 0f || max != 100f) min..max else valueRange
    val safeEnd = maxOf(baseRange.endInclusive, baseRange.start + 1f)
    val safeRange = baseRange.start..safeEnd
    val safeValue = value.coerceIn(safeRange)
    val threshold = 0.001f
    val actualAccent = if (accentColor != Color(0xFF0088FF)) accentColor else activeTrackColor
    val callback = { v: Float ->
        val clamped = v.coerceIn(safeRange)
        onValueChange(clamped)
        onChange(clamped)
    }
    val effectiveBackdrop = backdrop ?: LocalLiquidGlassBackdrop.current ?: rememberLayerBackdrop()
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        leadingIcon?.invoke()
        com.kyant.backdrop.catalog.components.LiquidSlider(
            value = { safeValue },
            onValueChange = callback,
            valueRange = safeRange,
            visibilityThreshold = threshold,
            backdrop = effectiveBackdrop,
            modifier = Modifier.weight(1f),
            accentColor = actualAccent
        )
        trailingIcon?.invoke()
    }
}

/**
 * 5.3 AppleStepper (SwiftUI.Stepper com Auto-Repeat & Transient Glass Lens).
 */
@Composable
fun AppleStepper(
    value: Int,
    min: Int = 0,
    max: Int = 9999,
    step: Int = 1,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val mat = LocalAppleMaterials.current
    val decInteraction = remember { MutableInteractionSource() }
    val incInteraction = remember { MutableInteractionSource() }
    val decPressed by decInteraction.collectIsPressedAsState()
    val incPressed by incInteraction.collectIsPressedAsState()

    val currentValue by rememberUpdatedState(value)
    val currentOnChange by rememberUpdatedState(onChange)
    var decRepeated by remember { mutableStateOf(false) }
    var incRepeated by remember { mutableStateOf(false) }
    val canDec = value > min
    val canInc = value < max

    Row(
        modifier = modifier
            .height(48.dp)
            .drawBehind {
                val inset = 4.dp.toPx()
                val height = 32.dp.toPx()
                drawRoundRect(
                    color = if (mat.scheme.isLight) Color(120, 120, 128, 36) else Color(120, 120, 128, 61),
                    topLeft = Offset(inset, (size.height - height) / 2f),
                    size = androidx.compose.ui.geometry.Size(size.width - inset * 2f, height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .semantics { contentDescription = "Diminuir" }
                .fillMaxHeight()
                .repeatOnHold(
                    onPress = { decRepeated = false },
                    onRepeat = {
                        if (currentValue > min) {
                            decRepeated = true
                            currentOnChange((currentValue - step).coerceIn(min, max))
                            true
                        } else false
                    }
                )
                .scale(if (decPressed) 0.97f else 1f)
                .clip(RoundedCornerShape(999.dp))
                .then(
                    if (decPressed) {
                        Modifier.drawWithContent {
                            drawContent()
                            drawOpticalGlassMeniscus(
                                cornerRadiusPx = size.height / 2f,
                                isLight = mat.scheme.isLight,
                                edgeRefraction = true,
                                cyanDispersionStrength = 0.45f
                            )
                        }
                    } else Modifier
                )
                .clickable(
                    interactionSource = decInteraction,
                    indication = null,
                    enabled = canDec
                ) {
                    if (!decRepeated) currentOnChange((currentValue - step).coerceIn(min, max))
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "−",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = mat.scheme.textPrimary.copy(alpha = if (canDec) 1f else 0.3f)
            )
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(16.dp)
                .alpha(if (decPressed || incPressed) 0f else 1f)
                .background(
                    if (mat.scheme.isLight) Color.Black.copy(alpha = 0.14f)
                    else Color.White.copy(alpha = 0.18f)
                )
        )

        Box(
            modifier = Modifier
                .width(48.dp)
                .semantics { contentDescription = "Aumentar" }
                .fillMaxHeight()
                .repeatOnHold(
                    onPress = { incRepeated = false },
                    onRepeat = {
                        if (currentValue < max) {
                            incRepeated = true
                            currentOnChange((currentValue + step).coerceIn(min, max))
                            true
                        } else false
                    }
                )
                .scale(if (incPressed) 0.97f else 1f)
                .clip(RoundedCornerShape(999.dp))
                .then(
                    if (incPressed) {
                        Modifier.drawWithContent {
                            drawContent()
                            drawOpticalGlassMeniscus(
                                cornerRadiusPx = size.height / 2f,
                                isLight = mat.scheme.isLight,
                                edgeRefraction = true,
                                cyanDispersionStrength = 0.45f
                            )
                        }
                    } else Modifier
                )
                .clickable(
                    interactionSource = incInteraction,
                    indication = null,
                    enabled = canInc
                ) {
                    if (!incRepeated) currentOnChange((currentValue + step).coerceIn(min, max))
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = mat.scheme.textPrimary.copy(alpha = if (canInc) 1f else 0.3f)
            )
        }
    }
}

data class SegmentOption<T>(
    val value: T,
    val label: String,
    val icon: ImageVector? = null
)

/**
 * 5.4 LiquidGlassSegmentedControl (Autêntico Liquid Glass, idêntico à arquitetura do LiquidBottomTabs com SNAP incondicional).
 */
@Composable
fun LiquidGlassSegmentedControl(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    tabIcons: List<ImageVector?>? = null
) {
    val isLightTheme = !LocalIsDarkTheme.current
    val themeColors = LocalIOS27Theme.current
    val accentColor = themeColors.accent
    val containerColor =
        if (isLightTheme) Color(0xFFFAFAFA).copy(0.4f)
        else Color(0xFF121212).copy(0.4f)
    val contentColor = if (isLightTheme) Color.Black else Color.White
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)
    val currentOnTabSelected by rememberUpdatedState(onTabSelected)
    val tabsCount = tabs.size.coerceAtLeast(1)
    val tabsBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val tabWidth = with(density) {
            (constraints.maxWidth.toFloat() - 8f.dp.toPx()) / tabsCount
        }

        val offsetAnimation = remember { Animatable(0f) }
        val panelOffset by remember(density) {
            derivedStateOf {
                val fraction = (offsetAnimation.value / constraints.maxWidth.coerceAtLeast(1)).fastCoerceIn(-1f, 1f)
                with(density) {
                    4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var didDrag by remember { mutableStateOf(false) }

        val dampedDragAnimation = remember(animationScope, tabsCount) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedIndex.coerceIn(0, tabsCount - 1).toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 56f / 40f,
                onDragStarted = {},
                onDragStopped = {
                    // SNAP INCONDICIONAL: garante que NUNCA pare no meio!
                    val targetIndex = targetValue.roundToInt().coerceIn(0, tabsCount - 1)
                    didDrag = false
                    if (currentSelectedIndex != targetIndex) {
                        currentOnTabSelected(targetIndex)
                    }
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
                    updateValue(
                        (targetValue + dragAmount.x / tabWidth * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            )
        }

        LaunchedEffect(selectedIndex) {
            val clamped = selectedIndex.coerceIn(0, tabsCount - 1).toFloat()
            if (dampedDragAnimation.targetValue != clamped) {
                dampedDragAnimation.animateToValue(clamped)
            }
        }

        val interactiveHighlight = remember(animationScope) {
            com.kyant.backdrop.catalog.legacy.InteractiveHighlight(
                animationScope = animationScope,
                position = { size, _ ->
                    Offset(
                        if (isLtr) (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset,
                        size.height / 2f
                    )
                }
            )
        }

        val rowContent: @Composable RowScope.() -> Unit = {
            tabs.forEachIndexed { index, title ->
                Column(
                    modifier = Modifier
                        .clip(ContinuousCapsule)
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            role = androidx.compose.ui.semantics.Role.Tab,
                            onClick = {
                                currentOnTabSelected(index)
                                animationScope.launch {
                                    dampedDragAnimation.animateToValue(index.toFloat())
                                }
                            }
                        )
                        .fillMaxHeight()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val icon = tabIcons?.getOrNull(index)
                        if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = androidx.compose.material3.LocalContentColor.current,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        BasicText(
                            text = title,
                            style = TextStyle(
                                color = androidx.compose.material3.LocalContentColor.current,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides contentColor
        ) {
            Row(
                Modifier
                    .graphicsLayer { translationX = panelOffset }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousCapsule },
                        effects = {
                            vibrancy()
                            blur(8f.dp.toPx())
                            lens(16f.dp.toPx(), 16f.dp.toPx())
                        },
                        layerBlock = {
                            val progress = dampedDragAnimation.pressProgress
                            val scale = lerp(1f, 1f + 4f.dp.toPx() / size.height, progress)
                            scaleX = scale
                            scaleY = scale
                        },
                        onDrawSurface = { drawRect(containerColor) }
                    )
                    .then(interactiveHighlight.modifier)
                    .height(48.dp)
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = rowContent
            )
        }

        CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides accentColor
        ) {
            Row(
                Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .graphicsLayer {
                        translationX = panelOffset
                        val progress = dampedDragAnimation.pressProgress
                        val scale = lerp(1f, 1f + 4f.dp.toPx() / size.height, progress)
                        scaleX = scale
                        scaleY = scale
                    }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousCapsule },
                        highlight = {
                            val progress = dampedDragAnimation.pressProgress
                            Highlight.Default.copy(alpha = progress)
                        },
                        effects = {
                            val progress = dampedDragAnimation.pressProgress
                            vibrancy()
                            blur(8f.dp.toPx())
                            lens(16f.dp.toPx() * progress, 16f.dp.toPx() * progress)
                        },
                        onDrawSurface = { drawRect(containerColor) }
                    )
                    .then(interactiveHighlight.modifier)
                    .height(40.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = rowContent
            )
        }

        Box(
            Modifier
                .padding(horizontal = 4.dp)
                .graphicsLayer {
                    translationX =
                        if (isLtr) dampedDragAnimation.value * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 1f) * tabWidth + panelOffset
                }
                .then(interactiveHighlight.gestureModifier)
                .then(dampedDragAnimation.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                    shape = { ContinuousCapsule },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Default.copy(alpha = progress)
                    },
                    shadow = {
                        val progress = dampedDragAnimation.pressProgress
                        Shadow(alpha = progress)
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(radius = 6f.dp * progress, alpha = progress)
                    },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        lens(
                            10f.dp.toPx() * progress,
                            18f.dp.toPx() * progress,
                            depthEffect = true,
                            chromaticAberration = true
                        )
                    },
                    layerBlock = {
                        val progress = dampedDragAnimation.pressProgress
                        val scale = lerp(1f, 1f + 12f.dp.toPx() / size.height, progress)
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(
                            if (isLightTheme) Color.Black.copy(0.1f)
                            else Color.White.copy(0.1f),
                            alpha = 1f - progress
                        )
                        drawRect(color = accentColor.copy(alpha = 0.1f * progress))
                    }
                )
                .height(40.dp)
                .fillMaxWidth(1f / tabsCount)
        )
    }
}

@Composable
fun AppleSegmentedPicker(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icons: List<ImageVector?>? = null,
    backdrop: Backdrop? = null
) {
    val effectiveBackdrop = backdrop ?: LocalLiquidGlassBackdrop.current ?: rememberLayerBackdrop()
    LiquidGlassSegmentedControl(
        tabs = options,
        selectedIndex = selectedIndex,
        onTabSelected = onSelect,
        backdrop = effectiveBackdrop,
        modifier = modifier,
        tabIcons = icons
    )
}

@Composable
fun <T> AppleSegmentedPicker(
    options: List<SegmentOption<T>>,
    value: T,
    onChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    forceLensPreview: Boolean = false,
    backdrop: Backdrop? = null
) {
    val labels = remember(options) { options.map { it.label } }
    val icons = remember(options) { options.map { it.icon } }
    val selectedIndex = remember(options, value) {
        options.indexOfFirst { it.value == value }.coerceAtLeast(0)
    }
    AppleSegmentedPicker(
        options = labels,
        selectedIndex = selectedIndex,
        onSelect = { idx ->
            options.getOrNull(idx)?.let { onChange(it.value) }
        },
        modifier = modifier,
        icons = icons,
        backdrop = backdrop
    )
}

/**
 * 5.4b LiquidGlassModeSwitcher (Baseado diretamente no código JS `trackPrevious(switcher)` e CSS `.switcher`
 * com camada de fundo `Modifier.liquidGlass()` isolada para manter o texto nítido).
 */
@Composable
fun LiquidGlassModeSwitcher(
    isLightMode: Boolean,
    onSelectMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val motion by androidx.compose.runtime.rememberUpdatedState(com.example.ui.theme.LocalAppMotion.current)
    val mat = LocalAppleMaterials.current
    val selectedIdx = if (isLightMode) 0 else 1
    var previousIdx by remember { mutableIntStateOf(selectedIdx) }
    val scaleToggle = remember { Animatable(1f) }
    var originX by remember { mutableFloatStateOf(0.5f) }

    LaunchedEffect(selectedIdx) {
        if (selectedIdx != previousIdx) {
            originX = if (previousIdx < selectedIdx) 0f else 1f
            previousIdx = selectedIdx
            scaleToggle.snapTo(1f)
            scaleToggle.animateTo(
                targetValue = 1f,
                animationSpec = keyframes {
                    durationMillis = 440
                    1.0f at 0
                    1.16f at 220
                    1.0f at 440
                }
            )
        }
    }

    val slideProgress by animateFloatAsState(
        targetValue = selectedIdx.toFloat(),
        animationSpec = motion.spring(dampingRatio = 0.78f, stiffness = 240f),
        label = "switcherSlide"
    )

    BoxWithConstraints(
        modifier = modifier
            .width(220.dp)
            .height(54.dp)
            .shadow(10.dp, RoundedCornerShape(999.dp), spotColor = mat.scheme.accentColor.copy(alpha = 0.16f))
            .liquidGlass(shape = RoundedCornerShape(999.dp), elevation = 10.dp)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val optionW = maxWidth / 2f
        val thumbOffset = optionW * slideProgress

        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .width(optionW)
                .fillMaxHeight()
                .graphicsLayer {
                    scaleX = scaleToggle.value
                    transformOrigin = TransformOrigin(originX, 0.5f)
                }
                .shadow(4.dp, RoundedCornerShape(999.dp))
                .clip(RoundedCornerShape(999.dp))
                .background(
                    if (mat.scheme.isLight) Color.White.copy(alpha = 0.72f)
                    else mat.scheme.pillActiveBg
                )
                .drawWithContent {
                    drawContent()
                    drawOpticalGlassMeniscus(
                        cornerRadiusPx = size.height / 2f,
                        isLight = mat.scheme.isLight,
                        edgeRefraction = true,
                        cyanDispersionStrength = 0.42f
                    )
                }
        )

        Row(modifier = Modifier.fillMaxSize()) {
            val modes = listOf(true to "Claro", false to "Escuro")
            modes.forEach { (lightVal, label) ->
                val active = isLightMode == lightVal
                val iconScale by animateFloatAsState(
                    targetValue = if (active) 1.0f else 1.06f,
                    animationSpec = motion.spring(dampingRatio = 0.78f, stiffness = 260f),
                    label = "switcherIconScale"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onSelectMode(lightVal)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) mat.scheme.textPrimary else mat.scheme.textSecondary,
                        modifier = Modifier.scale(iconScale)
                    )
                }
            }
        }
    }
}

/**
 * Modelo de item para o Menu Flutuante de Liquid Glass (iOS 26 / SwiftUI Menu).
 */
@androidx.compose.runtime.Stable
class LiquidGlassMenuItemData(
    val label: String,
    val icon: ImageVector? = null,
    val selected: Boolean = false,
    val showDividerBelow: Boolean = false,
    val onClick: () -> Unit
)

@androidx.compose.runtime.Stable
class LiquidGlassMenuRequest(
    val id: String,
    val anchorBounds: IntRect,
    val triggerLabel: String = "",
    val triggerIcon: ImageVector? = Icons.Default.UnfoldMore,
    val items: List<LiquidGlassMenuItemData>,
    val onDismiss: () -> Unit
)

@androidx.compose.runtime.Stable
class LiquidGlassMenuHostState {
    var currentRequest by mutableStateOf<LiquidGlassMenuRequest?>(null)
        private set
    var morphingMenuId by mutableStateOf<String?>(null)
        internal set
    var activeMenuProgress by mutableFloatStateOf(0f)
        internal set

    fun isMenuVisibleOrMorphing(id: String): Boolean {
        return currentRequest?.id == id || morphingMenuId == id
    }

    internal var menuProgressProvider: (() -> Float)? = null

    fun progressForMenu(id: String): Float {
        return if (isMenuVisibleOrMorphing(id)) menuProgressProvider?.invoke() ?: activeMenuProgress else 0f
    }

    fun showMenu(request: LiquidGlassMenuRequest) {
        morphingMenuId = request.id
        currentRequest = request
    }

    fun updateTriggerLabel(id: String, newLabel: String) {
        val current = currentRequest
        if (current != null && current.id == id && current.triggerLabel != newLabel) {
            currentRequest = LiquidGlassMenuRequest(
                id = current.id,
                anchorBounds = current.anchorBounds,
                triggerLabel = newLabel,
                triggerIcon = current.triggerIcon,
                items = current.items,
                onDismiss = current.onDismiss
            )
        }
    }

    fun updateAnchorBounds(id: String, bounds: IntRect) {
        val current = currentRequest
        if (current != null && current.id == id && current.anchorBounds != bounds) {
            currentRequest = LiquidGlassMenuRequest(
                id = current.id,
                anchorBounds = bounds,
                triggerLabel = current.triggerLabel,
                triggerIcon = current.triggerIcon,
                items = current.items,
                onDismiss = current.onDismiss
            )
        }
    }

    fun dismissMenu(id: String? = null) {
        val current = currentRequest
        if (current != null && (id == null || current.id == id)) {
            currentRequest = null
            current.onDismiss()
        }
    }
}

val LocalLiquidGlassMenuHost = staticCompositionLocalOf<LiquidGlassMenuHostState?> { null }

/** Root overlay; measures menu rows once and animates an interruptible render layer. */
@Composable
fun LiquidGlassMenuOverlayHost(state: LiquidGlassMenuHostState, backdrop: Backdrop, modifier: Modifier = Modifier) {
    RetainedGlassMenu(state, backdrop, modifier)
}

/**
 * 5.4 AppleMenuPicker (Pop-up Menu Picker com efeito gelatinoso iOS 26 Liquid Glass real sobre o conteúdo da tela).
 */
@Composable
fun <T> AppleMenuPicker(
    options: List<SegmentOption<T>>,
    value: T,
    onChange: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val motion by androidx.compose.runtime.rememberUpdatedState(com.example.ui.theme.LocalAppMotion.current)
    val mat = LocalAppleMaterials.current
    val s = mat.scheme
    val isLight = s.isLight
    val settings = com.example.ui.theme.LocalAppSettings.current
    val density = LocalDensity.current
    val backdrop = rememberCurrentBackdrop()
    val menuHost = LocalLiquidGlassMenuHost.current
    val menuId = remember { "apple_menu_${System.nanoTime()}" }

    var open by remember { mutableStateOf(false) }
    var anchorBounds by remember { mutableStateOf(IntRect.Zero) }
    val currentOpt = options.firstOrNull { it.value == value } ?: options.first()
    val currentOnChange by rememberUpdatedState(onChange)

    // GLASS_PRESS_FEEDBACK_V2: one touch handler for both click and press.
    // PICKER_PRESS_LIGHT_V6: observe finger position without consuming clicks.
    var pickerLightOrigin by remember { mutableStateOf(Offset.Zero) }
    val pickerInteractions = remember { MutableInteractionSource() }
    val pickerPressed by pickerInteractions.collectIsPressedAsState()
    val pickerPressProgress by animateFloatAsState(
        targetValue = if (pickerPressed) 1f else 0f,
        animationSpec = motion.spring(dampingRatio = 0.62f, stiffness = 420f),
        label = "pickerGlassPress"
    )
    val animationScope = rememberCoroutineScope()
    val triggerHighlight = remember(animationScope) {
        InteractiveHighlight(reduceMotion = { motion.reduced }, animationScope = animationScope)
    }

    // Progresso contínuo (0f..1f) da gota do menu para cross-morph suave sem nunca piscar ou sumir o botão
    val hostProgress = { menuHost?.progressForMenu(menuId) ?: 0f }
    fun triggerMorphAlpha() = (1f - (hostProgress() / 0.34f)).coerceIn(0f, 1f)
    fun triggerMorphScale() = lerp(1f, 0.94f, (hostProgress() / 0.45f).coerceIn(0f, 1f))

    val menuItems = remember(options, value) {
        options.mapIndexed { idx, opt ->
            LiquidGlassMenuItemData(
                label = opt.label,
                icon = opt.icon,
                selected = opt.value == value,
                showDividerBelow = idx < options.lastIndex,
                onClick = {
                    currentOnChange(opt.value)
                    open = false
                }
            )
        }
    }

    LaunchedEffect(open, anchorBounds, menuItems, currentOpt.label, menuHost) {
        if (menuHost != null) {
            if (open && anchorBounds.width > 0 && anchorBounds.height > 0 &&
                (menuHost.currentRequest?.id != menuId || menuHost.currentRequest?.items !== menuItems)) {
                menuHost.showMenu(
                    LiquidGlassMenuRequest(
                        id = menuId,
                        anchorBounds = anchorBounds,
                        triggerLabel = currentOpt.label,
                        triggerIcon = Icons.Default.UnfoldMore,
                        items = menuItems,
                        onDismiss = { open = false }
                    )
                )
            } else if (!open && menuHost.currentRequest?.id == menuId) {
                menuHost.dismissMenu(menuId)
            }
        }
    }

    androidx.compose.runtime.DisposableEffect(menuId, menuHost) {
        onDispose {
            menuHost?.dismissMenu(menuId)
        }
    }

    val effectiveBlurDp = if (settings.reduceTransparency) 20.dp else (4.5f * settings.glassBlurIntensity).coerceIn(2f, 14f).dp
    val refractionMult = if (!settings.reduceTransparency && settings.enableLensRefraction) settings.glassIntensity.coerceIn(0.7f, 2.2f) else 0f
    val chromaticAberration = !settings.reduceTransparency && settings.enableChromaticAberration
    val glassAlpha = ((if (isLight) 0.24f else 0.28f) * settings.glassOpacity).coerceIn(0.10f, 0.65f)
    val baseSurfaceColor = if (isLight) Color.White.copy(alpha = glassAlpha) else Color(0xFF16161C).copy(alpha = glassAlpha)
    val activeAccent = if (isLight && s.accentColor == Color.White) s.textPrimary else s.accentColor

    Box(
        modifier = modifier.onGloballyPositioned { coords ->
            if (coords.isAttached) {
                val pos = coords.positionInRoot()
                val sz = coords.size
                val newBounds = IntRect(
                    left = pos.x.roundToInt(),
                    top = pos.y.roundToInt(),
                    right = (pos.x + sz.width).roundToInt(),
                    bottom = (pos.y + sz.height).roundToInt()
                )
                if (anchorBounds != newBounds) {
                    anchorBounds = newBounds
                    if (open) {
                        menuHost?.updateAnchorBounds(menuId, newBounds)
                    }
                }
            }
        }
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .graphicsLayer {
                    // Cross-morph progressivo com a gota do LiquidGlassMenuOverlayHost:
                    // Ao fechar, o botão reaparece suavemente enquanto a gota se funde nele, sem nunca piscar!
                    alpha = triggerMorphAlpha()
                }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousCapsule },
                    highlight = {
                        Highlight.Default.copy(
                            alpha = (if (isLight) 0.80f else 0.64f) * triggerMorphAlpha()
                        )
                    },
                    shadow = {
                        Shadow(
                            radius = 8.dp,
                            offset = DpOffset(0.dp, 3.dp),
                            color = Color.Black.copy(alpha = (if (isLight) 0.12f else 0.28f) * triggerMorphAlpha())
                        )
                    },
                    innerShadow = {
                        InnerShadow(
                            radius = 5.dp,
                            alpha = lerp(if (isLight) 0.18f else 0.30f, 0.38f, pickerPressProgress) * triggerMorphAlpha()
                        )
                    },
                    effects = {
                        if (!settings.reduceTransparency) {
                            vibrancy()
                        }
                        val blurPx = effectiveBlurDp.toPx()
                        if (blurPx >= 1f && !blurPx.isNaN()) {
                            blur(blurPx)
                        }
                        if (refractionMult > 0f && size.isSpecified && size.width > 4f && size.height > 4f) {
                            val boost = 1f + 0.30f * pickerPressProgress
                            val maxAllowedHeight = min(size.width, size.height) * 0.46f
                            lens(
                                refractionHeight = ((14f * refractionMult * boost).dp.toPx()).coerceAtMost(maxAllowedHeight),
                                refractionAmount = (28f * refractionMult * boost).dp.toPx(),
                                depthEffect = true,
                                chromaticAberration = chromaticAberration
                            )
                        }
                    },
                    layerBlock = {
                        if (size.isSpecified && size.width > 0f && size.height > 0f) {
                            val width = size.width
                            val height = size.height
                            val progress = pickerPressProgress
                            val scale = lerp(1f, 1f + 5f.dp.toPx() / height, progress) * triggerMorphScale()

                            val maxOffset = size.minDimension
                            val initialDerivative = 0.055f
                            val offset = triggerHighlight.offset
                            translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                            translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                            val maxDragScale = 6f.dp.toPx() / height
                            val offsetAngle = atan2(offset.y, offset.x)
                            scaleX =
                                scale +
                                    maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                                    (width / height).fastCoerceAtMost(1f)
                            scaleY =
                                scale +
                                    maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                                    (height / width).fastCoerceAtMost(1f)
                        }
                    },
                    onDrawSurface = {
                        drawRect(baseSurfaceColor)
                    }
                )
                // Press animation is driven by the clickable interaction source.
                // SORT_PICKER_SINGLE_TOUCH_HANDLER: clickable owns touch; no competing decorative gesture
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        pickerLightOrigin = down.position
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            event.changes.firstOrNull()?.let { pickerLightOrigin = it.position }
                            if (event.changes.none { it.pressed }) break
                        }
                    }
                }
                // PICKER_STABLE_PRESS_COLOR_V10: retain the resting surface color while held.
                .clip(ContinuousCapsule)
                .clickable(
                    interactionSource = pickerInteractions,
                    indication = null
                ) {
                    val nextOpen = !open
                    open = nextOpen
                    if (menuHost != null) {
                        if (nextOpen && anchorBounds.width > 0 && anchorBounds.height > 0) {
                            // Dispara no mesmo frame do clique para zero atraso na abertura!
                            menuHost.showMenu(
                                LiquidGlassMenuRequest(
                                    id = menuId,
                                    anchorBounds = anchorBounds,
                                    triggerLabel = currentOpt.label,
                                    triggerIcon = Icons.Default.UnfoldMore,
                                    items = menuItems,
                                    onDismiss = { open = false }
                                )
                            )
                        } else if (!nextOpen) {
                            menuHost.dismissMenu(menuId)
                        }
                    }
                }
                .padding(horizontal = 13.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = currentOpt.label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (open) activeAccent else s.textPrimary,
                maxLines = 1
            )
            Icon(
                imageVector = Icons.Default.UnfoldMore,
                contentDescription = null,
                tint = (if (open) activeAccent else s.textPrimary).copy(alpha = 0.85f),
                modifier = Modifier.size(14.dp)
            )
        }

        // Fallback seguro caso LocalLiquidGlassMenuHost não esteja presente em um Preview isolado
        if (open && menuHost == null) {
            val popoverShape = remember { RoundedCornerShape(24.dp) }
            val positionProvider = remember(density) {
                object : PopupPositionProvider {
                    override fun calculatePosition(
                        anchorBounds: IntRect,
                        windowSize: IntSize,
                        layoutDirection: LayoutDirection,
                        popupContentSize: IntSize
                    ): IntOffset {
                        val marginPx = with(density) { 8.dp.roundToPx() }
                        val minEdgeMargin = with(density) { 12.dp.roundToPx() }
                        val targetX = anchorBounds.right - popupContentSize.width
                        val maxX = (windowSize.width - popupContentSize.width - minEdgeMargin).coerceAtLeast(minEdgeMargin)
                        val x = targetX.coerceIn(minEdgeMargin, maxX)
                        val y = anchorBounds.bottom + marginPx
                        return IntOffset(x, y)
                    }
                }
            }

            Popup(
                popupPositionProvider = positionProvider,
                onDismissRequest = { open = false },
                properties = PopupProperties(focusable = true)
            ) {
                Box(
                    modifier = Modifier
                        .shadow(
                            shape = { popoverShape },
                            shadow = {
                                Shadow(
                                    radius = 22.dp,
                                    offset = DpOffset(0.dp, 8.dp),
                                    color = Color.Black.copy(alpha = if (isLight) 0.18f else 0.40f)
                                )
                            }
                        )
                        .highlight(
                            shape = { popoverShape },
                            highlight = { Highlight.Default }
                        )
                        .clip(popoverShape)
                        .background(baseSurfaceColor)
                        .width(238.dp)
                        .padding(vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        options.forEach { opt ->
                            val active = opt.value == value
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onChange(opt.value)
                                        open = false
                                    }
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier.width(18.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (active) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = activeAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = opt.label,
                                    fontSize = 13.5.sp,
                                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (active) activeAccent else s.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (opt.icon != null) {
                                    Icon(
                                        imageVector = opt.icon,
                                        contentDescription = null,
                                        tint = if (active) activeAccent else s.textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 5.5 AppleTextField (SwiftUI.TextField).
 */
@Composable
fun AppleTextField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector? = null,
    useContentMaterial: Boolean = false,
    modifier: Modifier = Modifier
) {
    val mat = LocalAppleMaterials.current
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (useContentMaterial) Modifier.contentMaterialSurface(shape = shape, elevation = 4.dp)
                else Modifier.inputWellSurface(shape = shape)
            )
            .then(
                if (focused) Modifier.border(2.dp, mat.scheme.accentColor, shape)
                else Modifier
            )
            .heightIn(min = 48.dp)
            .padding(start = 14.dp, end = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (focused) mat.scheme.accentColor else mat.scheme.textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    fontSize = 13.sp,
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
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = mat.scheme.textPrimary
                ),
                cursorBrush = SolidColor(mat.scheme.accentColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .onFocusChanged { focused = it.isFocused }
            )
        }

        if (value.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { onChange("") }
                    .padding(end = 12.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Limpar",
                    tint = mat.scheme.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * StarRating (Supports 2xs, xs, sm, md, lg, readOnly, half-star precision, AND continuous horizontal drag selection!).
 */
@Composable
fun StarRating(
    rating: Float = 0f,
    onChange: ((Float) -> Unit)? = null,
    readOnly: Boolean = false,
    size: String = "md",
    starColor: Color = Color(0xFFFFD60A),
    allowHalf: Boolean = true,
    modifier: Modifier = Modifier
) {
    val mat = LocalAppleMaterials.current
    val iconDp = when (size) {
        "2xs" -> 9.5.dp
        "xs" -> 11.5.dp
        "sm" -> 14.dp
        "lg" -> 28.dp
        else -> 16.dp
    }
    val spacingDp = when (size) {
        "2xs" -> 1.dp
        "xs" -> 1.5.dp
        "lg" -> 5.dp
        else -> 2.dp
    }

    var rowWidthPx by remember { mutableFloatStateOf(1f) }
    val currentRating by rememberUpdatedState(rating)
    val currentOnChange by rememberUpdatedState(onChange)

    fun computeRatingFromX(xPx: Float): Float {
        val ratio = (xPx / rowWidthPx).coerceIn(0f, 1f)
        if (ratio < 0.05f) return 0f
        val raw = ratio * 5f
        return if (allowHalf) {
            ((raw * 2f).roundToInt() / 2f).coerceIn(0.5f, 5f)
        } else {
            kotlin.math.ceil(raw.toDouble()).toFloat().coerceIn(1f, 5f)
        }
    }

    Row(
        modifier = modifier
            .onSizeChanged { rowWidthPx = it.width.toFloat().coerceAtLeast(1f) }
            .then(
                if (!readOnly && onChange != null) {
                    Modifier
                        .heightIn(min = 48.dp)
                        .semantics {
                            progressBarRangeInfo = androidx.compose.ui.semantics.ProgressBarRangeInfo(rating, 0f..5f, if (allowHalf) 9 else 4)
                            stateDescription = "$rating de 5 estrelas"
                            setProgress { requested ->
                                val next = if (allowHalf) (requested * 2f).roundToInt() / 2f else requested.roundToInt().toFloat()
                                currentOnChange?.invoke(next.coerceIn(0f, 5f))
                                true
                            }
                        }
                        .pointerInput(allowHalf) {
                            var startRating = 0f
                            var tapRating = 0f
                            var dragged = false
                            inspectDragGestures(
                                onDragStart = { down ->
                                    startRating = currentRating
                                    tapRating = computeRatingFromX(down.position.x)
                                    dragged = false
                                },
                                onDragEnd = {
                                    if (!dragged) currentOnChange?.invoke(
                                        if (abs(tapRating - startRating) < 0.01f) 0f else tapRating
                                    )
                                },
                                onDragCancel = { if (dragged) currentOnChange?.invoke(startRating) }
                            ) { change, _ ->
                                dragged = true
                                currentOnChange?.invoke(computeRatingFromX(change.position.x))
                            }
                        }
                } else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacingDp)
    ) {
        for (idx in 1..5) {
            val fullVal = idx.toFloat()
            val halfVal = idx - 0.5f
            val icon = when {
                rating >= fullVal -> Icons.Default.Star
                rating >= halfVal -> Icons.Default.StarHalf
                else -> Icons.Outlined.StarOutline
            }
            val filled = rating >= halfVal

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (filled) starColor else if (mat.scheme.isLight) Color(0xFF6E6E73).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(iconDp)
            )
        }
    }
}

/**
 * SettingsSquircleIcon (30dp x 30dp rounded-10dp squircle with iOS 27 Theme-Tinted style).
 */
@Composable
fun SettingsSquircleIcon(
    colors: List<Color> = emptyList(),
    content: @Composable () -> Unit
) {
    val themeColors = LocalIOS27Theme.current
    val isDark = LocalIsDarkTheme.current
    val shape = RoundedCornerShape(10.dp)
    // O quadradinho segue a base neutra do tema do app (modo escuro/claro), enquanto apenas o ícone segue a cor de destaque (accent)
    val bgColor = if (isDark) {
        if (themeColors.isAllBlack) Color(0xFF1E1E22) else themeColors.bgTertiary.copy(alpha = 0.72f)
    } else {
        if (themeColors.isAllWhite) Color(0xFFE8E8EE) else themeColors.bgTertiary.copy(alpha = 0.85f)
    }
    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)

    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(shape)
            .background(bgColor)
            .border(0.5.dp, borderColor, shape),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * 6. IOS27LiquidGlassController (Compacto e adaptável ao tema selecionado, como na referência do iOS 27).
 */
@Composable
fun IOS27LiquidGlassController(
    glassLevel: Int,
    onChangeGlassLevel: (Int) -> Unit,
    edgeRefraction: Boolean,
    glassVariant: String
) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme
    val density = LocalDensity.current

    val variantOffset = if (glassVariant == "frosted") 0.38f else 0.08f
    val ratio = (glassLevel / 100f).coerceIn(0f, 1f)
    val centerFillAlpha = min(0.86f, 0.14f + variantOffset + ratio.pow(1.15f) * 0.70f)
    val lakeLensFill = if (s.isLight) {
        Color.White.copy(alpha = centerFillAlpha)
    } else {
        s.gradLinearMid.copy(alpha = (centerFillAlpha * 0.85f).coerceIn(0.16f, 0.82f))
    }
    val lensBlurDp = (if (glassVariant == "frosted") 15f + ratio * 8.5f else 1.5f + ratio * 8.5f).dp

    val cardBgColor = if (s.isLight) {
        s.gradLinearMid
    } else {
        s.gradLinearStart
    }

    @Composable
    fun LakeBackgroundContent(
        textColor: Color = s.textPrimary.copy(alpha = 0.86f),
        titleColor: Color = s.textPrimary
    ) {
        Column {
            Text(
                text = "O lago",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                letterSpacing = (-0.4).sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "With gentle lapping waves and a surrounding of trees, it's one of the most serene spots on campus.",
                fontSize = 13.5.sp,
                lineHeight = 18.sp,
                color = textColor
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(cardBgColor, s.baseBg)
                    )
                )
                .border(0.6.dp, mat.contentMaterialBorder, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                LakeBackgroundContent()
            }

            val controlsTopOffsetDp = 24.dp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = controlsTopOffsetDp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(8.dp, CircleShape, spotColor = Color.Black.copy(alpha = if (s.isLight) 0.16f else 0.45f))
                        .clip(CircleShape)
                        .background(lakeLensFill)
                        .drawWithContent {
                            drawContent()
                            drawOpticalGlassMeniscus(
                                cornerRadiusPx = size.height / 2f,
                                isLight = s.isLight,
                                edgeRefraction = edgeRefraction,
                                cyanDispersionStrength = (1f - ratio * 0.45f) * 0.55f
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(s.textPrimary)
                        )
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(s.textPrimary)
                        )
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(s.textPrimary)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .shadow(8.dp, RoundedCornerShape(999.dp), spotColor = Color.Black.copy(alpha = if (s.isLight) 0.16f else 0.45f))
                        .clip(RoundedCornerShape(999.dp))
                        .background(lakeLensFill)
                        .drawWithContent {
                            drawContent()
                            drawOpticalGlassMeniscus(
                                cornerRadiusPx = size.height / 2f,
                                isLight = s.isLight,
                                edgeRefraction = edgeRefraction,
                                cyanDispersionStrength = (1f - ratio * 0.45f) * 0.55f
                            )
                        }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = s.textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Search",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = s.textSecondary
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .contentMaterialSurface(RoundedCornerShape(20.dp), elevation = 3.dp)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            AppleSlider(
                value = glassLevel.toFloat(),
                min = 0f,
                max = 100f,
                step = 1f,
                ticks = listOf(0f, 33f, 66f, 100f),
                lightSurface = s.isLight,
                accentColor = s.accentColor,
                onChange = { v ->
                    onChangeGlassLevel(v.roundToInt())
                },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onChangeGlassLevel(0) },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(22.dp, 18.dp)) {
                            drawRoundRect(
                                color = s.textSecondary,
                                topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                                size = Size(12.dp.toPx(), 9.dp.toPx()),
                                cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx()),
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                            drawRoundRect(
                                color = s.textSecondary,
                                topLeft = Offset(7.dp.toPx(), 6.dp.toPx()),
                                size = Size(12.dp.toPx(), 9.dp.toPx()),
                                cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx()),
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                    }
                },
                trailingIcon = {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onChangeGlassLevel(100) },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(22.dp, 18.dp)) {
                            drawRoundRect(
                                color = s.textSecondary.copy(alpha = 0.68f),
                                topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                                size = Size(12.dp.toPx(), 9.dp.toPx()),
                                cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
                            )
                            drawRoundRect(
                                color = s.textSecondary,
                                topLeft = Offset(7.dp.toPx(), 6.dp.toPx()),
                                size = Size(12.dp.toPx(), 9.dp.toPx()),
                                cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
                            )
                        }
                    }
                }
            )
        }

        Text(
            text = "Clear é mais transparente e Tinted aumenta a opacidade, adicionando contraste ao conteúdo e controles.",
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            color = s.textSecondary,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
    }
}

/**
 * Sculptural Wave Wallpaper + SwiftUI `.backgroundExtensionEffect()` backdrop.
 */
@Composable
fun SculpturalWaveBackdrop(
    featuredCoverUrl: String?,
    backgroundExtensionEnabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme
    val hasActiveReadingCover = backgroundExtensionEnabled && !featuredCoverUrl.isNullOrBlank()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(s.baseBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(s.baseBg)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(s.gradLinearStart, s.gradLinearMid, s.gradLinearEnd),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    )
                )
                if (hasActiveReadingCover) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(s.gradTopRight, Color.Transparent),
                            center = Offset(size.width * 0.82f, size.height * 0.14f),
                            radius = size.width * 0.85f
                        ),
                        center = Offset(size.width * 0.82f, size.height * 0.14f),
                        radius = size.width * 0.85f
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(s.gradBottomLeft, Color.Transparent),
                            center = Offset(size.width * 0.15f, size.height * 0.82f),
                            radius = size.width * 0.85f
                        ),
                        center = Offset(size.width * 0.15f, size.height * 0.82f),
                        radius = size.width * 0.85f
                    )

                    val sx = size.width / 600f
                    val sy = size.height / 900f
                    val pathA = Path().apply {
                        moveTo(-80f * sx, 180f * sy)
                        cubicTo(180f * sx, 290f * sy, 340f * sx, 40f * sy, 680f * sx, 220f * sy)
                        lineTo(680f * sx, 950f * sy)
                        lineTo(-80f * sx, 950f * sy)
                        close()
                    }
                    drawPath(pathA, color = s.ribbonA.copy(alpha = s.ribbonA.alpha * 0.45f))

                    val pathB = Path().apply {
                        moveTo(-80f * sx, 520f * sy)
                        cubicTo(220f * sx, 380f * sy, 410f * sx, 690f * sy, 680f * sx, 470f * sy)
                        lineTo(680f * sx, 950f * sy)
                        lineTo(-80f * sx, 950f * sy)
                        close()
                    }
                    drawPath(pathB, color = s.ribbonB.copy(alpha = s.ribbonB.alpha * 0.45f))
                }
            }

            if (hasActiveReadingCover && featuredCoverUrl != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .alpha(0.32f)
                ) {
                    val context = LocalContext.current
                    val ambientScaffoldReq = remember(featuredCoverUrl, context) {
                        val clean = featuredCoverUrl.replace("http://", "https://")
                        ImageRequest.Builder(context)
                            .data(clean)
                            .size(28, 42)
                            .precision(coil.size.Precision.INEXACT)
                            .crossfade(false)
                            .allowHardware(true)
                            .allowRgb565(true)
                            .memoryCacheKey(clean + "_scaffold_amb")
                            .diskCacheKey(clean + "_scaffold_amb")
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .build()
                    }
                    AsyncImage(
                        model = ambientScaffoldReq,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(16.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(s.baseBg.copy(alpha = 0.24f), s.baseBg)
                                )
                            )
                    )
                }
            }
        }

        content()
    }
}

/**
 * Apple HIG Activity Rings & Circular Gauge (`activity-rings` + `gauges`).
 * Suporta 2 ou 3 anéis concêntricos no estilo Apple Fitness (Movimento →, Exercício ↠, Constância ↑).
 */
@Composable
fun AppleActivityRingsGauge(
    outerProgress: Float,
    innerProgress: Float,
    centerLabel: String,
    sizeDp: Dp = 68.dp,
    modifier: Modifier = Modifier,
    outerColor: Color = LocalIOS27Theme.current.accent,
    innerColor: Color = LocalIOS27Theme.current.accentSecondary,
    thirdProgress: Float? = null,
    thirdColor: Color = LocalIOS27Theme.current.accentSecondary,
    showCenterLabel: Boolean = true
) {
    if (LocalHomeSkeleton.current != null) { HomeSkeletonBlock(modifier.size(sizeDp).clip(CircleShape)); return }

    val motion by androidx.compose.runtime.rememberUpdatedState(com.example.ui.theme.LocalAppMotion.current)
    val s = LocalAppleMaterials.current.scheme
    val isDark = LocalIsDarkTheme.current
    val animOuter by animateFloatAsState(
        targetValue = outerProgress.coerceIn(0f, 1.5f),
        animationSpec = motion.spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessLow),
        label = "outerRing"
    )
    val animInner by animateFloatAsState(
        targetValue = innerProgress.coerceIn(0f, 1.5f),
        animationSpec = motion.spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessLow),
        label = "innerRing"
    )
    val animThird by animateFloatAsState(
        targetValue = (thirdProgress ?: 0f).coerceIn(0f, 1.5f),
        animationSpec = motion.spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessLow),
        label = "thirdRing"
    )

    Box(
        modifier = modifier.size(sizeDp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val hasThree = thirdProgress != null
            val minDim = this.size.minDimension
            val strokeWidth = when {
                hasThree -> (minDim * 0.135f).coerceIn(3.dp.toPx(), 20.dp.toPx())
                else -> (minDim * 0.15f).coerceIn(4.dp.toPx(), 18.dp.toPx())
            }
            val gap = (strokeWidth * 0.11f).coerceAtLeast(1.2.dp.toPx())
            val trackAlpha = if (isDark) 0.22f else 0.16f

            fun drawFitnessRing(
                ringIndex: Int,
                progress: Float,
                baseColor: Color
            ) {
                val inset = strokeWidth / 2f + 1.dp.toPx() + ringIndex * (strokeWidth + gap)
                val diameter = (minDim - inset * 2f).coerceAtLeast(4f)
                val topLeft = Offset(inset, inset)
                val arcSize = Size(diameter, diameter)

                // Trilho translúcido colorido na cor do respectivo anel (1, 2 ou 3), sem contorno preto
                drawArc(
                    color = baseColor.copy(alpha = trackAlpha),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Arco de progresso uniforme da mesma cor do anel (sem bolinha de outra cor na ponta)
                val clamped = progress.coerceIn(0f, 1f)
                if (clamped > 0.005f) {
                    val effectiveSweep = (clamped * 360f).coerceAtLeast(6f)
                    drawArc(
                        color = baseColor,
                        startAngle = -90f,
                        sweepAngle = effectiveSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            drawFitnessRing(0, animOuter, outerColor)
            drawFitnessRing(1, animInner, innerColor)
            if (hasThree) {
                drawFitnessRing(2, animThird, thirdColor)
            }
        }

        if (showCenterLabel && centerLabel.isNotBlank()) {
            Text(
                text = centerLabel,
                fontSize = if (thirdProgress != null) 11.sp else 11.5.sp,
                fontFamily = SpaceGroteskTabularFamily,
                fontWeight = FontWeight.Bold,
                color = s.textPrimary
            )
        }
    }
}

/**
 * Apple HIG Capsule Button (`LiquidButton.kt` com `InteractiveHighlight` e física elástica ao toque).
 */
@Composable
fun AppleCapsuleButton(
    label: String,
    prominent: Boolean = false,
    leadingIcon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppleMaterials.current.scheme
    com.kyant.backdrop.catalog.components.LiquidButton(
        onClick = onClick,
        backdrop = rememberCurrentBackdrop(),
        modifier = modifier,
        surfaceColor = if (prominent) s.accentBg else Color.Unspecified
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (prominent) s.accentText else s.textPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = if (prominent) FontWeight.Bold else FontWeight.SemiBold,
            color = if (prominent) s.accentText else s.textPrimary,
            maxLines = 1
        )
    }
}
