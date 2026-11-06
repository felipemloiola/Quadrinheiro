package com.example

import com.example.data.PersonalizedDiscovery
import com.example.data.RecommendedComic
import com.example.model.AppSettings
import com.example.model.ComicEntry
import com.example.model.ReadingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Patch3RecommendationEngineAndRetrievalTest {

    private fun sample(
        id: String,
        title: String,
        series: String = "",
        authors: String = "",
        publisher: String = "DC / Vertigo",
        issueNumber: Int = 0,
        status: ReadingStatus = ReadingStatus.READ,
        rating: Float = 0f,
        favorite: Boolean = false
    ) = ComicEntry(
        id = id,
        externalId = id,
        title = title,
        seriesName = series,
        authors = authors,
        publisher = publisher,
        issueNumber = issueNumber,
        status = status,
        rating = rating,
        favorite = favorite,
        coverUrl = "https://example.com/cover.jpg",
        releaseYear = 2022
    )

    @Test
    fun readingContinuationGivesHighestScoreAndTrueReason() {
        val library = listOf(
            sample("lib1", "Saga of the Swamp Thing #1", series = "Saga of the Swamp Thing", authors = "Alan Moore", issueNumber = 1, status = ReadingStatus.READING)
        )
        val catalog = listOf(
            sample("cat2", "Saga of the Swamp Thing #2", series = "Saga of the Swamp Thing", authors = "Alan Moore", issueNumber = 2),
            sample("catX", "Homem-Aranha Especial", series = "Homem-Aranha", authors = "Dan Slott", issueNumber = 1)
        )

        val recs = PersonalizedDiscovery.recommendWithReasons(catalog, library)

        assertTrue(recs.isNotEmpty())
        assertEquals("cat2", recs.first().comic.id)
        assertTrue(recs.first().reasonLabel.contains("Continuação da sua leitura"))
    }

    @Test
    fun ownedLibraryComicsAreNeverRecommendedAsNovelties() {
        val library = listOf(
            sample("owned1", "Watchmen", authors = "Alan Moore", status = ReadingStatus.READ)
        )
        val catalog = listOf(
            sample("owned1", "Watchmen", authors = "Alan Moore"),
            sample("cat_new", "V de Vingança", authors = "Alan Moore")
        )

        val recs = PersonalizedDiscovery.recommendWithReasons(catalog, library)

        assertFalse(recs.any { it.comic.id == "owned1" })
        assertTrue(recs.any { it.comic.id == "cat_new" })
    }

    @Test
    fun diversityMmrCapsSeriesToMaxTwoEntriesAtTop() {
        val library = listOf(
            sample("read1", "Monstro do Pântano #1", series = "Monstro do Pântano", authors = "Alan Moore", status = ReadingStatus.READING)
        )
        val catalog = listOf(
            sample("c2", "Monstro do Pântano #2", series = "Monstro do Pântano", authors = "Alan Moore", issueNumber = 2),
            sample("c3", "Monstro do Pântano #3", series = "Monstro do Pântano", authors = "Alan Moore", issueNumber = 3),
            sample("c4", "Monstro do Pântano #4", series = "Monstro do Pântano", authors = "Alan Moore", issueNumber = 4),
            sample("c5", "Monstro do Pântano #5", series = "Monstro do Pântano", authors = "Alan Moore", issueNumber = 5),
            sample("c_sand", "Sandman #1", series = "Sandman", authors = "Neil Gaiman", issueNumber = 1)
        )
        val settings = AppSettings(profileFavoriteCharacters = listOf("Sandman"))

        val recs = PersonalizedDiscovery.recommendWithReasons(catalog, library, settings = settings)

        // In the top 3, Monstro do Pântano should not exceed 2 entries
        val top3Series = recs.take(3).map { it.comic.seriesName }
        assertTrue(top3Series.count { it == "Monstro do Pântano" } <= 2)
    }
}
