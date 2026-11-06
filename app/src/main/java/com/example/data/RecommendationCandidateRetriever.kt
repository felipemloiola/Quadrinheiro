package com.example.data

import com.example.model.AppSettings
import com.example.model.ComicEntry
import com.example.model.ReadingStatus
import kotlinx.coroutines.Dispatchers
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

    @Volatile
    private var lastRunHash: String = ""

    @Volatile
    private var lastRunTime: Long = 0L

    @Volatile
    private var cachedRetrievedPool: List<ComicEntry> = emptyList()

    fun isCacheValid(currentHash: String): Boolean {
        return currentHash == lastRunHash && (System.currentTimeMillis() - lastRunTime) < TTL_MS && cachedRetrievedPool.isNotEmpty()
    }

    fun getCachedPool(): List<ComicEntry> = cachedRetrievedPool

    fun updateCache(hash: String, pool: List<ComicEntry>) {
        lastRunHash = hash
        lastRunTime = System.currentTimeMillis()
        cachedRetrievedPool = pool
    }

    /**
     * Gera até 4 consultas simultâneas seguras e não bloqueantes para alimentar o pool de candidatos.
     */
    suspend fun retrieveCandidates(
        taste: UserTasteProfile,
        searchEngine: MultiApiSearchEngine
    ): List<ComicEntry> = withContext(Dispatchers.IO) {
        if (isCacheValid(taste.tasteHash)) {
            return@withContext cachedRetrievedPool
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

        val results = mutableListOf<ComicEntry>()
        for (q in limitedQueries) {
            val candidatesForQuery = withTimeoutOrNull(4500L) {
                var items = emptyList<ComicEntry>()
                runCatching {
                    searchEngine.searchOnlineProgressive(q, searchLanguage = "ALL").collect { batch ->
                        if (batch.isNotEmpty()) items = batch
                    }
                }
                items
            }.orEmpty()
            results.addAll(candidatesForQuery)
        }

        val deduplicated = CatalogSearchPolicy.deduplicateByCanonicalWork(results)
        updateCache(taste.tasteHash, deduplicated)
        deduplicated
    }
}
