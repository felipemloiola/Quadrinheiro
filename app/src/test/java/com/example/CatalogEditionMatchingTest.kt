package com.example

import com.example.data.ComicVineCatalog
import com.example.model.ComicEntry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[24,34])
class CatalogEditionMatchingTest {
    private val comic=ComicEntry("local","","Batman (2024) 2",authors="",publisher="",seriesName="Batman",issueNumber=2,releaseYear=2024)
    @Test fun resolvesUniqueEditionAndRefusesAmbiguousOrUnrelatedCredits() {
        val exact=comic.copy(id="cv-123",externalId="cv-123")
        assertEquals(exact,ComicVineCatalog.matchingEdition(comic,listOf(exact,exact.copy(id="wrong",issueNumber=3))))
        assertNull(ComicVineCatalog.matchingEdition(comic,listOf(exact,exact.copy(id="duplicate"))))
        assertNull(ComicVineCatalog.matchingEdition(comic,listOf(exact.copy(seriesName="Superman"))))
        assertNull(ComicVineCatalog.matchingEdition(comic,listOf(exact.copy(releaseYear=1990))))
    }
}
