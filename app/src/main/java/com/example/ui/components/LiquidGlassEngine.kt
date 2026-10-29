package com.example.ui.components

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * ============================================================================================
 * KYANT ANDROID-LIQUID-GLASS-KMP ENGINE (HIGH-PERFORMANCE / ZERO-ALLOCATION PER FRAME)
 * Adaptado dos arquivos do projeto AndroidLiquidGlass-kmp (por Kyant):
 * - SdfShader.android.kt (AGSL Signed Distance Field Meniscus & Refraction com cache de RenderEffect)
 * - DampedDragAnimation.kt (MutatorMutex Elastic Spring Physics & Velocity Deformation)
 * - InteractiveHighlight.kt (Pointer-Following Specular Highlight sem flood de corrotinas)
 * - ProgressiveBlurContent.kt (Desfoque e máscara progressiva inferior para a Dock Bar)
 * ============================================================================================
 */

object SdfShader {
    /**
     * Shader AGSL baseado em `SdfShader.android.kt` (AndroidLiquidGlass-kmp por Kyant).
     * Calcula o SDF exato da caixa arredondada (`sdRoundedBox`) e seu vetor normal (`gradSdRoundedBox`),
     * aplicando refração de lente esférica (`circleMap`), aberração cromática e bisel especular.
     */
    const val AGSL_SDF_LIQUID_GLASS_SOURCE = """
        uniform shader content;
        uniform float2 size;
        uniform float4 cornerRadii;
        uniform float refractionHeight;
        uniform float refractionAmount;
        uniform float eccentricFactor;
        uniform float chromaticAberration;
        uniform float lightAngle;
        uniform float highlightStrength;

        float sdRoundedBox(float2 p, float2 b, float4 r) {
            float2 rSelected = (p.x > 0.0) ? r.zw : r.xy;
            float radius = (p.y > 0.0) ? rSelected.y : rSelected.x;
            float2 q = abs(p) - b + radius;
            return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius;
        }

        float2 gradSdRoundedBox(float2 p, float2 b, float4 r) {
            float2 rSelected = (p.x > 0.0) ? r.zw : r.xy;
            float radius = (p.y > 0.0) ? rSelected.y : rSelected.x;
            float2 q = abs(p) - b + radius;
            float2 s = sign(p);
            if (q.x > 0.0 && q.y > 0.0) {
                return s * normalize(q);
            }
            return s * ((q.x > q.y) ? float2(1.0, 0.0) : float2(0.0, 1.0));
        }

        float circleMap(float x) {
            float clamped = clamp(x, 0.0, 1.0);
            return 1.0 - sqrt(max(0.0, 1.0 - clamped * clamped));
        }

        half4 main(float2 fragCoord) {
            float2 safeSize = max(size, float2(2.0, 2.0));
            float2 halfSize = safeSize * 0.5;
            float2 centeredCoord = fragCoord - halfSize;

            float minHalf = min(halfSize.x, halfSize.y);
            float4 safeRadii = clamp(cornerRadii, float4(1.0), float4(minHalf));

            float sd = sdRoundedBox(centeredCoord, halfSize, safeRadii);
            if (sd > 0.5) {
                return half4(0.0);
            }

            float safeRefractionH = max(refractionHeight, 1.0);
            float normalizedDepth = clamp(1.0 + sd / safeRefractionH, 0.0, 1.0);
            float meniscusFactor = circleMap(normalizedDepth);

            float2 grad = gradSdRoundedBox(centeredCoord, halfSize, safeRadii);
            float2 normCenter = centeredCoord / max(halfSize, float2(1.0));
            float2 combinedNormal = normalize(grad + normCenter * eccentricFactor + float2(0.0001, 0.0001));

            float2 displaceVec = combinedNormal * (meniscusFactor * (-refractionAmount));
            float2 chromaShift = combinedNormal * (meniscusFactor * chromaticAberration * 1.6);

            float2 posR = clamp(fragCoord + displaceVec * 0.95 - chromaShift, float2(0.0), safeSize);
            float2 posG = clamp(fragCoord + displaceVec, float2(0.0), safeSize);
            float2 posB = clamp(fragCoord + displaceVec * 1.05 + chromaShift, float2(0.0), safeSize);

            half4 colorR = content.eval(posR);
            half4 colorG = content.eval(posG);
            half4 colorB = content.eval(posB);

            half4 refracted = half4(
                colorR.r,
                mix(colorG.g, colorB.g, half(meniscusFactor * 0.12)),
                colorB.b,
                colorG.a
            );

            float2 lightDir = float2(cos(lightAngle), sin(lightAngle));
            float dirAlignment = max(0.0, dot(grad, lightDir));
            float oppositeAlignment = max(0.0, dot(grad, -lightDir)) * 0.45;
            float rimEdgeMask = smoothstep(-safeRefractionH * 0.65, 0.0, sd);
            float bevelSpec = (dirAlignment * dirAlignment + oppositeAlignment * oppositeAlignment) * rimEdgeMask * highlightStrength;

            half totalHighlight = half(clamp(bevelSpec, 0.0, 0.42));
            refracted.rgb = clamp(refracted.rgb + half3(totalHighlight), half3(0.0), half3(1.0));

            return refracted;
        }
    """

