package com.example

import com.example.data.QuadrinheiroCloudRepository
import com.example.model.ComicEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Trava o comportamento do merge local/nuvem: nada de HQ sumir, tradução local preservada e
 * itens só-locais precisam ser agendados para upload.
 */
class SyncMergeTest {

    private fun comic(
        id: String = "hq-1",
        updatedAt: Long = 1_000L,
        isDirty: Boolean = false,
        titlePtBr: String = "",
        synopsisPtBr: String = ""
    ) = ComicEntry(
        id = id,
        externalId = id,
        title = "HQ",
        authors = "Autor",
        publisher = "Editora",
        titlePtBr = titlePtBr,
        synopsisPtBr = synopsisPtBr,
        updatedAt = updatedAt,
        isDirty = isDirty
    )

    @Test
    fun remoteNewerWinsAndClearsDirtyFlag() {
        val local = comic(updatedAt = 1_000L, isDirty = true)
        val remote = comic(updatedAt = 2_000L)

        val (merged, toUpload) = QuadrinheiroCloudRepository.mergeLocalAndRemoteComics(
            localComics = listOf(local),
            remoteComics = listOf(remote)
        )

        assertEquals(1, merged.size)
        assertEquals(2_000L, merged.first().updatedAt)
        assertFalse(merged.first().isDirty)
        assertTrue(toUpload.isEmpty())
    }

    @Test
    fun dirtyLocalNewerWinsAndIsQueuedForUpload() {
        val local = comic(updatedAt = 3_000L, isDirty = true)
        val remote = comic(updatedAt = 2_000L)

        val (merged, toUpload) = QuadrinheiroCloudRepository.mergeLocalAndRemoteComics(
            localComics = listOf(local),
            remoteComics = listOf(remote)
        )

        assertEquals(3_000L, merged.first().updatedAt)
        assertTrue(merged.first().isDirty)
        assertEquals(1, toUpload.size)
        assertEquals("hq-1", toUpload.first().id)
    }

    @Test
    fun comicThatExistsOnlyLocallyIsKeptAndScheduledForUpload() {
        val local = comic(id = "so-local", updatedAt = 1_500L)
        val remote = comic(id = "outra-hq", updatedAt = 2_000L)

        val (merged, toUpload) = QuadrinheiroCloudRepository.mergeLocalAndRemoteComics(
            localComics = listOf(local),
            remoteComics = listOf(remote)
        )

        assertEquals(2, merged.size)
        assertTrue(merged.any { it.id == "so-local" })
        assertEquals(listOf("so-local"), toUpload.map { it.id })
        assertTrue(toUpload.first().isDirty)
    }

    @Test
    fun localTranslationSurvivesWhenRemoteHasNone() {
        val local = comic(updatedAt = 1_000L, isDirty = true, titlePtBr = "Batman: Ano Um")
        val remote = comic(updatedAt = 2_000L, titlePtBr = "")

        val (merged, _) = QuadrinheiroCloudRepository.mergeLocalAndRemoteComics(
            localComics = listOf(local),
            remoteComics = listOf(remote)
        )

        assertEquals("Batman: Ano Um", merged.first().titlePtBr)
    }

    /**
     * Política atual (LWW por relógio do aparelho): edição local suja mais antiga perde para a
     * nuvem. Documentada aqui para que uma mudança de política seja intencional e revise o teste.
     */
    @Test
    fun dirtyButOlderLocalEditLosesToRemoteUnderCurrentPolicy() {
        val local = comic(updatedAt = 900L, isDirty = true)
        val remote = comic(updatedAt = 2_000L)

        val (merged, toUpload) = QuadrinheiroCloudRepository.mergeLocalAndRemoteComics(
            localComics = listOf(local),
            remoteComics = listOf(remote)
        )

        assertEquals(2_000L, merged.first().updatedAt)
        assertTrue(toUpload.isEmpty())
    }
}
