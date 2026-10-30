package com.example

import com.example.data.CatalogMatchIndex
import com.example.data.FirestoreSnapshotDispatch
import com.example.model.ComicEntry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionPerformancePolicyTest {
    private fun comic(id: String, external: String, title: String, isbn: String = "") =
        ComicEntry(id = id, externalId = external, title = title,
            authors = "Author", publisher = "Publisher", isbn = isbn)

    private fun previousMatch(catalog: List<ComicEntry>, comic: ComicEntry): ComicEntry? =
        catalog.firstOrNull {
            it.externalId == comic.externalId || it.id == comic.id ||
                it.id == comic.id.replace("init-", "cat-") ||
                (comic.isbn.isNotBlank() && it.isbn == comic.isbn) ||
                it.title.equals(comic.title, ignoreCase = true)
        }

    @Test
    fun indexPreservesPreviousFirstMatchBehaviorForLargerCatalog() {
        val catalog = (0 until 1000).map { comic("cat-$it", "ext-$it", "Title $it", "isbn-$it") } +
            comic("duplicate", "ext-10", "TITLE 10") + comic("blank-entry", "", "Blank")
        val index = CatalogMatchIndex(catalog)
        val queries = (0 until 1000).flatMap {
            listOf(
                comic("init-$it", "unknown-$it", "other"),
                comic("unknown-$it", "ext-$it", "other"),
                comic("unknown-$it", "unknown-$it", "title $it"),
                comic("unknown-$it", "unknown-$it", "other", "isbn-$it")
            )
        } + comic("cat-900", "ext-10", "Title 500") + comic("none", "", "none")
        queries.forEach { assertSame(previousMatch(catalog, it), index.findMatch(it)) }
    }

    @Test
    fun emptyCatalogAndNoMatchRemainNull() {
        val query = comic("none", "none", "Missing")
        assertNull(CatalogMatchIndex(emptyList()).findMatch(query))
        assertNull(CatalogMatchIndex(listOf(comic("one", "one", "One"))).findMatch(query))
    }

    @Test
    fun snapshotExecutorRunsOffTheCallingThread() {
        val caller = Thread.currentThread()
        val actual = AtomicReference<Thread>()
        val ready = CountDownLatch(1)
        FirestoreSnapshotDispatch.executor.execute {
            actual.set(Thread.currentThread())
            ready.countDown()
        }
        assertTrue(ready.await(5, TimeUnit.SECONDS))
        assertNotSame(caller, actual.get())
        assertEquals("Quadrinheiro-Firestore-Snapshots", actual.get().name)
    }
}
