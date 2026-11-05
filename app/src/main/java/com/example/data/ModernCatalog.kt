package com.example.data

import com.example.model.ComicEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object ModernCatalog {
    private val mutable = MutableStateFlow<List<ComicEntry>>(emptyList())
    val entries = mutable.asStateFlow()
    val current: List<ComicEntry> get() = mutable.value
    private val mutex = Mutex()
    private var nextPage = 0

    @Synchronized
    fun accept(items: List<ComicEntry>) {
        if (items.isEmpty()) return
        val merged = LinkedHashMap<String, ComicEntry>(mutable.value.size + items.size)
        (mutable.value + items).forEach { candidate ->
            merged[candidate.id] = merged[candidate.id]?.let { known -> preferred(known, candidate) } ?: candidate
        }
        mutable.value = merged.values.toList().takeLast(396)
    }

    /**
     * O catálogo antes mantinha sempre a primeira versão do id (`distinctBy`), então capa,
     * sinopse e páginas nunca eram atualizadas. Agora a versão nova entra, exceto quando é
     * mais pobre que a já conhecida — nesse caso a antiga é preservada.
     */
    internal fun preferred(existing: ComicEntry, incoming: ComicEntry): ComicEntry =
        if (richness(incoming) >= richness(existing)) incoming else existing

    private fun richness(c: ComicEntry) =
        listOf(c.title, c.authors, c.coverUrl, c.synopsis, c.seriesName, c.isbn).count { it.isNotBlank() } +
            (if (c.pageCount > 0) 1 else 0)

    suspend fun load() {
        mutex.withLock {
            if (mutable.value.isEmpty()) {
                try {
                    accept(ComicVineCatalog.catalog(0).items)
                    nextPage = maxOf(nextPage, 1)
                } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel } catch (_: Exception) { }
            }
        }
    }

    suspend fun loadMore(universe: String = ""): Boolean = mutex.withLock {
        val targetPage = nextPage
        try {
            val page = MultiApiSearchEngine().fetchCatalogPage(targetPage, universe)
            if (page.items.isNotEmpty()) {
                accept(page.items)
                nextPage = targetPage + 1
            }
            page.hasMore
        } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel } catch (_: Exception) { false }
    }
}
