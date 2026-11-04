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
    fun accept(items: List<ComicEntry>) {
        mutable.update { previous -> CatalogSearchPolicy.unique(previous + items).takeLast(396) }
    }
    suspend fun load() {
        mutex.withLock {
            if (mutable.value.isEmpty()) try { accept(ComicVineCatalog.catalog(0).items) }
            catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel } catch (_: Exception) { }
        }
    }
}
