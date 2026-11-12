package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.model.AppSettings
import com.example.model.ComicEntry
import com.example.model.ReadingStatus
import com.example.reading.ReadingNowBarPreference
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.theme.QuadrinheiroTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import android.app.Notification
import androidx.core.app.NotificationCompat
import androidx.test.filters.SdkSuppress
import com.example.reading.buildPromotableReadingNotification
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class LiveUpdatesPresentationTest {
    @get:Rule val compose = createComposeRule()

    @Test @SdkSuppress(minSdkVersion = 36)
    fun nativeFrameworkAcceptsProgressPromotionWithoutCustomLayouts() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val notification = buildPromotableReadingNotification(NotificationCompat.Builder(context, "reading-preview-test")
            .setSmallIcon(R.drawable.ic_reading_notification).setContentTitle("Leitura de exemplo")
            .setContentText("Pág. 42 de 120 · 35%").setOngoing(true).setRequestPromotedOngoing(true)
            .setStyle(NotificationCompat.ProgressStyle().setProgress(42)
                .setProgressSegments(listOf(NotificationCompat.ProgressStyle.Segment(120)))))
        assertTrue(notification.hasPromotableCharacteristics())
        assertNull(notification.contentView)
        if (android.os.Build.VERSION.SDK_INT >= 37) assertFalse(notification.extras.getBoolean(Notification.EXTRA_COLORIZED))
    }

    @Test fun progressIsReadableWithReducedTransparencyAndLargeText() {
        compose.setContent {
            QuadrinheiroTheme(settings = AppSettings(colorScheme = "dark", reduceMotion = true,
                reduceTransparency = true, dynamicTypeScale = 115)) {
                Column(Modifier.background(LocalIOS27Theme.current.bgPrimary).padding(12.dp)) {
                    ReadingNowBarPreference(enabled = true, comic = ComicEntry("fixture", "", "Uma jornada em quadrinhos",
                        authors = "", publisher = "", pageCount = 120, currentPage = 42, status = ReadingStatus.READING), onChange = {})
                }
            }
        }
        compose.onNodeWithText("Live Updates de leitura").assertIsDisplayed()
        compose.onNodeWithText("Pág. 42 de 120 · 35%").assertIsDisplayed()
        compose.onNodeWithText("Permissões dos Live Updates e da tela bloqueada ›").assertHeightIsAtLeast(48.dp)
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val output = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "live-updates-preview.png")
        output.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
}
