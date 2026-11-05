package com.example

import com.example.data.CatalogSearchPolicy
import com.example.model.ComicEntry
import org.junit.Assert.*
import org.junit.Test

class ApiFixesStage2Test {

    private fun testEntry(
        id: String,
        title: String,
        issueNumber: Int = 0,
        authors: String = "Alan Moore",
        publisher: String = "DC / Vertigo",
        pageCount: Int = 160,
        format: String = "Encadernado (TPB)",
        isbn: String = "",
        coverUrl: String = ""
    ): ComicEntry {
        return ComicEntry(
            id = id,
            externalId = id,
            title = title,
            authors = authors,
            publisher = publisher,
            issueNumber = issueNumber,
            pageCount = pageCount,
            format = format,
            isbn = isbn,
            coverUrl = coverUrl
        )
    }

    @Test
    fun mergesUnnumberedCollectedEditionWithEquivalentVolumeOne() {
        val cvEntry = testEntry(
            id = "cv-100",
            title = "A Saga do Monstro do Pântano",
            authors = "Alan Moore",
            publisher = "DC / Vertigo",
            issueNumber = 0,
            format = "Encadernado (TPB)",
            pageCount = 216,
            coverUrl = "https://example.com/cv.jpg"
        )
        val appleEntry = testEntry(
            id = "ab-BR-200",
            title = "A Saga do Monstro do Pântano - Volume 1",
            authors = "Alan Moore, Stephen Bissette",
            publisher = "Panini",
            issueNumber = 1,
            format = "Edição",
            pageCount = 216,
            coverUrl = "https://example.com/apple_hd.jpg"
        )

        assertTrue(CatalogSearchPolicy.shouldMergeEntries(cvEntry, appleEntry))

        val merged = CatalogSearchPolicy.deduplicateByCanonicalWork(listOf(cvEntry, appleEntry))
        assertEquals(1, merged.size)
        assertEquals("Alan Moore, Stephen Bissette", merged[0].authors)
        assertEquals("https://example.com/apple_hd.jpg", merged[0].coverUrl)
    }

    @Test
    fun doesNotMergeWhenIsbnsAreDifferent() {
        val a = testEntry(id = "1", title = "Watchmen", issueNumber = 0, isbn = "9788573516883", format = "Encadernado")
        val b = testEntry(id = "2", title = "Watchmen", issueNumber = 0, isbn = "9781401245252", format = "Encadernado")
        assertFalse(CatalogSearchPolicy.shouldMergeEntries(a, b))
    }

    @Test
    fun doesNotMergeWhenPageCountsDrasticallyDifferLikeOmnibusVsTrade() {
        val omnibus = testEntry(id = "1", title = "Monstro do Pântano", issueNumber = 0, pageCount = 820, format = "Omnibus")
        val trade = testEntry(id = "2", title = "Monstro do Pântano Vol. 1", issueNumber = 1, pageCount = 176, format = "Encadernado (TPB)")
        assertFalse(CatalogSearchPolicy.shouldMergeEntries(omnibus, trade))
    }

    @Test
    fun doesNotMergeDistinctNumberedIssues() {
        val issue1 = testEntry(id = "1", title = "Monstro do Pântano", issueNumber = 1, pageCount = 160)
        val issue2 = testEntry(id = "2", title = "Monstro do Pântano", issueNumber = 2, pageCount = 160)
        assertFalse(CatalogSearchPolicy.shouldMergeEntries(issue1, issue2))
    }

    @Test
    fun doesNotMergeSingleMonthlyIssueWithUnnumberedCollection() {
        val singleIssue1 = testEntry(id = "1", title = "Monstro do Pântano", issueNumber = 1, pageCount = 28, format = "Edição")
        val collection = testEntry(id = "2", title = "Monstro do Pântano", issueNumber = 0, pageCount = 200, format = "Encadernado")
        assertFalse(CatalogSearchPolicy.shouldMergeEntries(singleIssue1, collection))
    }
}
