package com.example

import com.example.data.CatalogCredit
import com.example.data.CatalogDetails
import com.example.data.ComicDetailPresentation
import com.example.data.PersonalizedDiscovery
import com.example.data.RecommendedComic
import com.example.model.ComicEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Patch6ComicDetailAndExploreIntegrationTest {

    private fun sample(
        id: String,
        title: String,
        titlePtBr: String = "",
        authors: String = "Alan Moore",
        publisher: String = "DC / Vertigo",
        seriesName: String = "",
        issueNumber: Int = 0,
        externalId: String = ""
    ) = ComicEntry(
        id = id,
        externalId = externalId,
        title = title,
        titlePtBr = titlePtBr,
        authors = authors,
        publisher = publisher,
        seriesName = seriesName,
        issueNumber = issueNumber,
        coverUrl = "https://img/$id.jpg"
    )

    @Test
    fun comicDetailRelatedEditionsCombinesMultiSignalsWithRealLinksAndBoundsTo8() {
        val current = sample(
            id = "swamp-1",
            externalId = "cv:4000-1",
            title = "Saga of the Swamp Thing #1",
            titlePtBr = "A Saga do Monstro do Pântano #1",
            authors = "Alan Moore, Stephen Bissette",
            seriesName = "Saga of the Swamp Thing",
            issueNumber = 1
        )
        val detail = CatalogDetails(
            id = "4000-1",
            type = "issue",
            title = current.titlePtBr,
            image = "",
            banner = "",
            description = "",
            publisher = "DC / Vertigo",
            date = "1984",
            url = "",
            covers = emptyList(),
            characters = listOf(
                CatalogCredit(id = "4005-1", name = "John Constantine", role = "Personagem")
            ),
            creators = listOf(
                CatalogCredit(id = "4040-1", name = "Alan Moore", role = "Roteiro")
            ),
            issues = emptyList()
        )

        val catalogPool = listOf(
            current,
            sample("swamp-2", "Saga of the Swamp Thing #2", "A Saga do Monstro do Pântano #2", "Alan Moore", seriesName = "Saga of the Swamp Thing", issueNumber = 2, externalId = "cv:4000-2"),
            sample("swamp-annual", "Saga of the Swamp Thing Annual", "A Saga do Monstro do Pântano: Anual", "Alan Moore", seriesName = "Saga of the Swamp Thing", issueNumber = 0, externalId = "cv:4000-99"),
            sample("watchmen", "Watchmen", "Watchmen", "Alan Moore, Dave Gibbons", seriesName = "Watchmen", externalId = "cv:4000-10"),
            sample("hellblazer-1", "Hellblazer: Original Sins", "John Constantine, Hellblazer: Pecados Originais", "Jamie Delano", seriesName = "Hellblazer", externalId = "cv:4000-20"),
            sample("rotworld", "Swamp Thing: Rotworld", "Monstro do Pântano: Mundo Podre", "Scott Snyder", seriesName = "Swamp Thing", externalId = "cv:4000-30"),
            sample("moore-v", "V for Vendetta", "V de Vingança", "Alan Moore", externalId = "cv:4000-40"),
            sample("moore-miracle", "Miracleman", "Miracleman", "Alan Moore", externalId = "cv:4000-41"),
            sample("moore-from-hell", "From Hell", "Do Inferno", "Alan Moore", externalId = "cv:4000-42"),
            sample("moore-league", "The League of Extraordinary Gentlemen", "A Liga Extraordinária", "Alan Moore", externalId = "cv:4000-43"),
            sample("moore-promethea", "Promethea", "Promethea", "Alan Moore", externalId = "cv:4000-44")
        )

        val related = ComicDetailPresentation.relatedEditionsWithReasons(current, detail, catalogPool)

        assertEquals(8, related.size)
        assertFalse(related.any { it.comic.id == current.id })
        assertEquals("swamp-2", related.first().comic.id)
        assertEquals("Próxima edição • #2", related.first().reasonLabel)

        val reasonsById = related.associate { it.comic.id to it.reasonLabel }
        assertEquals("Mesma série", reasonsById["swamp-annual"])
        assertEquals("De Alan Moore", reasonsById["watchmen"])
        assertEquals("Com John Constantine", reasonsById["hellblazer-1"])
    }

    @Test
    fun encodeAndDecodeCachedRecommendationsPreservesReasonsAndRespectsSizeLimit() {
        val list = (1..20).map { idx ->
            RecommendedComic(
                comic = sample("rec-$idx", "Batman Vol. $idx", "Batman Vol. $idx", "Scott Snyder", issueNumber = idx, externalId = "cv:4000-$idx"),
                reasonLabel = "Continue Batman • #$idx",
                score = 200 - idx,
                signalSource = "SERIES_CONTINUATION"
            )
        }

        val encoded = PersonalizedDiscovery.encodeCachedRecommendations(list)
        assertTrue(encoded.isNotBlank())
        assertTrue("Cache JSON must be <= 12 KB", encoded.toByteArray(Charsets.UTF_8).size <= 12_288)

        val decoded = PersonalizedDiscovery.decodeCachedRecommendations(encoded)
        assertEquals(16, decoded.size)
        assertEquals("rec-1", decoded.first().comic.id)
        assertEquals("Continue Batman • #1", decoded.first().reasonLabel)
        assertEquals(199, decoded.first().score)
    }
}
