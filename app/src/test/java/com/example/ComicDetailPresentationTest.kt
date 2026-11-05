package com.example

import com.example.data.CatalogDetails
import com.example.data.CatalogPortraits
import com.example.data.ComicDetailPresentation
import com.example.model.ComicEntry
import org.junit.Assert.*
import org.junit.Test

class ComicDetailPresentationTest {
    private fun comic(id: String, external: String = id) = ComicEntry(id, external, "Obra $id", authors = "", publisher = "")
    private fun details(issues: List<ComicEntry>) = CatalogDetails("1", "issue", "HQ", "", "", "", "", "", "", emptyList(), emptyList(), emptyList(), issues)

    @Test fun missingSourceRelationshipsNeverProduceGenericSuggestions() {
        assertTrue(ComicDetailPresentation.relatedEditions(comic("cv-1"), null).isEmpty())
        assertTrue(ComicDetailPresentation.relatedEditions(comic("cv-1"), details(emptyList())).isEmpty())
    }
    @Test fun currentEditionIsRemovedUsingBothLocalAndExternalIdentity() {
        val current = comic("local-1", "cv-1")
        val related = ComicDetailPresentation.relatedEditions(current, details(listOf(current, comic("cv-1"), comic("another-local", "cv-1"), comic("cv-2"))))
        assertEquals(listOf("cv-2"), related.map { it.id })
    }
    @Test fun repeatedSourceEditionsAreDeduplicatedAndTheShelfIsBounded() {
        val items = listOf(comic("cv-2"), comic("copy", "cv-2")) + (3..20).map { comic("cv-$it") }
        val related = ComicDetailPresentation.relatedEditions(comic("cv-1"), details(items))
        assertEquals(8, related.size)
        assertEquals(related.size, related.map { it.externalId }.distinct().size)
        assertEquals("cv-2", related.first().id)
    }
    @Test fun emptyExternalIdsDoNotDiscardDistinctEditions() {
        val related = ComicDetailPresentation.relatedEditions(comic("local", ""), details(listOf(comic("a", ""), comic("b", ""))))
        assertEquals(listOf("a", "b"), related.map { it.id })
    }
    @Test fun portraitsRequireKnownResourceTypesAndExactProviderIds() {
        assertTrue(CatalogPortraits.validResource("character", "1699"))
        assertTrue(CatalogPortraits.validResource("person", "mt-123"))
        assertFalse(CatalogPortraits.validResource("volume", "1699"))
        assertFalse(CatalogPortraits.validResource("character", "Batman"))
        assertFalse(CatalogPortraits.validResource("person", "../123"))
    }
    @Test fun creditLabelsPreserveUnrecognizedRolesWithoutInventingCredits() {
        assertEquals("Roteiro, Arte, Tradução", ComicDetailPresentation.creditRole("writer, artist, Tradução, writer"))
        assertEquals("", ComicDetailPresentation.creditRole(" , "))
    }
}
