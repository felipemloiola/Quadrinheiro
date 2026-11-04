package com.example.data

import com.example.model.ComicEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object ModernCatalog {
    private val mutable = MutableStateFlow<List<ComicEntry>>(emptyList())
    val entries = mutable.asStateFlow()
    val current: List<ComicEntry> get() = mutable.value
    private val mutex = Mutex()
    private var nextPage = 0
    fun accept(items: List<ComicEntry>) {
        mutable.update { previous -> CatalogSearchPolicy.unique(previous + items).takeLast(396) }
    }
    suspend fun load() {
        mutex.withLock {
            if (mutable.value.isEmpty()) try {
                accept(ComicVineCatalog.catalog(0).items)
                nextPage = maxOf(nextPage, 1)
            } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel } catch (_: Exception) { }
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
        } catch (cancel: kotlinx.coroutines.CancellationException) {
            throw cancel
        } catch (_: Exception) {
            false
        }
    }
}
