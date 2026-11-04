package com.example

import com.example.data.CatalogEpoch
import com.example.data.CloudComicDocument
import com.example.model.ComicEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Protege o filtro do reset e o carimbo de upload: HQ criada com relógio atrasado
 * não pode sumir da nuvem só porque `updatedAt` ficou abaixo de `RESET_AT`.
 */
class CatalogEpochUploadTest {

    private fun comic(updatedAt: Long) = ComicEntry(
        id = "hq-epoch",
        externalId = "cv-epoch",
        title = "HQ",
        authors = "Autor",
        publisher = "Editora",
        updatedAt = updatedAt
    )

    @Test
    fun afterResetKeepsOnlyStrictlyNewerComics() {
        assertFalse(CatalogEpoch.afterReset(comic(CatalogEpoch.RESET_AT)))
        assertFalse(CatalogEpoch.afterReset(comic(CatalogEpoch.RESET_AT - 1)))
        assertTrue(CatalogEpoch.afterReset(comic(CatalogEpoch.RESET_AT + 1)))
    }

    @Test
    fun uploadKeepsTimestampFromAfterTheReset() {
        val original = CatalogEpoch.RESET_AT + 5_000L
        val doc = CloudComicDocument.fromComicEntry("uid-test", comic(original))
        assertEquals(original, doc.updatedAtMillis)
    }

    @Test
    fun uploadRefreshesMissingTimestamp() {
        val before = System.currentTimeMillis()
        val doc = CloudComicDocument.fromComicEntry("uid-test", comic(0L))
        assertTrue(doc.updatedAtMillis >= before)
    }

    @Test
    fun uploadRefreshesClockSkewedTimestampBelowReset() {
        val before = System.currentTimeMillis()
        val doc = CloudComicDocument.fromComicEntry("uid-test", comic(CatalogEpoch.RESET_AT - 60_000L))
        assertTrue(doc.updatedAtMillis >= before)
    }
}
