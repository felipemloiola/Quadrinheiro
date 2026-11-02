package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.test.core.app.ApplicationProvider
import coil.Coil
import coil.ImageLoader
import coil.decode.DataSource
import coil.intercept.Interceptor
import coil.request.ErrorResult
import coil.request.SuccessResult
import com.example.model.ComicEntry
import com.example.widget.WidgetVisualRenderer
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WidgetCoverTest {
    @Test(timeout = 15000) fun temporaryFailureIsRetriedAndSuccessfulCoverIsReused() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val previous = Coil.imageLoader(context)
        var attempts = 0
        val bitmap = Bitmap.createBitmap(60, 90, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.RED) }
        val loader = ImageLoader.Builder(context).components {
            add(Interceptor { chain ->
                attempts++
                if (attempts == 1) ErrorResult(null, chain.request, java.io.IOException("offline"))
                else SuccessResult(BitmapDrawable(context.resources, bitmap), chain.request, DataSource.NETWORK)
            })
        }.build()
        try {
            Coil.setImageLoader(loader)
            val comic = ComicEntry("retry-cover-fixture", "", "Capa de teste", authors = "", publisher = "", coverUrl = "https://example.invalid/retry.jpg")
            val palette = WidgetVisualRenderer.resolvePaletteForThemeId("black")
            val fallback = WidgetVisualRenderer.loadOrRenderCoverBitmap(context, comic, palette, 60, 90)
            val loaded = WidgetVisualRenderer.loadOrRenderCoverBitmap(context, comic, palette, 60, 90)
            assertNotSame(fallback, loaded)
            assertEquals(60, loaded.width)
            assertEquals(90, loaded.height)
            assertSame(loaded, WidgetVisualRenderer.loadOrRenderCoverBitmap(context, comic, palette, 60, 90))
            assertEquals(2, attempts)
        } finally {
            Coil.setImageLoader(previous)
            loader.shutdown()
            Dispatchers.resetMain()
        }
    }
}
