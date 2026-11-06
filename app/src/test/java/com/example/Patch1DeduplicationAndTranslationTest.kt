package com.example

import com.example.data.CatalogSearchPolicy
import com.example.data.PortugueseComicTranslator
import com.example.model.ComicEntry
import com.example.model.ReadingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Patch1DeduplicationAndTranslationTest {

    private fun sample(
        id: String,
        title: String,
        authors: String = "",
        publisher: String = "",
        isbn: String = "",
        issueNumber: Int = 0,
        pageCount: Int = 160,
        releaseYear: Int = 2020,
        synopsis: String = "",
        coverUrl: String = ""
    ) = ComicEntry(
        id = id,
        externalId = id,
        title = title,
        authors = authors,
        publisher = publisher,
        isbn = isbn,
        issueNumber = issueNumber,
        pageCount = pageCount,
        releaseYear = releaseYear,
        synopsis = synopsis,
        coverUrl = coverUrl,
        status = ReadingStatus.READ
    )

    @Test
    fun immediateMergeWhenIsbnIsIdenticalEvenWithSlightTitleDifferences() {
        // ISBN 9788573516548 is valid
        val itemA = sample("a1", "Watchmen: Edição Definitiva", authors = "Alan Moore", isbn = "9788573516548", pageCount = 416)
        val itemB = sample("b1", "Watchmen (Absolute Edition)", authors = "Alan Moore, Dave Gibbons", isbn = "978-85-7351-654-8", pageCount = 416)

        assertTrue(CatalogSearchPolicy.shouldMergeEntries(itemA, itemB))
    }

    @Test
    fun neverMergeWhenIsbnsAreValidAndDifferent() {
        // Two distinct valid ISBNs
        val itemA = sample("a1", "Watchmen", isbn = "9788573516548")
        val itemB = sample("b1", "Watchmen", isbn = "9788542605228")

        assertFalse(CatalogSearchPolicy.shouldMergeEntries(itemA, itemB))
    }

    @Test
    fun bilingualCanonicalWorkAndAuthorCompatibilityMergeCorrectly() {
        val itemA = sample("a1", "Batman: Ano Um", authors = "Frank Miller", releaseYear = 1987)
        val itemB = sample("b1", "Batman: Year One", authors = "Frank Miller, David Mazzucchelli", releaseYear = 1987)

        assertTrue(CatalogSearchPolicy.shouldMergeEntries(itemA, itemB))
    }

    @Test
    fun distinctAuthorsPreventFalseMergeOnGenericTitle() {
        val itemA = sample("a1", "Homem-Aranha", authors = "Stan Lee")
        val itemB = sample("b1", "Homem-Aranha", authors = "Dan Slott")

        assertFalse(CatalogSearchPolicy.shouldMergeEntries(itemA, itemB))
    }

    @Test
    fun mergeComplementaryEditionDataPreservesBestFieldsAndNeverOverwritesWithEmpty() {
        val primary = sample(
            id = "p1",
            title = "Saga of the Swamp Thing",
            authors = "Alan Moore",
            publisher = "Editora Independente",
            isbn = "",
            pageCount = 0,
            synopsis = "",
            coverUrl = "https://example.com/cover_hd.jpg"
        )
        val secondary = sample(
            id = "s1",
            title = "A Saga do Monstro do Pântano",
            authors = "Alan Moore, Stephen Bissette",
            publisher = "DC / Vertigo",
            isbn = "9788573516548",
            pageCount = 216,
            synopsis = "Collects issues #20-27 of Saga of the Swamp Thing.",
            coverUrl = "https://example.com/placeholder.jpg"
        )

        val merged = CatalogSearchPolicy.mergeComplementaryEditionData(primary, secondary)

        assertEquals("Alan Moore, Stephen Bissette", merged.authors)
        assertEquals("DC / Vertigo", merged.publisher)
        assertEquals(216, merged.pageCount)
        assertEquals("9788573516548", merged.isbn)
        assertEquals("https://example.com/cover_hd.jpg", merged.coverUrl)
    }

    @Test
    fun editorialPhrasesAreTranslatedInPortugueseCleanSynopsis() {
        val raw = "Collects issues #1-6. Originally published in 1986. Written by Alan Moore. Illustrated by Dave Gibbons."
        val formatted = PortugueseComicTranslator.formatCleanSynopsis(raw)

        assertTrue(formatted.contains("Reúne as edições #1-6"))
        assertTrue(formatted.contains("Originalmente publicado em 1986"))
        assertTrue(formatted.contains("Roteiro de Alan Moore"))
        assertTrue(formatted.contains("Ilustrado por Dave Gibbons"))
    }
}
