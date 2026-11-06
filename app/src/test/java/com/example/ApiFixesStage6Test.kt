package com.example

import com.example.data.ComicDetailPresentation
import com.example.data.PersonalizedDiscovery
import com.example.model.AppSettings
import com.example.model.ComicEntry
import com.example.model.ReadingStatus
import org.junit.Assert.*
import org.junit.Test

class ApiFixesStage6Test {

    private fun testEntry(
        id: String,
        title: String,
        seriesName: String = "",
        authors: String = "",
        publisher: String = "DC Comics",
        status: ReadingStatus = ReadingStatus.READ
    ): ComicEntry {
        return ComicEntry(
            id = id,
            externalId = id,
            title = title,
            seriesName = seriesName,
            authors = authors,
            publisher = publisher,
            status = status,
            coverUrl = "https://example.com/cover.jpg"
        )
    }

    @Test
    fun relatedEditionsFallsBackToSameSeriesWhenDetailIssuesIsEmpty() {
        val current = testEntry(id = "1", title = "Sandman: Prelúdios e Noturnos", seriesName = "Sandman", authors = "Neil Gaiman")
        val pool = listOf(
            testEntry(id = "2", title = "Sandman: A Casa de Bonecas", seriesName = "Sandman", authors = "Neil Gaiman"),
            testEntry(id = "3", title = "Batman: Ano Um", seriesName = "Batman", authors = "Frank Miller")
        )

        val related = ComicDetailPresentation.relatedEditions(current, detail = null, catalogPool = pool)
        assertEquals(1, related.size)
        assertEquals("2", related[0].id)
    }

    @Test
    fun personalizedDiscoveryConsidersFavoriteCharactersAndReadingStatus() {
        val candidate = testEntry(id = "10", title = "Monstro do Pântano", authors = "Alan Moore", publisher = "DC / Vertigo")
        val other = testEntry(id = "20", title = "Manga Desconhecido", authors = "Autor X", publisher = "Outra")
        val settings = AppSettings(
            profileFavoriteCharacters = listOf("Monstro do Pântano")
        )

        val recs = PersonalizedDiscovery.recommend(
            catalog = listOf(candidate, other),
            library = emptyList(),
            settings = settings
        )

        assertTrue(recs.isNotEmpty())
        assertEquals("10", recs[0].id)
    }
}
