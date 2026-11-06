package com.example.data

import android.content.Context
import com.example.model.ComicEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

object ModernCatalog {
    const val SNAPSHOT_TTL_MS: Long = 6L * 60L * 60L * 1000L
    const val MAX_SNAPSHOT_ITEMS: Int = 48
    private const val INITIAL_LOAD_TIMEOUT_MS: Long = 8_500L

    private val mutable = MutableStateFlow<List<ComicEntry>>(emptyList())
    val entries = mutable.asStateFlow()
    val current: List<ComicEntry> get() = mutable.value

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    @Volatile
    var lastLoadedEpochMs: Long = 0L
        internal set

    @Volatile
    var isRestoredFromDiskCache: Boolean = false
        internal set

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
     * Verifica se o snapshot em memória está dentro da janela de validade (TTL de 6 horas).
     * Cache expirado permite abertura instantânea na UI, mas retorna `false` aqui para
     * disparar atualização silenciosa em segundo plano sem tratá-lo como resposta viva da API.
     */
    fun isSnapshotFresh(nowEpochMs: Long = System.currentTimeMillis()): Boolean {
        if (mutable.value.isEmpty() || lastLoadedEpochMs <= 0L) return false
        val age = nowEpochMs - lastLoadedEpochMs
        return age in 0L..SNAPSHOT_TTL_MS
    }

    @Synchronized
    fun restoreFromDiskCache(storage: SafeStorage): Boolean {
        val snapshot = storage.loadModernCatalogSnapshot() ?: return false
        if (snapshot.entries.isEmpty()) return false
        val wasEmpty = mutable.value.isEmpty()
        accept(snapshot.entries)
        if (lastLoadedEpochMs == 0L || (wasEmpty && snapshot.savedAtEpochMs > 0L)) {
            lastLoadedEpochMs = snapshot.savedAtEpochMs
            isRestoredFromDiskCache = true
        }
        return true
    }

    fun restoreFromDiskCache(context: Context): Boolean =
        restoreFromDiskCache(SafeStorage(context.applicationContext))

    @Synchronized
    fun resetForTests() {
        mutable.value = emptyList()
        lastLoadedEpochMs = 0L
        isRestoredFromDiskCache = false
        nextPage = 0
        _isLoading.value = false
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

    suspend fun load(
        context: Context? = null,
        forceRefresh: Boolean = false,
        nowEpochMs: Long = System.currentTimeMillis(),
        networkFetcher: suspend () -> List<ComicEntry> = { ComicVineCatalog.catalog(0).items }
    ) {
        if (mutable.value.isEmpty() && context != null) {
            runCatching { restoreFromDiskCache(context) }
        }
        if (!forceRefresh && isSnapshotFresh(nowEpochMs)) {
            return
        }
        mutex.withLock {
            if (mutable.value.isEmpty() && context != null) {
                runCatching { restoreFromDiskCache(context) }
            }
            if (!forceRefresh && isSnapshotFresh(nowEpochMs)) {
                return@withLock
            }
            _isLoading.value = true
            try {
                val fetched = withTimeoutOrNull(INITIAL_LOAD_TIMEOUT_MS) {
                    networkFetcher()
                }
                if (!fetched.isNullOrEmpty()) {
                    val freshTimestamp = System.currentTimeMillis()
                    accept(fetched)
                    lastLoadedEpochMs = freshTimestamp
                    isRestoredFromDiskCache = false
                    nextPage = maxOf(nextPage, 1)
                    if (context != null) {
                        runCatching {
                            SafeStorage(context.applicationContext).saveModernCatalogSnapshot(
                                entries = mutable.value.take(MAX_SNAPSHOT_ITEMS),
                                savedAtEpochMs = freshTimestamp
                            )
                        }
                    }
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                // Preserva o cache já restaurado em memória caso a rede falhe
            } finally {
                _isLoading.value = false
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
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            false
        }
    }
}
