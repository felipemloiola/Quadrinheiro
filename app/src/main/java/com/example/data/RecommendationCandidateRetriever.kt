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
    val signalSource: String,
    val explanation: RecommendationReason? = null
)

data class RecommendationReason(val kind: String, val reference: String, val detail: String)

/**
 * Perfil de gosto do usuário extraído de toda a biblioteca existente e dos favoritos.
 * Preserva 100% dos dados prévios sem jamais resetar o acervo.
 */
data class UserTasteProfile(
    val readingSeries: List<Pair<String, Int>>, // baseSeriesTitle to currentMaxIssue
    val readSeries: List<Pair<String, Int>>,
    val topAuthors: List<String>, // Autores >= 3.5★ ou com re-leituras
    val favoriteCharacters: List<String>,
    val favoriteComics: List<ComicEntry>,
    val wantToReadSeries: List<String>,
    val favoriteUniverse: String,
    val tasteHash: String,
    val moreLike: List<ComicEntry> = emptyList(),
    val dislikedSeries: Set<String> = emptySet(),
    val dislikedAuthors: Set<String> = emptySet()
)

object RecommendationCandidateRetriever {
    private const val TTL_MS = 12 * 60 * 60 * 1000L // 12 horas

    internal val characterPicks: Map<String, List<String>> = mapOf(
        "Batman" to listOf("Batman Silêncio", "Batman O Cavaleiro das Trevas", "Batman A Corte das Corujas"),
        "Superman" to listOf("Grandes Astros Superman", "Superman As Quatro Estações", "Superman Entre a Foice e o Martelo"),
        "Homem-Aranha" to listOf("Homem-Aranha Azul", "Homem-Aranha A Última Caçada de Kraven", "Homem-Aranha História de Vida"),
        "Lanterna Verde" to listOf("Lanterna Verde Renascimento", "Lanterna Verde A Guerra dos Anéis", "Lanterna Verde A Noite Mais Densa"),
        "Hal Jordan" to listOf("Lanterna Verde Renascimento", "Lanterna Verde A Guerra dos Anéis"),
        "Mulher-Maravilha" to listOf("Mulher-Maravilha Hiketeia", "Mulher-Maravilha Ano Um", "Mulher-Maravilha Terra Morta"),
        "Demolidor" to listOf("Demolidor A Queda de Murdock", "Demolidor O Homem Sem Medo", "Demolidor Amarelo"),
        "X-Men" to listOf("Surpreendentes X-Men", "X-Men A Saga da Fênix Negra", "X-Men Dias de um Futuro Esquecido"),
        "Wolverine" to listOf("Wolverine Velho Logan", "Wolverine Arma X", "Wolverine Inimigo do Estado"),
        "Flash" to listOf("Flash Ponto de Ignição", "Flash Renascimento", "Flash Ano Um"),
        "Asa Noturna" to listOf("Asa Noturna Saltando à Luz", "Asa Noturna Ano Um"),
        "Robin" to listOf("Robin Ano Um", "Batman e Robin"),
        "Capuz Vermelho" to listOf("Batman Sob o Capuz Vermelho", "Capuz Vermelho"),
        "Batgirl" to listOf("Batgirl Ano Um", "Batman A Piada Mortal"),
        "Liga da Justiça" to listOf("Liga da Justiça Reino do Amanhã", "Liga da Justiça Torre de Babel", "Liga da Justiça Origem"),
        "Vingadores" to listOf("Vingadores Guerra Civil", "Novos Vingadores Motim", "Vingadores Dinastia Kang"),
        "Capitão América" to listOf("Capitão América O Soldado Invernal", "Capitão América Branco"),
        "Homem de Ferro" to listOf("Homem de Ferro Extremis", "Homem de Ferro O Demônio na Garrafa"),
        "Thor" to listOf("Thor O Carniceiro dos Deuses", "Thor Renascer dos Deuses"),
        "Hulk" to listOf("Planeta Hulk", "O Imortal Hulk", "Hulk Cinza"),
        "Quarteto Fantástico" to listOf("Quarteto Fantástico 1234", "Quarteto Fantástico Imaginautas"),
        "Gavião Arqueiro" to listOf("Gavião Arqueiro Minha Vida Como Uma Arma", "Gavião Arqueiro Pequenos Acertos"),
        "Justiceiro" to listOf("Justiceiro Bem-Vindo de Volta Frank", "Justiceiro MAX"),
        "Deadpool" to listOf("Deadpool Clássico", "Homem-Aranha e Deadpool"),
        "Doutor Estranho" to listOf("Doutor Estranho O Juramento", "Doutor Estranho Triunfo e Tormento"),
        "Cavaleiro da Lua" to listOf("Cavaleiro da Lua Lemire", "Cavaleiro da Lua Do Fundo dos Mortos"),
        "Viúva Negra" to listOf("Viúva Negra Waid Samnee", "Viúva Negra O Nome da Rosa"),
        "Pantera Negra" to listOf("Pantera Negra Uma Nação Sob Nossos Pés", "Pantera Negra Quem é o Pantera Negra"),
        "Sandman" to listOf("Sandman Prelúdios e Noturnos", "Sandman Estação das Brumas"),
        "Monstro do Pântano" to listOf("A Saga do Monstro do Pântano", "Monstro do Pântano Raízes"),
        "John Constantine" to listOf("Hellblazer Hábitos Perigosos", "Hellblazer Pecados Originais"),
        "Constantine" to listOf("Hellblazer Hábitos Perigosos", "Hellblazer Pecados Originais"),
        "Arqueiro Verde" to listOf("Arqueiro Verde Ano Um", "Arqueiro Verde O Espírito da Flecha"),
        "Aquaman" to listOf("Aquaman As Profundezas", "Aquaman O Trono de Atlântida"),
        "Supergirl" to listOf("Supergirl Mulher do Amanhã", "Supergirl Sendo Super"),
        "Hellboy" to listOf("Hellboy Sementes da Destruição", "Hellboy O Despertar do Demônio"),
        "Invencível" to listOf("Invencível Assuntos de Família", "Invencível Oito é Demais"),
        "Spawn" to listOf("Spawn Origens", "Spawn Compêndio")
    )

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
     * Até 12 sinais, com duas consultas simultâneas e 4500ms por consulta.
     * Retém lotes parciais; a seleção persistida evita repetir a busca ao navegar.
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
        queries += taste.moreLike.take(3).map { it.authors.substringBefore(',').ifBlank { it.displayTitle }.take(80) }
        taste.readingSeries.take(3).forEach { (series, issue) ->
            queries += if (issue > 0) "$series #${issue + 1}" else series
        }
        taste.readSeries.take(3).forEach { (series, issue) ->
            queries += if (issue > 0) "$series #${issue + 1}" else series
        }
        queries += taste.favoriteCharacters.take(4).flatMap { character ->
            val clean = character.substringBefore('(').trim()
            characterPicks[clean] ?: listOf(clean)
        }.take(8)
        queries += taste.favoriteComics.take(4).map { it.displayTitle.take(80) }
        queries += taste.topAuthors.take(4)
        queries += taste.wantToReadSeries.take(3)
        queries += taste.favoriteUniverse
        val limitedQueries = queries.map(String::trim).filter(String::isNotBlank).distinct().take(16)
        if (limitedQueries.isEmpty()) {
            return@withContext emptyList()
        }

