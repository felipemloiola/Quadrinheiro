package com.example

import com.example.data.CatalogSearchPolicy
import com.example.data.PortugueseBookSources
import com.example.model.ComicEntry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PortugueseSearchTest {
    private fun comic(id: String, pages: Int = 0, isbn: String = "", format: String = "Encadernado") =
        ComicEntry(id, id, "Batman: Ano Um", authors = "Frank Miller", publisher = "Panini", releaseYear = 2011, format = format, pageCount = pages, isbn = isbn)

    @Test fun literalInputIsKeptAndKnownTitlesAcceptSmallTypos() {
        assertEquals(listOf("Watchmem", "Watchmen"), CatalogSearchPolicy.variants("Watchmem"))
        assertEquals("batma ano un", CatalogSearchPolicy.variants("batma ano un").first())
        assertTrue(CatalogSearchPolicy.variants("batma ano un").contains("Batman: Ano Um"))
        assertEquals(listOf("Watchmen"), CatalogSearchPolicy.variants("Watchmen"))
    }
    @Test fun accentsAndPartialTitlesWorkWithoutChangingTheTextField() {
        assertEquals("monstro do pantano", CatalogSearchPolicy.normalize("Monstro do Pântano"))
        assertEquals(listOf("batm", "Batman"), CatalogSearchPolicy.variants("batm"))
        assertEquals(listOf("xy"), CatalogSearchPolicy.variants("xy"))
    }
    @Test fun isbnTenAndThirteenIdentifyTheSameEditionButInvalidIsbnsDoNot() {
        assertEquals("9781401207526", CatalogSearchPolicy.canonicalIsbn("1-4012-0752-9"))
        assertEquals("9781401207526", CatalogSearchPolicy.canonicalIsbn("9781401207526"))
        assertNull(CatalogSearchPolicy.canonicalIsbn("9781401207520"))
        val items = CatalogSearchPolicy.unique(listOf(comic("a", isbn = "1401207529"), comic("b", pages = 144, isbn = "9781401207526")))
        assertEquals(1, items.size); assertEquals(144, items.single().pageCount)
    }
    @Test fun differentFormatsAreNotMergedWithoutAnIsbn() {
        val items = CatalogSearchPolicy.unique(listOf(comic("monthly", format = "Mensal"), comic("collected", format = "Encadernado")))
        assertEquals(2, items.size)
    }
    @Test fun incompleteMetadataDoesNotCollapseDifferentProviderIds() {
        val a = comic("a").copy(publisher = "", authors = "")
        val b = a.copy(id = "b", externalId = "b")
        assertEquals(2, CatalogSearchPolicy.unique(listOf(a, b)).size)
    }
    @Test fun appleMetadataDoesNotInventPagesYearOrPublisher() {
        val root = JSONObject("""{"results":[{"trackId":1,"trackName":"HQ brasileira","artistName":"Autora","sellerName":"Distribuidora","genres":["Comics & Graphic Novels"]}]}""")
        val book = PortugueseBookSources.parseApple(root).single()
        assertEquals(0, book.pageCount); assertEquals(0, book.releaseYear); assertEquals("", book.publisher)
        assertEquals("ab-BR-1", book.externalId)
    }
    @Test fun googleUsesTheActualPageCountAndIsbn() {
        val root = JSONObject("""{"items":[{"id":"test","volumeInfo":{"title":"Batman: Ano Um","categories":["Comics & Graphic Novels"],"pageCount":144,"industryIdentifiers":[{"type":"ISBN_10","identifier":"1401207529"}]}}]}""")
        val book = PortugueseBookSources.parseGoogle(root).single()
        assertEquals(144, book.pageCount); assertEquals("9781401207526", book.isbn)
    }
    @Test fun proseBooksAreNotAddedToComicSearchResults() {
        val root = JSONObject("""{"items":[{"id":"prose","volumeInfo":{"title":"Batman romance","categories":["Fiction"]}}]}""")
        assertTrue(PortugueseBookSources.parseGoogle(root).isEmpty())
    }
}
