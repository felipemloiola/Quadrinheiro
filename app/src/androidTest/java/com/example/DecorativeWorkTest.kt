package com.example

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import coil.Coil
import coil.ImageLoader
import coil.intercept.Interceptor
import com.example.model.AppSettings
import com.example.ui.components.*
import com.example.ui.theme.QuadrinheiroTheme
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DecorativeWorkTest {
    @get:Rule val compose = createComposeRule()
    @Test fun stalledCoversStopRequestingAnimationFramesAfterTheInitialFeedback() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val old = Coil.imageLoader(context)
        val loader = ImageLoader.Builder(context).components { add(Interceptor { awaitCancellation() }) }.build()
        Coil.setImageLoader(loader)
        try {
            compose.mainClock.autoAdvance = false
            compose.setContent { QuadrinheiroTheme(AppSettings(colorScheme = "dark")) {
                Box(Modifier.size(100.dp, 160.dp)) { BookCover("https://example.invalid/pending", "Teste", spine3d = false, modifier = Modifier.fillMaxSize()) }
            } }
            compose.mainClock.advanceTimeBy(100)
            val first = compose.onRoot().captureToImage().asAndroidBitmap()
            compose.mainClock.advanceTimeBy(600)
            assertFalse(first.sameAs(compose.onRoot().captureToImage().asAndroidBitmap()))
            compose.mainClock.advanceTimeBy(7000)
            val settled = compose.onRoot().captureToImage().asAndroidBitmap()
            compose.mainClock.advanceTimeBy(1500)
            assertTrue(settled.sameAs(compose.onRoot().captureToImage().asAndroidBitmap()))
        } finally { Coil.setImageLoader(old); loader.shutdown() }
    }
    @Test fun coverAtmosphereFadesAcrossTheUpperThird() {
        compose.setContent { QuadrinheiroTheme(AppSettings(reduceMotion = true)) {
            Box(Modifier.size(300.dp, 400.dp).background(Color.Black).testTag("host")) {
                ReadingCoverAtmosphere("", Color.Black)
            }
        } }
        val host = compose.onNodeWithTag("host").getUnclippedBoundsInRoot()
        val light = compose.onNodeWithTag("reading_cover_atmosphere", true).getUnclippedBoundsInRoot()
        assertTrue(kotlin.math.abs((light.bottom - light.top).value - (host.bottom - host.top).value * .34f) < 1f)
    }
}