        val prefersManga = taste.favoriteUniverse.contains("Mang", true) ||
            taste.favoriteComics.any { it.genre.contains("Mang", true) }
        val primaryProvider = if (prefersManga) "MULTI" else "BOOKS"

        val permits = kotlinx.coroutines.sync.Semaphore(3)
        val queryDeferreds = limitedQueries.map { q ->
            async(Dispatchers.IO) {
                permits.acquire()
                try {
                    var partialBatch = emptyList<ComicEntry>()
                    withTimeoutOrNull(5000L) {
                        runCatching {
                            searchEngine.searchOnlineProgressive(
                                q,
                                searchLanguage = "PT",
                                provider = primaryProvider
                            ).collect { batch ->
                                if (batch.isNotEmpty()) {
                                    partialBatch = batch
                                }
                            }
                        }
                    }
                    if (partialBatch.isEmpty()) {
                        withTimeoutOrNull(4000L) {
                            runCatching {
                                searchEngine.searchOnlineProgressive(
                                    q,
                                    searchLanguage = "PT",
                                    provider = "VERIFIED_COMICS"
                                ).collect { batch ->
                                    if (batch.isNotEmpty()) partialBatch = batch
                                }
                            }
                        }
                    }
                    partialBatch
                } finally { permits.release() }
            }
        }

        val allResults = queryDeferreds.awaitAll().flatten()
        val deduplicated = CatalogSearchPolicy.deduplicateByCanonicalWork(allResults, "PT")
        updateCache(taste.tasteHash, deduplicated, ownerUid)
        deduplicated
    }
}
