package com.example

import com.example.model.CatalogPagePolicy
import com.example.model.ComicEntry
import com.example.model.ReadingStatus
import org.junit.Assert.*
import org.junit.Test

class CatalogPagePolicyTest {
    private fun book(status: ReadingStatus = ReadingStatus.READING, current: Int = 50) =
        ComicEntry("book", "mt-1", "HQ", authors = "Autora", publisher = "Editora", status = status,
            pageCount = 160, currentPage = current, rating = 4.5f, review = "Minha resenha", tags = listOf("Pessoal"))
    @Test fun realRemotePagesPopulateNewAndUnknownEditions() {
        assertEquals(144, CatalogPagePolicy.remoteCount(0, false, false, 144))
        assertEquals(144, CatalogPagePolicy.remoteCount(1, true, false, 144))
        assertEquals(1, CatalogPagePolicy.remoteCount(0, false, false, 1))
    }
    @Test fun unknownOrInvalidRemotePagesAreNeverInvented() {
        assertNull(CatalogPagePolicy.remoteCount(0, false, false, 0))
        assertNull(CatalogPagePolicy.remoteCount(0, false, false, 20001))
    }
    @Test fun remoteDataPreservesSavedManualCountsAndActiveUserEdits() {
        assertNull(CatalogPagePolicy.remoteCount(320, true, false, 144))
        assertNull(CatalogPagePolicy.remoteCount(0, false, true, 144))
    }
    @Test fun blankPagesRemainOptionalAndInvalidInputIsRejected() {
        listOf("", "0", "20001", "invalid").forEach { assertNull(CatalogPagePolicy.count(it)) }
        assertEquals(144, CatalogPagePolicy.count("144"))
    }
    @Test fun unknownTotalsDoNotCreateProgressInAnyStatus() {
        ReadingStatus.entries.forEach { status ->
            val saved = CatalogPagePolicy.withCount(book(status), 0)
            assertEquals(0, saved.pageCount); assertEquals(0, saved.currentPage)
        }
    }
    @Test fun applyingPagesPreservesPrivateReviewTagsAndRating() {
        val original = book()
        val edited = CatalogPagePolicy.withCount(original, 144)
        assertEquals(50, edited.currentPage)
        assertEquals(original.review, edited.review); assertEquals(original.tags, edited.tags); assertEquals(original.rating, edited.rating)
        assertEquals(144, CatalogPagePolicy.withCount(book(ReadingStatus.READ), 144).currentPage)
        assertEquals(0, CatalogPagePolicy.withCount(book(ReadingStatus.WANT_TO_READ), 144).currentPage)
        assertEquals(30, CatalogPagePolicy.withCount(book(), 30).currentPage)
    }
}
