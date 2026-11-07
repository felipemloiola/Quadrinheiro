package com.example.data

import com.example.model.AppSettings
import com.example.model.ComicEntry
import com.example.model.ReadingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.security.MessageDigest

/**
 * Representa uma recomendação com pontuação, fonte do sinal e motivo em português.
 */
data class RecommendedComic(
    val comic: ComicEntry,
    val reasonLabel: String,
    val score: Int,
    val signalSource: String
)

/**
 * Perfil de gosto do usuário extraído de toda a biblioteca existente e dos favoritos.
 * Preserva 100% dos dados prévios sem jamais resetar o acervo.
 */
data class UserTasteProfile(
    val readingSeries: List<Pair<String, Int>>, // baseSeriesTitle to currentMaxIssue
    val readSeries: List<Pair<String, Int>>,
    val topAuthors: List<String>, // Autores 4.5★ - 5★ ou com re-leituras
    val favoriteCharacters: List<String>,
    val favoriteComics: List<ComicEntry>,
    val wantToReadSeries: List<String>,
    val favoriteUniverse: String,
    val tasteHash: String
)

object RecommendationCandidateRetriever {
    private const val TTL_MS = 12 * 60 * 60 * 1000L // 12 horas

    data class MemoryPoolCache(
        val ownerUid: String,
        val hash: String,
        val timestamp: Long,
        val pool: List<ComicEntry>
    )

    @Volatile
    private var memoryPool: MemoryPoolCache? = null

    fun isCacheValid(currentHash: String, ownerUid: String = ""): Boolean {
        val cached = memoryPool ?: return false
        val matchesOwner = ownerUid.isBlank() || cached.ownerUid == ownerUid
        return matchesOwner &&
            currentHash == cached.hash &&
            (System.currentTimeMillis() - cached.timestamp) < TTL_MS &&
            cached.pool.isNotEmpty()
    }

    fun getCachedPool(ownerUid: String = ""): List<ComicEntry> {
        val cached = memoryPool ?: return emptyList()
        if (ownerUid.isNotBlank() && cached.ownerUid != ownerUid) return emptyList()
        return cached.pool
    }

    fun updateCache(hash: String, pool: List<ComicEntry>, ownerUid: String = "") {
        memoryPool = MemoryPoolCache(
            ownerUid = ownerUid,
            hash = hash,
            timestamp = System.currentTimeMillis(),
            pool = pool
        )
    }

    fun clearForAccountSwitch() {
        memoryPool = null
    }

    /**
     * Executa até 4 consultas simultâneas em paralelo (async/awaitAll) com orçamento de 4500ms,
     * retendo lotes parciais já recebidos caso ocorra timeout antes da conclusão total.
     */
    suspend fun retrieveCandidates(
        taste: UserTasteProfile,
        searchEngine: MultiApiSearchEngine,
        ownerUid: String = ""
    ): List<ComicEntry> = withContext(Dispatchers.IO) {
        if (isCacheValid(taste.tasteHash, ownerUid)) {
            return@withContext getCachedPool(ownerUid)
        }

        val queries = mutableListOf<String>()

        // 1. Próxima edição das séries em leitura
        taste.readingSeries.firstOrNull()?.let { (series, issue) ->
            if (issue > 0) queries.add("$series #${issue + 1}")
            else queries.add(series)
        }

        // 2. Personagem favorito clássico
        taste.favoriteCharacters.firstOrNull()?.let { char ->
            val clean = char.substringBefore('(').trim()
            if (clean.isNotBlank()) queries.add(clean)
        }

        // 3. Autor mais aclamado na estante
        taste.topAuthors.firstOrNull()?.let { author ->
            if (author.isNotBlank() && author.length >= 4) queries.add(author)
        }

        // 4. HQ favorita ou Fila Quero Ler
        if (queries.size < 4) {
            taste.favoriteComics.firstOrNull()?.let { fav ->
                val title = PortugueseComicTranslator.translateTitleToPtBr(fav.displayTitle).take(40)
                if (title.isNotBlank()) queries.add(title)
            }
        }
        if (queries.size < 4) {
            taste.wantToReadSeries.firstOrNull()?.let { wtr ->
                if (wtr.isNotBlank()) queries.add(wtr)
            }
        }

        val limitedQueries = queries.distinct().take(4)
        if (limitedQueries.isEmpty()) {
            return@withContext emptyList()
        }

        val queryDeferreds = limitedQueries.map { q ->
            async(Dispatchers.IO) {
                var partialBatch = emptyList<ComicEntry>()
                withTimeoutOrNull(4500L) {
                    runCatching {
                        searchEngine.searchOnlineProgressive(q, searchLanguage = "ALL").collect { batch ->
                            if (batch.isNotEmpty()) {
                                partialBatch = batch
                            }
                        }
                    }
                }
                partialBatch
            }
        }

        val allResults = queryDeferreds.awaitAll().flatten()
        val deduplicated = CatalogSearchPolicy.deduplicateByCanonicalWork(allResults)
        updateCache(taste.tasteHash, deduplicated, ownerUid)
        deduplicated
    }
}
