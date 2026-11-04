package com.example

import com.example.data.AdditionalCatalogSources
import com.example.data.CatalogSearchPolicy
import com.example.model.ComicEntry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AdditionalCatalogSourcesTest {
    private fun series(id: String, author: String = "", year: Int = 1997) = ComicEntry(id, id, "One Piece", authors = author, publisher = "", releaseYear = year, genre = "Mangá", format = "Série de mangá", pageCount = 0)
    @Test fun sourceFiltersRespectMangaBooksAndPortuguesePreferences() {
        assertEquals(listOf("kitsu", "mangadex"), AdditionalCatalogSources.enabledSources("MANGA", "ALL"))
        assertFalse(AdditionalCatalogSources.enabledSources("BOOKS", "ALL").contains("kitsu"))
        assertFalse(AdditionalCatalogSources.enabledSources("MULTI", "PT").contains("apple-us"))
        assertTrue(AdditionalCatalogSources.enabledSources("MULTI", "ALL").containsAll(listOf("catalog", "apple-br", "apple-us", "google-pt", "kitsu", "mangadex", "open-library")))
    }
    @Test fun kitsuChapterAndVolumeCountsNeverBecomePageCounts() {
        val root = JSONObject("""{"data":[{"id":"123","attributes":{"canonicalTitle":"One Piece","chapterCount":1000,"volumeCount":100,"startDate":"1997-01-01","synopsis":"A story","posterImage":{"medium":"https://example.com/cover.jpg"}}}]}""")
        val entry = AdditionalCatalogSources.parseKitsu(root).single()
        assertEquals(0, entry.pageCount); assertEquals(0, entry.currentPage); assertEquals("", entry.publisher); assertEquals("Série de mangá", entry.format)
    }
    @Test fun mangadexExpandedRelationshipsSupplyCreatorsAndThumbnailWithoutInventingAnEdition() {
        val root = JSONObject("""{"data":[{"id":"8f3e1818-a015-491d-bd81-3addc4d7d56a","attributes":{"title":{"en":"Manga"},"contentRating":"safe","year":1997},"relationships":[{"type":"author","attributes":{"name":"Autora"}},{"type":"cover_art","attributes":{"fileName":"cover.png","volume":"12"}}]}]}""")
        val entry = AdditionalCatalogSources.parseMangaDex(root).single()
        assertEquals("Autora", entry.authors); assertTrue(entry.coverUrl.endsWith("cover.png.256.jpg"))
        assertEquals(0, entry.pageCount); assertEquals(0, entry.issueNumber); assertEquals("Série de mangá", entry.format)
    }
    @Test fun mangadexRejectsUnsafeRatingsAndMalformedIdentities() {
        val root = JSONObject("""{"data":[{"id":"invalid","attributes":{"title":{"en":"X"},"contentRating":"safe"}},{"id":"8f3e1818-a015-491d-bd81-3addc4d7d56a","attributes":{"title":{"en":"X"},"contentRating":"pornographic"}}]}""")
        assertTrue(AdditionalCatalogSources.parseMangaDex(root).isEmpty())
    }
    @Test fun appleUsUsesUsIdentityAndDoesNotRepeatTheSameGlobalBookInBrazil() {
        val root = JSONObject("""{"results":[{"trackId":7,"trackName":"HQ","genres":["Comics & Graphic Novels"]}]}""")
        val us = AdditionalCatalogSources.parseAppleUs(root).single()
        assertEquals("ab-US-7", us.externalId)
        val br = us.copy(id = "ab-BR-7", externalId = "ab-BR-7")
        assertEquals(listOf(br), CatalogSearchPolicy.unique(listOf(us, br)))
    }
    @Test fun compatibleSeriesDeduplicateButConflictingCreatorsRemainSeparate() {
        assertEquals(1, CatalogSearchPolicy.unique(listOf(series("kt-1"), series("md-1", "Eiichiro Oda"))).size)
        assertEquals(2, CatalogSearchPolicy.unique(listOf(series("a", "Pessoa A"), series("b", "Pessoa B"))).size)
        assertEquals(2, CatalogSearchPolicy.unique(listOf(series("a", year = 0), series("b", year = 0))).size)
    }
    private fun work() = JSONObject("""{"key":"/works/OL1W","title":"Work title","author_name":["Autora"],"first_publish_year":1900,"publisher":["Wrong publisher"],"subject":["Comics"],"editions":{"docs":[{"key":"/books/OL2M"}]}}""")
    private fun edition() = JSONObject("""{"key":"/books/OL2M","title":"Edição brasileira","works":[{"key":"/works/OL1W"}],"publishers":["Panini"],"publish_date":"2011","number_of_pages":144,"isbn_13":["9781401207526"],"languages":[{"key":"/languages/por"}],"covers":[999]}""")
    @Test fun openLibraryUsesOnlyTheSelectedEditionForPagesPublisherYearAndIsbn() {
        val parsed = AdditionalCatalogSources.parseOpenLibrary(work(), edition(), "OL2M", true)!!
        assertEquals(144, parsed.pageCount); assertEquals("Panini", parsed.publisher); assertEquals(2011, parsed.releaseYear)
        assertEquals("9781401207526", parsed.isbn); assertTrue(parsed.coverUrl.contains("999-L.jpg"))
    }
    @Test fun openLibraryRejectsWrongEditionsWorksAndUnconfirmedPortuguese() {
        assertNull(AdditionalCatalogSources.parseOpenLibrary(work(), edition().put("key", "/books/OL3M"), "OL2M", false))
        assertNull(AdditionalCatalogSources.parseOpenLibrary(work(), edition().put("works", org.json.JSONArray("""[{"key":"/works/OL9W"}]""")), "OL2M", false))
        assertNull(AdditionalCatalogSources.parseOpenLibrary(work(), edition().remove("languages").let { edition().put("languages", org.json.JSONArray()) }, "OL2M", true))
    }
    @Test fun openLibraryDoesNotTreatOrdinaryProseOrAnUnlinkedWorkAsAComicEdition() {
        assertNull(AdditionalCatalogSources.selectOpenLibrary(JSONObject().put("docs", org.json.JSONArray().put(work().put("subject", org.json.JSONArray().put("Fiction"))))))
        val selected = AdditionalCatalogSources.selectOpenLibrary(JSONObject().put("docs", org.json.JSONArray().put(work())))!!
        assertEquals("OL2M", selected.second)
    }
    @Test fun laterExactResultsAreNotLostBehindEarlierUnrelatedBatches() {
        val filler = (1..48).map { ComicEntry("f$it", "f$it", "Outra obra $it", authors = "Outro", publisher = "Outra", pageCount = 0) }
        val exact = ComicEntry("exact", "exact", "Batman: Ano Um", authors = "Frank Miller", publisher = "DC", pageCount = 144)
        assertEquals("exact", CatalogSearchPolicy.ranked(filler + exact, listOf("Batman: Ano Um")).take(48).first().id)
    }
    @Test fun localizationPreservesUnknownSeriesPaginationAndRealEditorialFields() {
        val raw = series("kt-123").copy(releaseYear = 0, synopsis = "Original description")
        val result = AdditionalCatalogSources.localize(raw)
        assertEquals(0, result.pageCount); assertEquals(0, result.issueNumber); assertEquals(0, result.releaseYear)
        assertEquals("", result.publisher); assertEquals("Série de mangá", result.format)
    }
}
