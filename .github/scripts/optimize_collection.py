from pathlib import Path

pending = {}
report = []
def once(text, old, new):
    if text.count(old) != 1:
        raise RuntimeError('Collection performance anchor not unique; no files written: ' + old[:100])
    return text.replace(old, new, 1)

def plan(path, marker, transform):
    p = Path(path)
    raw = p.read_bytes()
    text = raw.decode('utf-8').replace('\r\n', '\n')
    if marker in text:
        report.append('SKIP ' + path)
        return
    result = transform(text)
    pending[p] = (result.replace('\n', '\r\n') if b'\r\n' in raw else result).encode('utf-8')
    report.append('OK ' + path)

def cloud(text):
    text = once(text, 'import kotlinx.coroutines.flow.callbackFlow\n', 'import kotlinx.coroutines.flow.callbackFlow\nimport kotlinx.coroutines.flow.conflate\n')
    old = '.addSnapshotListener { snapshot, error ->'
    if text.count(old) != 2:
        raise RuntimeError('Expected profile and collection snapshot listeners; refusing partial change')
    text = text.replace(old, '.addSnapshotListener(FirestoreSnapshotDispatch.executor) { snapshot, error ->')
    text = once(text, '''        awaitClose {
            registration.remove()
        }
    }

    suspend fun checkUsernameAvailability(''', '''        awaitClose {
            registration.remove()
        }
    }.conflate() // COLLECTION_SNAPSHOT_BACKGROUND_V15: retain the newest full collection snapshot.

    suspend fun checkUsernameAvailability(''')
    return text

def viewmodel(text):
    text = once(text, '''            var updatedAny = false
            val healedList = _comics.value.map { comic ->''', '''            // INDEXED_CATALOG_HEALING_V15: build lookup tables once, not one catalog scan per HQ.
            val healingOwner = activeOwnerUid()
            val healingComics = _comics.value
            val catalogIndex = com.example.data.CatalogMatchIndex(CuratedCatalog.curatedComicsCatalog)
            var updatedAny = false
            val healedList = healingComics.map { comic ->''')
    text = once(text, '''                val catalogHit = CuratedCatalog.curatedComicsCatalog.firstOrNull {
                    it.externalId == comic.externalId ||
                        it.id == comic.id ||
                        it.id == comic.id.replace("init-", "cat-") ||
                        (comic.isbn.isNotBlank() && it.isbn == comic.isbn) ||
                        it.title.equals(comic.title, ignoreCase = true)
                }''', '''                val catalogHit = catalogIndex.findMatch(comic)''')
    text = once(text, '''            if (updatedAny) {
                _comics.value = healedList
                persistAll()
            }
            // Prefetch leve e prioritário:''', '''            if (updatedAny && activeOwnerUid() == healingOwner && (_comics.value === healingComics)) {
                // Do not overwrite edits or another account while cover resolution was running.
                _comics.value = healedList
                persistAll(healingOwner)
            }
            // Prefetch leve e prioritário:''')
    return text

plan('app/src/main/java/com/example/data/QuadrinheiroCloudRepository.kt', 'COLLECTION_SNAPSHOT_BACKGROUND_V15', cloud)
plan('app/src/main/java/com/example/viewmodel/QuadrinheiroViewModel.kt', 'INDEXED_CATALOG_HEALING_V15', viewmodel)

INDEX = '''package com.example.data

import com.example.model.ComicEntry
import java.util.TreeMap

/** Indexed equivalent of the existing first matching catalog-entry search. */
internal class CatalogMatchIndex(entries: List<ComicEntry>) {
    private val byExternalId = HashMap<String, IndexedValue<ComicEntry>>()
    private val byId = HashMap<String, IndexedValue<ComicEntry>>()
    private val byIsbn = HashMap<String, IndexedValue<ComicEntry>>()
    private val byTitle = TreeMap<String, IndexedValue<ComicEntry>>(String.CASE_INSENSITIVE_ORDER)

    init {
        entries.forEachIndexed { index, comic ->
            val entry = IndexedValue(index, comic)
            byExternalId.putIfAbsent(comic.externalId, entry)
            byId.putIfAbsent(comic.id, entry)
            byIsbn.putIfAbsent(comic.isbn, entry)
            byTitle.putIfAbsent(comic.title, entry)
        }
    }

    fun findMatch(comic: ComicEntry): ComicEntry? {
        var winner: IndexedValue<ComicEntry>? = null
        fun consider(candidate: IndexedValue<ComicEntry>?) {
            if (candidate != null && candidate.index < (winner?.index ?: Int.MAX_VALUE)) {
                winner = candidate
            }
        }
        consider(byExternalId[comic.externalId])
        consider(byId[comic.id])
        consider(byId[comic.id.replace("init-", "cat-")])
        if (comic.isbn.isNotBlank()) consider(byIsbn[comic.isbn])
        consider(byTitle[comic.title])
        return winner?.value
    }
}
'''
DISPATCH = '''package com.example.data

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** One serial background executor shared by snapshot registrations; never used for UI work. */
internal object FirestoreSnapshotDispatch {
    val executor: ExecutorService by lazy {
        Executors.newSingleThreadExecutor { task ->
            Thread(task, "Quadrinheiro-Firestore-Snapshots").apply { isDaemon = true }
        }
    }
}
'''
TEST = '''package com.example

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
'''
for name, content in [
    ('app/src/main/java/com/example/data/CatalogMatchIndex.kt', INDEX),
    ('app/src/main/java/com/example/data/FirestoreSnapshotDispatch.kt', DISPATCH),
    ('app/src/test/java/com/example/CollectionPerformancePolicyTest.kt', TEST)
]:
    path = Path(name)
    if path.exists() and path.read_text(encoding='utf-8') != content:
        raise RuntimeError('New performance file already exists with different content: ' + name)
    pending[path] = content.encode('utf-8')

for path, content in pending.items():
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(content)
Path('.audit').mkdir(exist_ok=True)
Path('.audit/collection_performance_v15.txt').write_text('\n'.join(report) + '\nSnapshot conversion uses a shared background executor; only full remote snapshots are conflated. Catalog lookups preserve original first-match precedence. Healing results cannot overwrite a changed source list or owner. No graphics, gestures, database schema or collection limits changed. Device frame times have not been measured. Remaining hotspots to profile: synchronous initial load, whole-state JSON writes, library filters and cover image rendering.\n', encoding='utf-8')
