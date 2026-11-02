package com.example

import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

class ActiveReadingTest {
    private val first = ComicEntry("first", "external-first", "Primeira", authors = "", publisher = "", status = ReadingStatus.READING)
    private val second = first.copy(id = "second", externalId = "external-second", title = "Segunda")

    @Test fun activeReadingWinsOverListOrderIncludingExternalIds() {
        val books = listOf(first, second)
        assertEquals(second, activeReadingComic(books, "second"))
        assertEquals(second, activeReadingComic(books, "external-second"))
    }

    @Test fun missingOrCompletedSelectionFallsBackToFirstCurrentReading() {
        assertEquals(first, activeReadingComic(listOf(first, second), "removed"))
        assertEquals(first, activeReadingComic(listOf(second.copy(status = ReadingStatus.READ), first), "second"))
        assertNull(activeReadingComic(listOf(first.copy(status = ReadingStatus.WANT_TO_READ)), "first"))
    }
}
