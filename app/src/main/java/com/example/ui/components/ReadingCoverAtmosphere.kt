package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import coil.compose.AsyncImage
import coil.request.ImageRequest

/** Decorative cover light ends at the display midpoint, with no full-screen blur pass. */
@Composable
internal fun ReadingCoverAtmosphere(url: String, background: Color, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val request = remember(url, context) { ImageRequest.Builder(context).data(url).size(28, 42)
        .crossfade(false).allowHardware(true).allowRgb565(true)
        .memoryCacheKey(url + "_ambient").diskCacheKey(url + "_ambient").build() }
    Box(modifier.fillMaxWidth().fillMaxHeight(.5f).clipToBounds().testTag("reading_cover_atmosphere")) {
        AsyncImage(request, null, Modifier.fillMaxSize().alpha(.24f), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0f to background.copy(alpha = .15f), .55f to background.copy(alpha = .35f), 1f to background)))
    }
}
