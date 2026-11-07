package com.example

import com.example.model.ComicEntry
import com.example.model.ReadingStatus
import com.example.ui.screens.ExploreShowcaseBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExploreScreenStateStabilityTest {

    @Before
    fun setUp() {
        ExploreShowcaseBuilder.resetMemoForTests()
    }

    private fun sampleComic(
        id: String,
        title: String,
        publisher: String = "DC Comics"
    ): ComicEntry = ComicEntry(
        id = id,
        externalId = id,
        title = title,
        authors = "Frank Miller",
        publisher = publisher,
        genre = "Super-heróis",
        format = "Encadernado",
        releaseYear = 1986,
        pageCount = 200,
        currentPage = 200,
        status = ReadingStatus.READ,
        rating = 5f,
        favorite = true
    )

    @Test
    fun testExploreShowcaseBuilderFilteringAndMemoization() {
        val catalog = listOf(
            sampleComic("1", "Batman: Ano Um", publisher = "DC Comics"),
            sampleComic("2", "Homem-Aranha Especial", publisher = "Marvel"),
            sampleComic("3", "Watchmen", publisher = "DC / Vertigo"),
            sampleComic("4", "Demolidor: A Queda de Murdock", publisher = "Marvel")
        )

        val starter = ExploreShowcaseBuilder.filterStarter(catalog)
        val dc = ExploreShowcaseBuilder.filterDc(catalog)
        val marvel = ExploreShowcaseBuilder.filterMarvel(catalog)

        assertEquals(2, starter.size) // Ano Um and Watchmen
        assertEquals(2, dc.size) // Batman and Watchmen
        assertEquals(2, marvel.size) // Homem-Aranha and Demolidor

        val stamp = ExploreShowcaseBuilder.catalogStamp(catalog)
        ExploreShowcaseBuilder.updateMemoizedSlices(stamp, starter, dc, marvel)

        assertEquals(2, ExploreShowcaseBuilder.memoStarter.size)
        assertEquals(2, ExploreShowcaseBuilder.memoDc.size)
        assertEquals(2, ExploreShowcaseBuilder.memoMarvel.size)

        assertTrue(ExploreShowcaseBuilder.hasSameComicIds(starter, ExploreShowcaseBuilder.memoStarter))
    }

    @Test
    fun testHasSameComicIdsDetection() {
        val listA = listOf(sampleComic("1", "Batman"))
        val listB = listOf(sampleComic("1", "Batman"))
        val listC = listOf(sampleComic("2", "Batman"))

        assertTrue(ExploreShowcaseBuilder.hasSameComicIds(listA, listB))
        assertFalse(ExploreShowcaseBuilder.hasSameComicIds(listA, listC))
    }
}
