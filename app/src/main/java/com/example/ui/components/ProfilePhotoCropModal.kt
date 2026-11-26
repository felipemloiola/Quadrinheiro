package com.example.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.LocalAppleMaterials
import com.example.ui.theme.SpaceGroteskTabularFamily
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.ContinuousCapsule
import com.kyant.shapes.ContinuousRoundedRectangle
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Decodifica um Bitmap a partir da Uri selecionada com proteção de memória (inSampleSize)
 */
fun decodeAvatarBitmapFromUri(
    context: Context,
    uri: Uri,
    maxDimension: Int = 1280
): Bitmap? {
    return try {
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            com.example.data.SafeImageFormats.decodeStreamSafely(stream, boundsOptions)
        }
        val rawW = boundsOptions.outWidth
        val rawH = boundsOptions.outHeight
        if (rawW <= 0 || rawH <= 0) return null

        var sampleSize = 1
        while (rawW / sampleSize > maxDimension || rawH / sampleSize > maxDimension) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            com.example.data.SafeImageFormats.decodeStreamSafely(stream, decodeOptions)
        }
    } catch (_: Throwable) {
        null
    }
}

/**
 * Recorta a região circular/quadrada visível de acordo com o zoom e deslocamento (offset) definidos pelo usuário
 * e retorna a string Base64 ("data:image/jpeg;base64,...").
 */
