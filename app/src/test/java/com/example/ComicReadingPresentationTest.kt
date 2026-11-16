package com.example

import com.example.data.ComicReadingPresentation
import com.example.model.ComicEntry
import com.example.model.ReadingStatus
import org.junit.Assert.*
import org.junit.Test

class ComicReadingPresentationTest {
    private val comic=ComicEntry("fixture","edition","HQ",authors="",publisher="",pageCount=144,currentPage=32)
    @Test fun anUnregisteredComicNeverInheritsTheCatalogDefaultReadStatus() {
        assertEquals("Registrar leitura",ComicReadingPresentation.from(null).action)
        assertNull(ComicReadingPresentation.from(null).progress)
    }
    @Test fun readingUsesOnlyValidTotalAndNeverMutatesTheEntry() {
        val reading=comic.copy(status=ReadingStatus.READING)
        assertEquals(32f/144,ComicReadingPresentation.from(reading).progress!!,.0001f)
        assertNull(ComicReadingPresentation.from(reading.copy(pageCount=0)).progress)
        assertEquals(1f,ComicReadingPresentation.from(reading.copy(currentPage=999)).progress!!,0f)
        assertEquals(32,reading.currentPage)
    }
    @Test fun readAndWishlistDoNotExposeStaleProgressOrFabricateRatings() {
        val read=ComicReadingPresentation.from(comic.copy(status=ReadingStatus.READ,rating=0f,readDate=""))
        assertEquals("Editar registro",read.action);assertEquals("",read.detail);assertNull(read.progress)
        val want=ComicReadingPresentation.from(comic.copy(status=ReadingStatus.WANT_TO_READ,rating=4f))
        assertEquals("Iniciar leitura",want.action);assertEquals("Ainda não iniciada",want.detail);assertNull(want.progress)
    }
}
