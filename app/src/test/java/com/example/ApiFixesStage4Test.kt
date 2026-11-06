package com.example

import com.example.data.CatalogSearchPolicy
import com.example.model.ComicEntry
import org.junit.Assert.*
import org.junit.Test

class ApiFixesStage4Test {

    private fun testEntry(
        id: String,
        title: String,
        issueNumber: Int = 0,
        authors: String = "",
        pageCount: Int = 160
    ): ComicEntry {
        return ComicEntry(
            id = id,
            externalId = id,
            title = title,
            authors = authors,
            publisher = "DC / Vertigo",
            issueNumber = issueNumber,
            pageCount = pageCount,
            coverUrl = "https://example.com/cover.jpg"
        )
    }

    @Test
    fun swampThingSearchPrioritizesSagaOverObscureSingleIssue() {
        val obscureSingle = testEntry(
            id = "cv-9999",
            title = "Swamp Thing #47",
            issueNumber = 47,
            authors = "",
            pageCount = 24
        )
        val canonicalSaga = testEntry(
            id = "cv-1001",
            title = "A Saga do Monstro do Pântano",
            issueNumber = 0,
            authors = "Alan Moore",
            pageCount = 216
        )

        val candidates = listOf(obscureSingle, canonicalSaga)
        val results = CatalogSearchPolicy.filterAndRankForQuery(candidates, "monstro do pantano")

        assertEquals(2, results.size)
        assertEquals("cv-1001", results[0].id)
        assertEquals("A Saga do Monstro do Pântano", results[0].title)
    }

    @Test
    fun daredevilSearchPrioritizesBornAgainOverRandomIssue() {
        val randomIssue = testEntry(
            id = "cv-5555",
            title = "Daredevil #312",
            issueNumber = 312,
            pageCount = 32
        )
        val bornAgain = testEntry(
            id = "cv-2002",
            title = "Demolidor: A Queda de Murdock",
            issueNumber = 0,
            authors = "Frank Miller, David Mazzucchelli",
            pageCount = 180
        )

        val candidates = listOf(randomIssue, bornAgain)
        val results = CatalogSearchPolicy.filterAndRankForQuery(candidates, "demolidor")

        assertEquals("cv-2002", results[0].id)
    }
}