    @RequiresApi(Build.VERSION_CODES.S)
    fun createSdfLiquidGlassRenderEffect(
        radiusX: Float,
        radiusY: Float,
        saturation: Float = 1.85f,
        width: Float = 0f,
        height: Float = 0f,
        cornerRadiusPx: Float = 0f,
        refractionHeightPx: Float = 20f,
        refractionAmountPx: Float = 24f,
        chromaticAberration: Float = 0.55f,
        lightAngleRad: Float = (-PI / 4.0).toFloat(),
        highlightStrength: Float = 0.20f,
        enableEdgeRefraction: Boolean = true
    ): AndroidRenderEffect {
        val safeRx = radiusX
        val safeRy = radiusY
        val blurEffect = if (safeRx >= 0.5f && safeRy >= 0.5f && !safeRx.isNaN() && !safeRy.isNaN() && !safeRx.isInfinite() && !safeRy.isInfinite()) {
            runCatching {
                AndroidRenderEffect.createBlurEffect(
                    safeRx,
                    safeRy,
                    Shader.TileMode.CLAMP
                )
            }.getOrNull()
        } else null
        val vibrancyMatrix = ColorMatrix().apply {
            setSaturation(saturation)
        }
        val vibrancyEffect = AndroidRenderEffect.createColorFilterEffect(
            ColorMatrixColorFilter(vibrancyMatrix)
        )
        val vibrancyBlurChain = if (blurEffect != null) {
            AndroidRenderEffect.createChainEffect(
                vibrancyEffect,
                blurEffect
            )
        } else {
            vibrancyEffect
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && enableEdgeRefraction && width > 4f && height > 4f) {
            return runCatching {
                val maxBound = max(1f, min(width, height) * 0.5f)
                val cr = if (cornerRadiusPx > 1f) cornerRadiusPx.coerceIn(1f, maxBound) else maxBound
                val safeH = refractionHeightPx.coerceIn(1f, maxBound)
                val shader = RuntimeShader(AGSL_SDF_LIQUID_GLASS_SOURCE).apply {
                    setFloatUniform("size", width, height)
                    setFloatUniform("cornerRadii", cr, cr, cr, cr)
                    setFloatUniform("refractionHeight", safeH)
                    setFloatUniform("refractionAmount", refractionAmountPx)
                    setFloatUniform("eccentricFactor", 0.16f)
                    setFloatUniform("chromaticAberration", chromaticAberration)
                    setFloatUniform("lightAngle", lightAngleRad)
                    setFloatUniform("highlightStrength", highlightStrength)
                }
                val sdfEffect = AndroidRenderEffect.createRuntimeShaderEffect(shader, "content")
                AndroidRenderEffect.createChainEffect(sdfEffect, vibrancyBlurChain)
            }.getOrDefault(vibrancyBlurChain)
        }

        return vibrancyBlurChain
    }
}

/**
 * Cache de `RenderEffect` por instância de componente para NUNCA recompilar `RuntimeShader`
 * nem realocar `RenderEffect` durante os frames de rolagem.
 */
@Stable
internal class CachedLiquidGlassEffectHolder {
    private var lastW: Int = -1
    private var lastH: Int = -1
    private var lastBlur: Float = -1f
    private var lastRefraction: Boolean = false
    private var cachedEffect: RenderEffect? = null
    private var appliedToLayerEffect: RenderEffect? = null

