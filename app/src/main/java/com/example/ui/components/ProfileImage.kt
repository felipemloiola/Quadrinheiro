package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

internal object ProfileBitmapMemoryCache {
    private val cache = object : LruCache<String, ImageBitmap>(24) {}

    fun getCached(dataUri: String): ImageBitmap? {
        if (!dataUri.startsWith("data:image")) return null
        return synchronized(cache) { cache.get(cacheKey(dataUri)) }
    }

    fun decodeAndCache(dataUri: String): ImageBitmap? {
        if (!dataUri.startsWith("data:image")) return null
        val key = cacheKey(dataUri)
        synchronized(cache) { cache.get(key) }?.let { return it }
        return try {
            val base64Part = dataUri.substringAfter("base64,", "")
            if (base64Part.isEmpty()) return null
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            if (bmp != null) {
                synchronized(cache) { cache.put(key, bmp) }
            }
            bmp
        } catch (_: Throwable) {
            null
        }
    }

    fun prewarmAll(rawSources: Iterable<String>) {
        for (raw in rawSources) {
            if (raw.isBlank()) continue
            val resolved = com.example.data.ProfileShowcase.characterImage(raw).trim()
            if (resolved.startsWith("data:image")) {
                decodeAndCache(resolved)
            }
        }
    }

    private fun cacheKey(dataUri: String): String =
        "${dataUri.length}:${dataUri.hashCode()}:${dataUri.takeLast(32)}"
}

@Composable
internal fun ProfileImage(source: String, description: String?, modifier: Modifier = Modifier, face: Boolean = false) {
    val context = LocalContext.current
    val resolved = remember(source) { com.example.data.ProfileShowcase.characterImage(source).trim() }
    val alignment = if (face) Alignment.TopCenter else Alignment.Center
    if (resolved.startsWith("data:image")) {
        var bitmap by remember(resolved) { mutableStateOf(ProfileBitmapMemoryCache.getCached(resolved)) }
        LaunchedEffect(resolved) {
            if (bitmap == null) {
                bitmap = withContext(Dispatchers.Default) {
                    ProfileBitmapMemoryCache.decodeAndCache(resolved)
                }
            }
        }
        val currentBmp = bitmap
        if (currentBmp != null) {
            Image(
                bitmap = currentBmp,
                contentDescription = description,
                modifier = modifier,
                contentScale = ContentScale.Crop,
                alignment = alignment
            )
        } else {
            Box(modifier)
        }
        return
    }
    LaunchedEffect(resolved) {
        if (resolved.startsWith("http")) {
            com.example.data.LibraryDiskCacheRegistry.registerUrls(listOf(resolved))
        }
    }
    val request = remember(resolved, face) {
        val diskPolicy = if (resolved.startsWith("http")) com.example.data.LibraryDiskCacheRegistry.diskCachePolicyFor(resolved) else coil.request.CachePolicy.ENABLED
        ImageRequest.Builder(context)
            .data(resolved)
            .allowHardware(true)
            .allowRgb565(true)
            .size(if (face) 140 else 720, if (face) 140 else 240)
            .diskCachePolicy(diskPolicy)
            .build()
    }
    AsyncImage(request, description, modifier, contentScale = ContentScale.Crop, alignment = alignment)
}

/** Bounds decoding, EXIF rotation and 3:1 center crop happen off the UI thread. */
internal suspend fun prepareProfileBanner(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { com.example.data.SafeImageFormats.decodeStreamSafely(it, bounds) }
    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Imagem inválida" }
    val options = BitmapFactory.Options().apply { var sample = 1; while (bounds.outWidth / sample > 2400 || bounds.outHeight / sample > 2400) sample *= 2; inSampleSize = sample }
    val decoded = context.contentResolver.openInputStream(uri)?.use { com.example.data.SafeImageFormats.decodeStreamSafely(it, options) } ?: error("Não foi possível abrir a imagem")
    val rotation = runCatching { context.contentResolver.openInputStream(uri).use { input ->
        when (android.media.ExifInterface(input!!).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1)) { 3 -> 180f; 6 -> 90f; 8 -> 270f; else -> 0f }
    } }.getOrDefault(0f)
    val upright = if (rotation == 0f) decoded else Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(rotation) }, true).also { decoded.recycle() }
    val cropWidth = minOf(upright.width, upright.height * 3)
    val cropHeight = cropWidth / 3
    val cropped = Bitmap.createBitmap(upright, (upright.width - cropWidth) / 2, (upright.height - cropHeight) / 2, cropWidth, cropHeight)
    val scaled = Bitmap.createScaledBitmap(cropped, 1200, 400, true)
    val out = ByteArrayOutputStream(); var quality = 86
    do { out.reset(); scaled.compress(Bitmap.CompressFormat.JPEG, quality, out); quality -= 10 } while (out.size() > 140000 && quality >= 36)
    val result = "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    if (scaled !== cropped) scaled.recycle(); if (cropped !== upright) cropped.recycle(); upright.recycle()
    require(result.length <= 200000) { "Imagem muito detalhada. Escolha outra imagem." }; result
}
