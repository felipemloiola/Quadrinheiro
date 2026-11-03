package com.example

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.example.data.CacheMaintenance
import com.example.data.SafeStorage
import com.example.data.SeriesSequenceEngine
import com.example.model.ComicEntry
import com.example.widget.WidgetVisualRenderer
import com.example.widget.isSystemNight
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PresentationMaintenanceTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun collectedYearOneIsNotMonthlyButMonthlyAndVolumesKeepTheirNumbers() {
        val book = ComicEntry("year-one", "", "Batman: Ano Um", authors = "Frank Miller", publisher = "DC", pageCount = 144, issueNumber = 1)
        assertEquals(0, SeriesSequenceEngine.inferIssueNumber(book))
        assertEquals(404, SeriesSequenceEngine.inferIssueNumber(book.copy(title = "Batman #404", format = "Mensal", pageCount = 32, issueNumber = 404)))
        assertEquals(2, SeriesSequenceEngine.inferIssueNumber(book.copy(title = "Akira Vol. 2", issueNumber = 2)))
    }

    @Test fun widgetPaletteAndResourcesFollowPhoneAppearance() {
        val day = WidgetVisualRenderer.resolvePaletteForThemeId("SYSTEM_MONO", false)
        val night = WidgetVisualRenderer.resolvePaletteForThemeId("SYSTEM_MONO", true)
        assertTrue(day.bgCard.red > .9f)
        assertTrue(night.bgCard.red < .15f)
        assertTrue(day.textPrimary.red < .2f)
        assertTrue(night.textPrimary.red > .9f)
        for (mode in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
            val config = Configuration(context.resources.configuration).apply { uiMode = mode }
            val themed = context.createConfigurationContext(config)
            assertEquals(mode == Configuration.UI_MODE_NIGHT_YES, isSystemNight(themed))
            val color = themed.getColor(R.color.widget_background)
            assertTrue(if (isSystemNight(themed)) android.graphics.Color.red(color) < 40 else android.graphics.Color.red(color) > 230)
        }
    }

    @Test fun cleanupOnlyRemovesOwnedExpiredOrInstalledFiles() {
        val now = System.currentTimeMillis()
        val updates = File(context.cacheDir, "updates").apply { mkdirs() }
        val old = File(updates, "quadrinheiro-${BuildConfig.VERSION_CODE}.apk").apply { writeText("old") }
        val future = File(updates, "quadrinheiro-${BuildConfig.VERSION_CODE + 5000}.apk").apply { writeText("new") }
        val unrelated = File(updates, "backup.zip").apply { writeText("keep") }
        val cards = File(context.cacheDir, "shared_cards").apply { mkdirs() }
        val expired = File(cards, "expired.png").apply { writeText("old"); setLastModified(now - 4L * 86400000) }
        val recent = File(cards, "recent.png").apply { writeText("new") }
        val storage = SafeStorage(context)
        storage.setItem("maintenance-library-fixture", "preserved")
        CacheMaintenance.prune(context, now)
        assertFalse(old.exists())
        assertFalse(expired.exists())
        assertTrue(future.exists())
        assertTrue(unrelated.exists())
        assertTrue(recent.exists())
        assertEquals("preserved", storage.getItem("maintenance-library-fixture"))
    }

    @Test fun translationCacheIsBoundedAndRepeatedTrimsPreserveNewestEntries() {
        val storage = SafeStorage(context)
        val translations = JSONObject().apply {
            repeat(1100) { put("book-$it", JSONObject().put("titlePtBr", "Título $it")) }
        }
        storage.setItem(SafeStorage.TRANSLATION_CACHE_KEY, translations.toString())
        repeat(2) { storage.trimTranslationCache() }
        val bounded = JSONObject(storage.getItem(SafeStorage.TRANSLATION_CACHE_KEY)!!)
        assertEquals(1024, bounded.length())
        assertTrue(bounded.has("book-1099"))
        assertFalse(bounded.has("book-0"))
    }
}
