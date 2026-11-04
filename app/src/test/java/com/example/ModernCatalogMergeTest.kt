package com.example

import com.example.data.ModernCatalog
import com.example.model.ComicEntry
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * O catálogo acumulado precisa aceitar metadados novos sem nunca regredir para um resultado
 * mais pobre (busca costuma devolver itens sem capa/sinopse de um volume já conhecido).
 */
class ModernCatalogMergeTest {

    private fun comic(
        title: String = "Batman",
        authors: String = "",
        coverUrl: String = "",
        synopsis: String = "",
        seriesName: String = "",
        isbn: String = "",
        pageCount: Int = 0
    ) = ComicEntry(
        id = "cv-1",
        externalId = "cv-1",
        title = title,
        authors = authors,
        publisher = "DC",
        coverUrl = coverUrl,
        synopsis = synopsis,
        seriesName = seriesName,
        isbn = isbn,
        pageCount = pageCount
    )

    @Test
    fun richerIncomingReplacesKnownEntry() {
        val known = comic()
        val incoming = comic(coverUrl = "https://img/1.jpg", synopsis = "Sinopse", pageCount = 96)
        assertEquals(incoming, ModernCatalog.preferred(known, incoming))
    }

    @Test
    fun poorerIncomingDoesNotEraseKnownMetadata() {
        val known = comic(coverUrl = "https://img/1.jpg", synopsis = "Sinopse", pageCount = 96)
        val incoming = comic()
        assertEquals(known, ModernCatalog.preferred(known, incoming))
    }

    @Test
    fun tiedCompletenessUsesIncomingSoTranslationsRefresh() {
        val known = comic(coverUrl = "https://img/old.jpg")
        val incoming = comic(coverUrl = "https://img/new.jpg")
        assertEquals("https://img/new.jpg", ModernCatalog.preferred(known, incoming).coverUrl)
    }

    @Test
    fun sameComicKeepsItsIdentityAfterMerge() {
        val merged = ModernCatalog.preferred(comic(), comic(coverUrl = "https://img/1.jpg"))
        assertEquals("cv-1", merged.id)
        assertEquals("cv-1", merged.externalId)
    }
}
