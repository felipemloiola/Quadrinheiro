package com.example

import com.example.data.CatalogPageCountResolver
import com.example.model.ComicEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CatalogPageCountResolverTest {
    private fun book(ref: String, isbn: String = "", pages: Int = 0) =
        ComicEntry(ref, ref, "Uma HQ", authors = "", publisher = "", pageCount = pages, isbn = isbn)

    @Test fun getsMissingSearchPageCountFromTheExactGoogleVolume() = runBlocking {
        assertEquals(144, CatalogPageCountResolver.resolve(book("gb-edition")) { url ->
            assertEquals("/books/v1/volumes/edition", url.encodedPath)
            JSONObject("""{"id":"edition","volumeInfo":{"pageCount":144}}""")
        })
    }
    @Test fun rejectsAnotherEditionEvenWhenTheTitleMatches() = runBlocking {
        assertNull(CatalogPageCountResolver.resolve(book("gb-edition")) {
            JSONObject("""{"id":"other","volumeInfo":{"title":"Uma HQ","pageCount":144}}""")
        })
    }
    @Test fun readsTheSelectedOpenLibraryEditionIncludingOnePage() = runBlocking {
        assertEquals(1, CatalogPageCountResolver.resolve(book("ol-OL123M")) {
            JSONObject("""{"key":"/books/OL123M","number_of_pages":1}""")
        })
    }
    @Test fun fallbackChecksTheExactIsbnInsteadOfUsingTheFirstSearchResult() = runBlocking {
        val isbn = "9780140328721"
        assertEquals(96, CatalogPageCountResolver.resolve(book("cv-123", isbn)) { url ->
            if (url.host == "openlibrary.org") throw java.io.IOException("Indisponível")
            JSONObject("""{"items":[
                {"volumeInfo":{"pageCount":160,"industryIdentifiers":[{"type":"ISBN_13","identifier":"9788573516883"}]}},
                {"volumeInfo":{"pageCount":96,"industryIdentifiers":[{"type":"ISBN_13","identifier":"$isbn"}]}}
            ]}""")
        })
    }
    @Test fun unavailableMetadataNeverInventsATotalOrSearchesByTitle() = runBlocking {
        assertNull(CatalogPageCountResolver.resolve(book("cv-123")) { error("Não deve pesquisar sem identificador da edição") })
        assertNull(CatalogPageCountResolver.resolve(book("gb-edition")) {
            JSONObject("""{"id":"edition","volumeInfo":{"pageCount":20001}}""")
        })
    }
    @Test fun existingApiPageCountDoesNotTriggerAnotherRequest() = runBlocking {
        assertEquals(1, CatalogPageCountResolver.resolve(book("mt-1", pages = 1)) { error("Total já conhecido") })
    }
    @Test(expected = CancellationException::class) fun cancellationIsNotTurnedIntoAMissingTotal() = runBlocking {
        CatalogPageCountResolver.resolve(book("gb-edition")) { throw CancellationException() }
        Unit
    }
}