    fun applyToLayer(
        layer: GraphicsLayer,
        width: Int,
        height: Int,
        blurRadius: Float,
        enableEdgeRefraction: Boolean
    ) {
        layer.clip = true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || blurRadius < 0.5f || blurRadius.isNaN() || blurRadius.isInfinite() || width <= 1 || height <= 1) {
            if (appliedToLayerEffect != null) {
                layer.renderEffect = null
                appliedToLayerEffect = null
            }
            return
        }
        val effect = getOrCreate(width, height, blurRadius, enableEdgeRefraction)
        if (effect !== appliedToLayerEffect) {
            layer.renderEffect = effect
            appliedToLayerEffect = effect
        }
    }

    fun getOrCreate(
        width: Int,
        height: Int,
        blurRadius: Float,
        enableEdgeRefraction: Boolean
    ): RenderEffect? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || blurRadius < 0.5f || blurRadius.isNaN() || blurRadius.isInfinite() || width <= 1 || height <= 1) {
            return null
        }
        if (cachedEffect != null &&
            lastW == width &&
            lastH == height &&
            abs(lastBlur - blurRadius) < 0.1f &&
            lastRefraction == enableEdgeRefraction
        ) {
            return cachedEffect
        }
        lastW = width
        lastH = height
        lastBlur = blurRadius
        lastRefraction = enableEdgeRefraction
        val wf = width.toFloat()
        val hf = height.toFloat()
        cachedEffect = SdfShader.createSdfLiquidGlassRenderEffect(
            radiusX = blurRadius,
            radiusY = blurRadius,
            saturation = 1.85f,
            width = wf,
            height = hf,
            cornerRadiusPx = min(wf, hf) * 0.5f,
            enableEdgeRefraction = enableEdgeRefraction
        ).asComposeRenderEffect()
        return cachedEffect
    }
}

/**
 * Controlador de Física Elástica Amortecida (`DampedDragAnimation.kt` do AndroidLiquidGlass-kmp por Kyant).
 * Usa `MutatorMutex` e física de mola amortecida sem criar corrotinas concorrentes por frame de gesto.
 */
@Stable
class DampedDragAnimation(
    private val animationScope: CoroutineScope,
    initialValue: Float,
    val valueRange: ClosedFloatingPointRange<Float> = 0f..1f
) {
    private val valueAnimationSpec = spring(1f, 1000f, 0.0001f)
    private val velocityAnimationSpec = spring(0.5f, 300f, 0.001f)
    private val pressProgressAnimationSpec = spring(1f, 1000f, 0.001f)

    private val valueAnimatable = Animatable(initialValue.coerceIn(valueRange), 0.0001f)
    private val velocityAnimatable = Animatable(0f, 0.001f)
    private val pressProgressAnimatable = Animatable(0f, 0.001f)

    private val mutatorMutex = MutatorMutex()
    private var lastDragTimestampMs: Long = 0L

    val value: Float
        get() = valueAnimatable.value

    val progress: Float
        get() {
            val span = valueRange.endInclusive - valueRange.start
            if (span <= 0.0001f || span.isNaN()) return 0f
            val p = (value - valueRange.start) / span
            return if (p.isNaN() || p.isInfinite()) 0f else p.coerceIn(0f, 1f)
        }

    val targetValue: Float
        get() = valueAnimatable.targetValue

    val pressProgress: Float
        get() = pressProgressAnimatable.value

    val velocity: Float
        get() = velocityAnimatable.value

    val scaleX: Float
        get() {
            val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(1f)
            val normVel = (abs(velocity) / span).coerceIn(0f, 1.8f)
            return 1f + pressProgress * 0.10f + normVel * 0.08f
        }

    val scaleY: Float
        get() {
            val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(1f)
            val normVel = (abs(velocity) / span).coerceIn(0f, 1.8f)
            return (1f + pressProgress * 0.14f - normVel * 0.04f).coerceAtLeast(0.92f)
        }

    fun press() {
        lastDragTimestampMs = System.currentTimeMillis()
        animationScope.launch {
            pressProgressAnimatable.animateTo(1f, pressProgressAnimationSpec)
        }
    }

    fun release() {
        animationScope.launch {
            pressProgressAnimatable.animateTo(0f, pressProgressAnimationSpec)
        }
        animationScope.launch {
            velocityAnimatable.animateTo(0f, velocityAnimationSpec)
        }
    }

    fun updateValue(value: Float, stiffness: Float = 1400f) {
        if (value.isNaN() || value.isInfinite()) return
        val targetValue = value.coerceIn(valueRange)
        val delta = targetValue - this.value
        if (abs(delta) < 0.0005f) return

        val now = System.currentTimeMillis()
        val dtSec = ((now - lastDragTimestampMs).coerceIn(8L, 100L)) / 1000f
        lastDragTimestampMs = now
        val rawInstVel = delta / dtSec
        val instVel = if (rawInstVel.isNaN() || rawInstVel.isInfinite()) 0f else rawInstVel.coerceIn(-6f, 6f)

        animationScope.launch {
            mutatorMutex.mutate {
                valueAnimatable.snapTo(targetValue)
                velocityAnimatable.snapTo(instVel * 0.35f)
            }
        }
    }

    fun animateToValue(value: Float) {
        if (value.isNaN() || value.isInfinite()) return
        val targetValue = value.coerceIn(valueRange)
        animationScope.launch {
            mutatorMutex.mutate {
                val rawVelocity = (targetValue - this@DampedDragAnimation.value) * 1.4f
                val velocity = if (rawVelocity.isNaN() || rawVelocity.isInfinite()) 0f else rawVelocity
                coroutineScope {
                    launch {
                        valueAnimatable.animateTo(targetValue, valueAnimationSpec)
                    }
                    launch {
                        velocityAnimatable.animateTo(velocity, velocityAnimationSpec)
                    }
                }
            }
        }
    }
}

