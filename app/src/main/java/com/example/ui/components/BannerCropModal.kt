package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalIOS27Theme
import com.kyant.shapes.ContinuousRoundedRectangle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

internal fun renderBannerCrop(bitmap: Bitmap, viewport: IntSize, zoom: Float, offset: Offset): String {
    val safeW = viewport.width.coerceAtLeast(1)
    val safeH = viewport.height.coerceAtLeast(1)
    val base = max(safeW.toFloat() / bitmap.width, safeH.toFloat() / bitmap.height)
    val output = Bitmap.createBitmap(1200, 400, Bitmap.Config.ARGB_8888)
    val ratio = 1200f / safeW
    val matrix = Matrix().apply {
        postTranslate(-bitmap.width / 2f, -bitmap.height / 2f)
        postScale(base * zoom, base * zoom)
        postTranslate(safeW / 2f + offset.x, safeH / 2f + offset.y)
        postScale(ratio, ratio)
    }
    Canvas(output).drawBitmap(bitmap, matrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    val bytes = ByteArrayOutputStream()
    var quality = 88
    do {
        bytes.reset()
        output.compress(Bitmap.CompressFormat.JPEG, quality, bytes)
        quality -= 8
    } while (bytes.size() > 140000 && quality >= 32)
    output.recycle()
    return "data:image/jpeg;base64," + Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP)
}

@Composable
fun BannerCropModal(bitmap: Bitmap, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val t = LocalIOS27Theme.current
    val scope = rememberCoroutineScope()
    var zoom by remember(bitmap) { mutableFloatStateOf(1f) }
    var offset by remember(bitmap) { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize(300, 100)) }
    var busy by remember { mutableStateOf(false) }
    val viewportShape = remember { ContinuousRoundedRectangle(18.dp) }

    fun clamp(candidate: Offset, z: Float): Offset {
        val w = viewport.width.coerceAtLeast(1)
        val h = viewport.height.coerceAtLeast(1)
        val base = max(w.toFloat() / bitmap.width, h.toFloat() / bitmap.height)
        val dx = max(0f, (bitmap.width * base * z - w) / 2f)
        val dy = max(0f, (bitmap.height * base * z - h) / 2f)
        return Offset(candidate.x.coerceIn(-dx, dx), candidate.y.coerceIn(-dy, dy))
    }

    LiquidGlassDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        maxWidth = 520.dp,
        dismissOnScrimClick = !busy
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .testTag("banner_crop"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ajustar banner",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = t.textPrimary,
                    modifier = Modifier.semantics { heading() }
                )
                LiquidGlassPillButton(
                    text = "Repor",
                    onClick = { zoom = 1f; offset = Offset.Zero },
                    enabled = !busy,
                    leadingIcon = Icons.Default.Replay,
                    modifier = Modifier.testTag("banner_crop_reset")
                )
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f)
                    .clip(viewportShape)
                    .background(t.bgPrimary)
                    .border(1.dp, t.divider, viewportShape)
                    .onSizeChanged { if (it.width > 0 && it.height > 0) viewport = it }
                    .pointerInput(bitmap) {
                        detectTransformGestures { centroid, pan, change, _ ->
                            val next = (zoom * change).coerceIn(1f, 4f)
                            offset = clamp(
                                zoomAroundCentroid(
                                    offset,
                                    centroid - Offset(viewport.width / 2f, viewport.height / 2f),
                                    pan,
                                    zoom,
                                    next
                                ),
                                next
                            )
                            zoom = next
                        }
                    }
                    .graphicsLayer { clip = true }
                    .semantics { contentDescription = "Área de recorte do banner. Use gestos ou os botões de enquadramento abaixo." }
                    .testTag("banner_crop_viewport")
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Prévia do banner",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = zoom
                            scaleY = zoom
                            translationX = offset.x
                            translationY = offset.y
                        },
                    contentScale = ContentScale.Crop
                )
            }

            Text(
                text = "Arraste para enquadrar, use dois dedos ou os controles de precisão abaixo.",
                style = MaterialTheme.typography.bodySmall,
                color = t.textSecondary
            )

            CropPositionControls(
                zoom = zoom,
                zoomRange = 1f..4f,
                onZoomChange = { next ->
                    zoom = next
                    offset = clamp(offset, next)
                },
                onNudge = { dxFrac, dyFrac ->
                    val stepX = viewport.width.coerceAtLeast(1) * dxFrac
                    val stepY = viewport.height.coerceAtLeast(1) * dyFrac
                    offset = clamp(Offset(offset.x + stepX, offset.y + stepY), zoom)
                },
                onReset = {
                    zoom = 1f
                    offset = Offset.Zero
                },
                enabled = !busy,
                onDarkScrim = false,
                zoomSliderTag = "banner_zoom_slider"
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiquidGlassPillButton(
                    text = "Cancelar",
                    onClick = onDismiss,
                    enabled = !busy,
                    contentTint = rememberAdaptiveLiquidGlassInk(t.textSecondary, t.bgSecondary, 4.5f),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("banner_crop_cancel")
                )
                LiquidGlassPillButton(
                    text = if (busy) "Salvando…" else "Concluir",
                    onClick = {
                        busy = true
                        scope.launch {
                            val result = withContext(Dispatchers.Default) {
                                renderBannerCrop(bitmap, viewport, zoom, offset)
                            }
                            onConfirm(result)
                        }
                    },
                    enabled = !busy,
                    emphasized = true,
                    loading = busy,
                    leadingIcon = Icons.Default.Check,
                    modifier = Modifier
                        .weight(1.25f)
                        .testTag("banner_crop_confirm")
                )
            }
        }
    }
}