fun renderCroppedAvatarBase64(
    source: Bitmap,
    viewportSizePx: Float,
    circleRadiusPx: Float,
    userZoom: Float,
    panOffset: Offset,
    outputSizePx: Int = 240
): String {
    val srcW = source.width.toFloat().coerceAtLeast(1f)
    val srcH = source.height.toFloat().coerceAtLeast(1f)
    val circleDiameterPx = (circleRadiusPx * 2f).coerceAtLeast(1f)

    // Escala base que faz o menor lado da imagem preencher o diâmetro do círculo de prévia em zoom = 1.0f
    val baseScale = max(circleDiameterPx / srcW, circleDiameterPx / srcH)
    val totalScale = baseScale * userZoom.coerceIn(1f, 4f)

    val outputBitmap = Bitmap.createBitmap(outputSizePx, outputSizePx, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(outputBitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    // Mapeia o quadrado inscrito na janela circular (centro da viewport, lado = circleDiameterPx)
    // para o bitmap de saída (0..outputSizePx)
    val viewportToOutputRatio = outputSizePx.toFloat() / circleDiameterPx

    val matrix = Matrix().apply {
        // 1. Centraliza a imagem na origem (0, 0)
        postTranslate(-srcW / 2f, -srcH / 2f)
        // 2. Aplica a escala total (baseScale * userZoom) na coordenada da viewport
        postScale(totalScale, totalScale)
        // 3. Aplica o deslocamento do usuário na viewport
        postTranslate(panOffset.x, panOffset.y)
        // 4. Converte da coordenada da janela circular centrada em (0,0) para o bitmap final (0..outputSizePx)
        postScale(viewportToOutputRatio, viewportToOutputRatio)
        postTranslate(outputSizePx / 2f, outputSizePx / 2f)
    }

    canvas.drawBitmap(source, matrix, paint)

    val baos = ByteArrayOutputStream()
    outputBitmap.compress(Bitmap.CompressFormat.JPEG, 86, baos)
    return "data:image/jpeg;base64," + Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
}

internal fun zoomAroundCentroid(
    offset: Offset, centroid: Offset, pan: Offset, oldZoom: Float, newZoom: Float
): Offset = centroid + (offset - centroid) * (newZoom / oldZoom) + pan

@Composable
fun ProfilePhotoCropModal(
    bitmap: Bitmap,
    onDismiss: () -> Unit,
    onConfirmCrop: (String) -> Unit
) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme
    val motion by androidx.compose.runtime.rememberUpdatedState(com.example.ui.theme.LocalAppMotion.current)
    val scope = rememberCoroutineScope()
    var settleJob by remember { mutableStateOf<Job?>(null) }
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    var userZoom by remember(bitmap) { mutableFloatStateOf(1f) }
    var panOffset by remember(bitmap) { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    val viewportSidePx = min(viewportSize.width, viewportSize.height).toFloat().coerceAtLeast(1f)
    // Raio do círculo de prévia ocupa 82% da área útil do visor
    val circleRadiusPx = viewportSidePx * 0.41f
    val circleDiameterPx = circleRadiusPx * 2f

    val srcW = bitmap.width.toFloat().coerceAtLeast(1f)
    val srcH = bitmap.height.toFloat().coerceAtLeast(1f)
    val baseScale = max(circleDiameterPx / srcW, circleDiameterPx / srcH)
    val viewportShape = remember { ContinuousRoundedRectangle(28.dp) }
    val modalBackdrop = rememberLayerBackdrop()

    fun clampOffset(candidate: Offset, zoom: Float): Offset {
        val scaledW = srcW * baseScale * zoom
        val scaledH = srcH * baseScale * zoom
        val maxDx = max(0f, (scaledW - circleDiameterPx) / 2f)
        val maxDy = max(0f, (scaledH - circleDiameterPx) / 2f)
        return Offset(
            x = candidate.x.coerceIn(-maxDx, maxDx),
            y = candidate.y.coerceIn(-maxDy, maxDy)
        )
    }

    val settings = com.example.ui.theme.LocalAppSettings.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(modalBackdrop)
                    .background(
                        if (settings.reduceTransparency) {
                            Brush.verticalGradient(listOf(Color(0xFF0B0C10), Color(0xFF0B0C10)))
                        } else {
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xF20B0C10),
                                    s.accentColor.copy(alpha = 0.14f),
                                    Color(0xF50B0C10)
                                )
                            )
                        }
                    )
            )

            CompositionLocalProvider(LocalLiquidGlassBackdrop provides modalBackdrop) {
                Column(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .fillMaxWidth()
                        .widthIn(max = 460.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ajustar Foto de Perfil",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.semantics { heading() }
                            )
                            Text(
                                text = "Arraste, dê zoom ou use os botões abaixo",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.78f)
                            )
                        }

                        LiquidGlassPillButton(
                            text = "Centralizar",
                            onClick = {
                                userZoom = 1f
                                panOffset = Offset.Zero
                            },
                            leadingIcon = Icons.Default.Replay,
                            onDarkScrim = true,
                            modifier = Modifier.testTag("reset_crop_avatar_button")
                        )
                    }

                    // Visor interativo com gestos de pinça/arraste + máscara circular de prévia
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(viewportShape)
                            .background(Color(0xFF14161D))
                            .border(1.dp, Color.White.copy(alpha = 0.18f), viewportShape)
                            .onSizeChanged { viewportSize = it }
                            .pointerInput(bitmap, viewportSidePx, motion.reduced) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    settleJob?.cancel()
                                    var totalPan = Offset.Zero
                                    var totalZoom = 1f
                                    var claimed = false
                                    try {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            if (event.changes.none { it.pressed } || event.changes.any { it.isConsumed }) break
                                            val zoom = event.calculateZoom()
                                            val pan = event.calculatePan()
                                            val centroid = event.calculateCentroid(useCurrent = false)
                                            totalPan += pan
                                            totalZoom *= zoom
                                            if (!claimed) claimed = totalPan.getDistance() > viewConfiguration.touchSlop ||
                                                kotlin.math.abs(1f - totalZoom) * viewportSidePx > viewConfiguration.touchSlop
                                            if (claimed) {
                                                val nextZoom = (userZoom * zoom).coerceIn(1f, 4f)
                                                val center = Offset(viewportSidePx / 2f, viewportSidePx / 2f)
                                                val candidate = zoomAroundCentroid(panOffset, centroid - center, pan, userZoom, nextZoom)
                                                val clamped = clampOffset(candidate, nextZoom)
                                                val excess = candidate - clamped
                                                panOffset = clamped + Offset(
                                                    sheetResistance(-kotlin.math.abs(excess.x), viewportSidePx) * -kotlin.math.sign(excess.x),
                                                    sheetResistance(-kotlin.math.abs(excess.y), viewportSidePx) * -kotlin.math.sign(excess.y)
                                                )
                                                userZoom = nextZoom
                                                event.changes.forEach { it.consume() }
                                            }
                                        }
                                    } finally {
                                        val target = clampOffset(panOffset, userZoom)
                                        settleJob = scope.launch {
                                            val animation = Animatable(panOffset, Offset.VectorConverter)
                                            if (motion.reduced) panOffset = target
                                            else animation.animateTo(target, motion.spring(dampingRatio = 1f, stiffness = 500f)) {
                                                panOffset = value
                                            }
                                        }
                                    }
                                }
                            }
                            .testTag("avatar_crop_viewport"),
                        contentAlignment = Alignment.Center
                    ) {
                        val fitScale = min(viewportSidePx / srcW, viewportSidePx / srcH).coerceAtLeast(0.0001f)
                        val relativeScale = (baseScale * userZoom) / fitScale

                        Image(
                            bitmap = imageBitmap,
                            contentDescription = "Foto selecionada para recorte",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = relativeScale
                                    scaleY = relativeScale
                                    translationX = panOffset.x
                                    translationY = panOffset.y
                                }
                        )

                        // Máscara escura externa com recorte circular transparente (BlendMode.Clear) + anel de prévia
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    compositingStrategy = CompositingStrategy.Offscreen
                                }
                        ) {
                            val canvasCenter = Offset(size.width / 2f, size.height / 2f)
                            val r = min(size.width, size.height) * 0.41f

                            // Escurece a área fora do círculo mantendo contexto visual
                            drawRect(color = Color.Black.copy(alpha = 0.62f))

                            // Abre o círculo de prévia transparente
                            drawCircle(
                                color = Color.Transparent,
                                radius = r,
                                center = canvasCenter,
                                blendMode = BlendMode.Clear
                            )

                            // Grades sutis de auxílio dentro do círculo
                            val gridColor = Color.White.copy(alpha = 0.16f)
                            val third = (r * 2f) / 3f
                            val left = canvasCenter.x - r
                            val top = canvasCenter.y - r
                            for (i in 1..2) {
                                val x = left + third * i
                                val y = top + third * i
                                drawLine(
                                    color = gridColor,
                                    start = Offset(x, canvasCenter.y - r * 0.86f),
                                    end = Offset(x, canvasCenter.y + r * 0.86f),
                                    strokeWidth = 1.dp.toPx()
                                )
                                drawLine(
                                    color = gridColor,
                                    start = Offset(canvasCenter.x - r * 0.86f, y),
                                    end = Offset(canvasCenter.x + r * 0.86f, y),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }

                            // Anel principal do círculo de prévia
                            drawCircle(
                                color = Color.White.copy(alpha = 0.90f),
                                radius = r,
                                center = canvasCenter,
                                style = Stroke(width = 2.2.dp.toPx())
                            )

                            // Anel tracejado interno sutil
                            drawCircle(
                                color = s.accentColor.copy(alpha = 0.55f),
                                radius = r - 4.dp.toPx(),
                                center = canvasCenter,
                                style = Stroke(
                                    width = 1.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                                )
                            )
                        }

                        // Selo informativo na base do visor
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp)
                                .clip(ContinuousCapsule)
                                .background(Color.Black.copy(alpha = 0.64f), ContinuousCapsule)
                                .border(0.6.dp, Color.White.copy(alpha = 0.22f), ContinuousCapsule)
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "Círculo de prévia • ${(userZoom * 100).roundToInt()}%",
                                fontSize = 11.sp,
                                fontFamily = SpaceGroteskTabularFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        }
                    }

                    // Controles acessíveis de Zoom e Enquadramento (TalkBack / Teclado / Toque)
                    CropPositionControls(
                        zoom = userZoom,
                        zoomRange = 1f..4f,
                        onZoomChange = { nextZoom ->
                            val clampedZoom = nextZoom.coerceIn(1f, 4f)
                            userZoom = clampedZoom
                            panOffset = clampOffset(panOffset, clampedZoom)
                        },
                        onNudge = { dxFrac, dyFrac ->
                            val step = viewportSidePx * 0.12f
                            panOffset = clampOffset(
                                Offset(panOffset.x + step * (dxFrac / 0.12f), panOffset.y + step * (dyFrac / 0.12f)),
                                userZoom
                            )
                        },
                        onReset = {
                            userZoom = 1f
                            panOffset = Offset.Zero
                        },
                        onDarkScrim = true,
                        zoomSliderTag = "avatar_zoom_slider"
                    )

                    // Botões de Ação: Cancelar e Salvar Foto
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LiquidGlassPillButton(
                            text = "Cancelar",
                            onClick = onDismiss,
                            leadingIcon = Icons.Default.Close,
                            onDarkScrim = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("cancel_crop_avatar_button")
                        )

                        LiquidGlassPillButton(
                            text = "Salvar Foto",
                            onClick = {
                                val croppedB64 = renderCroppedAvatarBase64(
                                    source = bitmap,
                                    viewportSizePx = viewportSidePx,
                                    circleRadiusPx = circleRadiusPx,
                                    userZoom = userZoom,
                                    panOffset = panOffset,
                                    outputSizePx = 240
                                )
                                onConfirmCrop(croppedB64)
                            },
                            emphasized = true,
                            leadingIcon = Icons.Default.Check,
                            modifier = Modifier
                                .weight(1.25f)
                                .testTag("confirm_crop_avatar_button")
                        )
                    }
                }
            }
        }
    }
}
