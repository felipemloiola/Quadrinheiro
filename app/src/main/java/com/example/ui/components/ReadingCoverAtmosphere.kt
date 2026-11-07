package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import coil.compose.AsyncImage
import coil.request.ImageRequest

/** Decorative cover light ends at the display midpoint, with no full-screen blur pass. */
@Composable
internal fun ReadingCoverAtmosphere(url: String, background: Color, modifier: Modifier = Modifier, fraction: Float = .34f) {
    val context = LocalContext.current
    val cleanUrl = remember(url) { url.trim().replace("http://", "https://") }
    val request = remember(cleanUrl, context) {
        ImageRequest.Builder(context)
            .data(cleanUrl)
            .size(240, 360)
            .precision(coil.size.Precision.INEXACT)
            .crossfade(false)
            .allowHardware(true)
            .allowRgb565(true)
            .memoryCacheKey(cleanUrl)
            .diskCacheKey(cleanUrl)
            .placeholderMemoryCacheKey(cleanUrl)
            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
            .build()
    }
    val fadeBrush = remember(background) {
        Brush.verticalGradient(
            0f to background.copy(alpha = .12f),
            .25f to background.copy(alpha = .25f),
            .55f to background.copy(alpha = .60f),
            .8f to background.copy(alpha = .92f),
            1f to background
        )
    }
    Box(modifier.fillMaxWidth().fillMaxHeight(fraction).clipToBounds().testTag("reading_cover_atmosphere")) {
        AsyncImage(request, null, Modifier.fillMaxSize().blur(18.dp).alpha(.25f), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(fadeBrush))
    }
}