@Composable
fun rememberDampedDragAnimation(
    initialValue: Float,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f
): DampedDragAnimation {
    val scope = rememberCoroutineScope()
    return remember(valueRange) {
        DampedDragAnimation(
            animationScope = scope,
            initialValue = initialValue,
            valueRange = valueRange
        )
    }
}

/**
 * Destaque Especular Interativo (`InteractiveHighlight.kt` do AndroidLiquidGlass-kmp por Kyant).
 * Segue a posição do toque com zero alocação de corrotina durante os eventos de movimento.
 */
@Stable
class InteractiveHighlight(
    val animationScope: CoroutineScope,
    val positionProvider: (size: Size, offset: Offset) -> Offset = { _, offset -> offset }
) {
    private val pressProgressAnimationSpec = spring(0.5f, 300f, 0.001f)
    private val pressProgressAnimatable = Animatable(0f, 0.001f)

    private var startPosition = Offset.Zero
    var touchPosition by mutableStateOf(Offset.Zero)
        private set

    val pressProgress: Float
        get() = pressProgressAnimatable.value

    val offset: Offset
        get() = touchPosition - startPosition

    val gestureModifier: Modifier = Modifier.pointerInput(animationScope) {
        awaitEachGesture {
            val down = awaitFirstDown(
                requireUnconsumed = false,
                pass = PointerEventPass.Initial
            )
            val canvasSize = Size(size.width.toFloat(), size.height.toFloat())
            startPosition = positionProvider(canvasSize, down.position)
            touchPosition = startPosition

            animationScope.launch {
                pressProgressAnimatable.animateTo(1f, pressProgressAnimationSpec)
            }

            try {
                do {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    val currentChange = event.changes.firstOrNull { it.pressed } ?: event.changes.firstOrNull()
                    if (currentChange != null) {
                        touchPosition = positionProvider(canvasSize, currentChange.position)
                    }
                } while (event.changes.any { it.pressed })
            } finally {
                animationScope.launch {
                    pressProgressAnimatable.animateTo(0f, pressProgressAnimationSpec)
                }
            }
        }
    }

    val modifier: Modifier = Modifier.drawWithContent {
        drawContent()
        val progress = pressProgressAnimatable.value
        if (progress > 0.005f) {
            val clampedCenter = Offset(
                x = touchPosition.x.coerceIn(0f, size.width),
                y = touchPosition.y.coerceIn(0f, size.height)
            )
            val radius = (size.maxDimension * 0.72f).coerceAtLeast(48f)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = (0.18f * progress).coerceIn(0f, 0.28f)),
                        Color.Transparent
                    ),
                    center = clampedCenter,
                    radius = radius
                ),
                topLeft = Offset.Zero,
                size = size,
                blendMode = BlendMode.Plus
            )
        }
    }
}

@Composable
fun rememberInteractiveHighlight(
    positionProvider: (size: Size, offset: Offset) -> Offset = { _, offset -> offset }
): InteractiveHighlight {
    val scope = rememberCoroutineScope()
    return remember {
        InteractiveHighlight(
            animationScope = scope,
            positionProvider = positionProvider
        )
    }
}

/**
 * Camada de transição progressiva na borda inferior (`ProgressiveBlurContent.kt` do AndroidLiquidGlass-kmp por Kyant).
 * Suaviza a chegada das capas das HQs sob a Dock Bar flutuante sem custo de realocação por frame.
 */
@Composable
fun ProgressiveBlurBackdrop(
    blurRadius: Float = 20f,
    tintColor: Color = Color.Transparent,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        tintColor.copy(alpha = (tintColor.alpha * 0.45f).coerceIn(0f, 0.28f)),
                        tintColor.copy(alpha = tintColor.alpha.coerceIn(0f, 0.48f))
                    )
                )
            )
    )
}
