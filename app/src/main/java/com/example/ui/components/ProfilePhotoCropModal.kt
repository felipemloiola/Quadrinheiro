package com.example.ui.components

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.LocalAppleMaterials
import com.example.ui.theme.SpaceGroteskTabularFamily
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
            BitmapFactory.decodeStream(stream, null, boundsOptions)
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
            BitmapFactory.decodeStream(stream, null, decodeOptions)
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

@Composable
fun ProfilePhotoCropModal(
    bitmap: Bitmap,
    onDismiss: () -> Unit,
    onConfirmCrop: (String) -> Unit
) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF20B0C10))
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ajustar Foto de Perfil",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Arraste e dê zoom para enquadrar no círculo",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.68f)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(0.8.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
                            .clickable {
                                userZoom = 1f
                                panOffset = Offset.Zero
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("reset_crop_avatar_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "Centralizar foto",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Centralizar",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                // Visor interativo com gestos de pinça/arraste + máscara circular de prévia
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF14161D))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(28.dp))
                        .onSizeChanged { viewportSize = it }
                        .pointerInput(bitmap, viewportSidePx) {
                            detectTransformGestures { _, pan, gestureZoom, _ ->
                                val nextZoom = (userZoom * gestureZoom).coerceIn(1f, 4f)
                                val nextOffset = clampOffset(panOffset + pan, nextZoom)
                                userZoom = nextZoom
                                panOffset = nextOffset
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
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .border(0.6.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(999.dp))
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

                // Controle deslizante de Zoom + Miniatura real da prévia circular
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Miniatura ao vivo do avatar circular final
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1A1B20))
                            .border(1.5.dp, s.accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val miniSidePx = 52f
                        val ratio = if (circleDiameterPx > 0f) miniSidePx / circleDiameterPx else 1f
                        val fitScale = min(viewportSidePx / srcW, viewportSidePx / srcH).coerceAtLeast(0.0001f)
                        val relativeScale = (baseScale * userZoom) / fitScale
                        val viewportToCircleRatio = if (circleDiameterPx > 0f) viewportSidePx / circleDiameterPx else 1f

                        Image(
                            bitmap = imageBitmap,
                            contentDescription = "Mini prévia circular",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = relativeScale * viewportToCircleRatio
                                    scaleY = relativeScale * viewportToCircleRatio
                                    translationX = panOffset.x * ratio * density
                                    translationY = panOffset.y * ratio * density
                                }
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Zoom da foto",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = String.format(java.util.Locale.US, "%.1fx", userZoom),
                                fontSize = 12.sp,
                                fontFamily = SpaceGroteskTabularFamily,
                                fontWeight = FontWeight.Bold,
                                color = s.accentColor
                            )
                        }
                        Slider(
                            value = userZoom,
                            onValueChange = { nextZoom ->
                                val clampedZoom = nextZoom.coerceIn(1f, 4f)
                                userZoom = clampedZoom
                                panOffset = clampOffset(panOffset, clampedZoom)
                            },
                            valueRange = 1f..4f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = s.accentColor,
                                inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("avatar_zoom_slider")
                        )
                    }
                }

                // Botões de Ação: Cancelar e Salvar Foto
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = 0.10f))
                            .border(0.8.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
                            .clickable { onDismiss() }
                            .testTag("cancel_crop_avatar_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Cancelar",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(s.accentBg)
                            .clickable {
                                val croppedB64 = renderCroppedAvatarBase64(
                                    source = bitmap,
                                    viewportSizePx = viewportSidePx,
                                    circleRadiusPx = circleRadiusPx,
                                    userZoom = userZoom,
                                    panOffset = panOffset,
                                    outputSizePx = 240
                                )
                                onConfirmCrop(croppedB64)
                            }
                            .testTag("confirm_crop_avatar_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = s.accentText,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Salvar Foto",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = s.accentText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
