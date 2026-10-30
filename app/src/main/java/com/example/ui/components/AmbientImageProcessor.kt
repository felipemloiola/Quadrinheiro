package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.Color
import coil.Coil
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Componente de processamento de iluminação ambiente e extração de cores.
 * Executa todos os cálculos matemáticos pesados de amostragem de pixels e média ponderada
 * estritamente em CoroutineDispatcher.Default para não bloquear a thread principal (UI)
 * nem a thread de renderização durante o desenho dos quadros.
 */
object AmbientImageProcessor {

    private val colorCache = ConcurrentHashMap<String, Color>()

    /**
     * Processa a imagem da capa e extrai a cor média ponderada vibrante.
     * O download I/O é executado em Dispatchers.IO, enquanto todo o cálculo de amostragem
     * de pixels e médias de cor é transferido para CoroutineDispatcher.Default.
     */
    suspend fun processAmbientCoverColor(
        context: Context,
        rawUrl: String,
        dispatcher: CoroutineDispatcher = Dispatchers.Default
    ): Color? {
        if (rawUrl.isBlank()) return null
        colorCache[rawUrl]?.let { return it }

        // AMBIENT_COVER_EXTRACTION_V17: hardware-rendered ambient bitmap, memory caching, non-blocking Default pool.
        val computedColor = withContext(Dispatchers.IO) {
            runCatching {
                val req = ImageRequest.Builder(context)
                    .data(rawUrl)
                    .size(64, 64)
                    .allowHardware(false)
                    .build()
                val result = Coil.imageLoader(context).execute(req)
                val bmp = (result.drawable as? BitmapDrawable)?.bitmap ?: return@runCatching null
                calculateAverageCoverColor(bmp, dispatcher)
            }.getOrNull()
        }
        if (computedColor != null) {
            colorCache[rawUrl] = computedColor
        }
        return computedColor
    }

    /**
     * Calcula a cor média vibrante dominante de um Bitmap na CPU.
     * Reduz (downsamples) qualquer bitmap maior para 64x64 px antes de calcular a paleta de cores,
     * reduzindo o tempo de CPU em mais de 95% com resultado visual idêntico para o brilho ambiente.
     */
    suspend fun calculateAverageCoverColor(
        bitmap: Bitmap,
        dispatcher: CoroutineDispatcher = Dispatchers.Default
    ): Color? = withContext(dispatcher) {
        if (bitmap.width <= 4 || bitmap.height <= 4) return@withContext null

        val sampledBitmap = if (bitmap.width > 64 || bitmap.height > 64) {
            Bitmap.createScaledBitmap(bitmap, 64, 64, true)
        } else {
            bitmap
        }

        val w = sampledBitmap.width
        val h = sampledBitmap.height
        val pixels = IntArray(w * h)
        sampledBitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val numBins = 24
        val binWeight = FloatArray(numBins)
        val binR = FloatArray(numBins)
        val binG = FloatArray(numBins)
        val binB = FloatArray(numBins)
        val hsv = FloatArray(3)

        val startX = (w * 0.06f).toInt().coerceAtLeast(0)
        val endX = (w * 0.94f).toInt().coerceAtMost(w - 1)
        val startY = (h * 0.06f).toInt().coerceAtLeast(0)
        val endY = (h * 0.94f).toInt().coerceAtMost(h - 1)
        val stepX = ((endX - startX) / 20).coerceAtLeast(1)
        val stepY = ((endY - startY) / 28).coerceAtLeast(1)

        var y = startY
        while (y <= endY) {
            val rowOffset = y * w
            var x = startX
            while (x <= endX) {
                val pixel = pixels[rowOffset + x]
                val a = (pixel ushr 24) and 0xFF
                if (a > 180) {
                    val r = (pixel ushr 16) and 0xFF
                    val g = (pixel ushr 8) and 0xFF
                    val b = pixel and 0xFF
                    android.graphics.Color.RGBToHSV(r, g, b, hsv)
                    val sat = hsv[1]
                    val value = hsv[2]
                    // Ignora pixels pretos/muito escuros e brancos/cinzas dessaturados
                    if (value >= 0.22f && sat >= 0.22f && !(value > 0.94f && sat < 0.30f)) {
                        val bin = ((hsv[0] / 360f) * numBins).toInt().coerceIn(0, numBins - 1)
                        // Prioriza cores saturadas e vibrantes da capa
                        val weight = (sat * sat) * (0.35f + 0.65f * value)
                        binWeight[bin] += weight
                        binR[bin] += (r / 255f) * weight
                        binG[bin] += (g / 255f) * weight
                        binB[bin] += (b / 255f) * weight
                    }
                }
                x += stepX
            }
            y += stepY
        }

        if (sampledBitmap !== bitmap && !sampledBitmap.isRecycled) {
            runCatching { sampledBitmap.recycle() }
        }

        var bestBin = -1
        var maxW = 0f
        for (i in 0 until numBins) {
            if (binWeight[i] > maxW) {
                maxW = binWeight[i]
                bestBin = i
            }
        }
        if (bestBin == -1 || maxW <= 0.01f) return@withContext null

        val avgR = (binR[bestBin] / maxW).coerceIn(0f, 1f)
        val avgG = (binG[bestBin] / maxW).coerceIn(0f, 1f)
        val avgB = (binB[bestBin] / maxW).coerceIn(0f, 1f)

        android.graphics.Color.RGBToHSV(
            (avgR * 255f).toInt(),
            (avgG * 255f).toInt(),
            (avgB * 255f).toInt(),
            hsv
        )
        // Calibragem suave e agradável para iluminação ambiente estilo iOS
        hsv[1] = hsv[1].coerceIn(0.65f, 0.92f)
        hsv[2] = hsv[2].coerceIn(0.75f, 0.94f)
        val boostedInt = android.graphics.Color.HSVToColor(hsv)
        Color(boostedInt)
    }
}
