package com.example

import com.example.data.*
import com.example.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InteractionRevisionTest {
    @Test fun legacyYearOneMetadataIsRepairedBeforeIssueInference() {
        val legacy = ComicEntry("legacy-year-one", "", "Batman: Ano Um #1", titlePtBr = "Batman: Ano Um #1",
            authors = "Frank Miller", publisher = "DC", pageCount = 36, format = "Revista Mensal (Issue)", issueNumber = 1)
        val repaired = PortugueseComicTranslator.localizeComicMetadataOnly(legacy)
        assertEquals("Batman: Ano Um", repaired.displayTitle)
        assertEquals(0, repaired.issueNumber)
        assertEquals(144, repaired.pageCount)
        assertEquals("Encadernado (TPB)", repaired.format)
        val monthly = PortugueseComicTranslator.localizeComicMetadataOnly(legacy.copy(id = "monthly-404", title = "Batman #404", titlePtBr = "", issueNumber = 404, pageCount = 32))
        assertEquals(404, monthly.issueNumber)
        assertEquals(32, monthly.pageCount)
    }

    @Test fun frostedChangesOpticalParametersWithoutMutatingStoredControls() {
        val standard = AppSettings()
        val frosted = standard.copy(glassVariant = "frosted")
        val visual = frosted.withGlassAppearance()
        assertTrue(visual.glassBlurIntensity > standard.glassBlurIntensity)
        assertTrue(visual.glassOpacity > standard.glassOpacity)
        assertTrue(visual.glassIntensity < standard.glassIntensity)
        assertEquals(1f, frosted.glassBlurIntensity)
        assertEquals(standard, standard.withGlassAppearance())
    }

    @Test fun colorOrderBeginsWithMonochromesThenDescendsTheHueWheel() {
        assertEquals(listOf(AppTheme.BLACK, AppTheme.WHITE, AppTheme.PURPLE, AppTheme.BLUE, AppTheme.GREEN, AppTheme.ORANGE, AppTheme.RED), accentThemesByDescendingHue())
    }

    @Test fun discoveriesUseReadingHistoryAndExcludeEveryOwnedStatus() {
        val catalog = com.example.data.LegacyCatalogFixture.curatedComicsCatalog
        val watchmen = catalog.first { it.title.contains("Watchmen") }.copy(status = ReadingStatus.READ, rating = 5f)
        val sandman = catalog.first { it.title.contains("Sandman") }.copy(status = ReadingStatus.WANT_TO_READ)
        val result = PersonalizedDiscovery.recommend(catalog, listOf(watchmen, sandman))
        assertFalse(result.any { it.id == watchmen.id || it.id == sandman.id })
        assertTrue("A well-rated Watchmen reading should recommend V de Vingança", result.any { it.title.contains("Vingança") })
        assertFalse("Unrelated zero-affinity Akira should be filtered, not required at a lower rank", result.any { it.title.contains("Akira") })
        assertNotEquals(PersonalizedDiscovery.recommend(catalog, emptyList()).take(5).map { it.id }, result.take(5).map { it.id })
    }

    @Test fun sequenceCacheDoesNotReuseAnotherSameSizeLibrary() = runBlocking {
        val catalog = com.example.data.LegacyCatalogFixture.curatedComicsCatalog
        val watchmen = catalog.first { it.title.contains("Watchmen") }
        val vengeance = catalog.first { it.title.contains("Vingança") }
        val sandman = catalog.first { it.title.contains("Sandman") }
        val first = SeriesSequenceEngine.findSequenceForComicAsync(watchmen, listOf(vengeance.copy(status = ReadingStatus.READ)),catalog)
        val second = SeriesSequenceEngine.findSequenceForComicAsync(watchmen, listOf(sandman.copy(status = ReadingStatus.READING)),catalog)
        assertNotSame(first, second)
        val key = PortugueseComicTranslator.canonicalWorkKey(vengeance.title)
        assertNotNull(first.recommendations.first { PortugueseComicTranslator.canonicalWorkKey(it.comic.title) == key }.savedEntry)
        assertNull(second.recommendations.first { PortugueseComicTranslator.canonicalWorkKey(it.comic.title) == key }.savedEntry)
    }
}
