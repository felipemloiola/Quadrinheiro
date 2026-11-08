package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@Composable
internal fun ProfileImage(source: String, description: String?, modifier: Modifier = Modifier, face: Boolean = false) {
    val context = LocalContext.current
    val resolved = com.example.data.ProfileShowcase.characterImage(source)
    val data = remember(resolved) { if (resolved.startsWith("data:image")) runCatching { Base64.decode(resolved.substringAfter("base64,"), Base64.DEFAULT) }.getOrNull() else resolved }
    LaunchedEffect(resolved) {
        if (resolved.startsWith("http")) {
            com.example.data.LibraryDiskCacheRegistry.registerUrls(listOf(resolved))
        }
    }
    val request = remember(data, face) {
        val diskPolicy = if (resolved.startsWith("http")) com.example.data.LibraryDiskCacheRegistry.diskCachePolicyFor(resolved) else coil.request.CachePolicy.ENABLED
        ImageRequest.Builder(context)
            .data(data)
            .allowHardware(true)
            .allowRgb565(true)
            .size(if (face) 140 else 720, if (face) 140 else 240)
            .diskCachePolicy(diskPolicy)
            .build()
    }
    AsyncImage(request, description, modifier, contentScale = ContentScale.Crop, alignment = if (face) Alignment.TopCenter else Alignment.Center)
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
