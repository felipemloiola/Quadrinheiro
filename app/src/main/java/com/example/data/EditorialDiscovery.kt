package com.example.data

import android.content.Context
import android.util.AtomicFile
import com.example.model.ComicEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.File

/** Editorial queries are independent from search/catalog pagination and personal taste. */
object EditorialDiscovery {
    private const val MAX_CACHE_BYTES = 384 * 1024 // 384 KB

    val selections = linkedMapOf(
        "Para começar" to listOf(
            "Batman: Ano Um",
            "Watchmen",
            "Maus",
            "Sandman: Prelúdios e Noturnos",
            "V de Vingança",
            "Persépolis",
            "Marvels",
            "Grandes Astros: Superman",
            "Demolidor: A Queda de Murdock",
            "Homem-Aranha: Azul",
            "Reino do Amanhã",
            "Daytripper"
        ),
        "Universo DC & Vertigo" to listOf(
            "Batman: O Longo Dia das Bruxas",
            "Superman: As Quatro Estações",
            "Batman: O Cavaleiro das Trevas",
            "Batman: A Piada Mortal",
            "Batman: A Corte das Corujas",
            "Asilo Arkham",
            "A Saga do Monstro do Pântano",
            "Sandman: Estação das Brumas",
            "Lanterna Verde: A Noite Mais Densa",
            "Ponto de Ignição",
            "Superman: Entre a Foice e o Martelo",
            "Mulher-Maravilha: Hiketeia",
            "Hellblazer: Hábitos Perigosos",
            "Y: O Último Homem",
            "Supergirl: Mulher do Amanhã",
            "Liga da Justiça: A Nova Fronteira"
        ),
        "Universo Marvel" to listOf(
            "Marvels",
            "Homem-Aranha: Azul",
            "Demolidor: A Queda de Murdock",
            "Homem-Aranha: A Última Caçada de Kraven",
            "Guerra Civil",
            "Capitão América: O Soldado Invernal",
            "Wolverine: Velho Logan",
            "Surpreendentes X-Men",
            "X-Men: A Saga da Fênix Negra",
            "Planeta Hulk",
            "Gavião Arqueiro: Minha Vida Como Uma Arma",
            "Thor: O Carniceiro dos Deuses",
            "Homem de Ferro: Extremis",
            "Demolidor: O Homem Sem Medo",
            "Homem-Aranha: História de Vida",
            "Visão"
        ),
        "Mangás Essenciais" to listOf(
            "Berserk",
            "Akira",
            "Vagabond",
            "Monster",
            "Vinland Saga",
            "Uzumaki",
            "Pluto",
            "Fullmetal Alchemist",
            "Slam Dunk",
            "20th Century Boys",
            "Boa Noite Punpun",
            "Death Note",
            "One Piece",
            "Nausicaä do Vale do Vento"
        ),
        "Independentes & Autorais" to listOf(
            "Saga",
            "Invencível",
            "Hellboy: Sementes da Destruição",
            "Sin City: A Cidade do Pecado",
            "Blacksad",
            "The Walking Dead",
            "Paper Girls",
            "Scott Pilgrim Contra o Mundo",
            "Criminal",
            "Retalhos",
            "Fun Home",
            " Asterios Polyp".trim()
        ),
        "Quadrinhos Nacionais" to listOf(
            "Daytripper",
            "Turma da Mônica: Laços",
            "Turma da Mônica: Lições",
            "Astronauta: Magnetar",
            "Jeremias: Pele",
            "Angola Janga",
            "Cumbe",
            "Tungstênio",
            "Bidu: Caminhos",
            "Piteco: Ingá",
            "Dois Irmãos",
            "Arlindo"
        )
    )
    private val mutable = MutableStateFlow<Map<String, List<ComicEntry>>>(emptyMap())
    val shelves = mutable.asStateFlow()
    private val mutex = Mutex()
    private var attempted = false

    private val originalTitles = mapOf(
        "Batman: O Longo Dia das Bruxas" to "Batman The Long Halloween",
        "Superman: As Quatro Estações" to "Superman For All Seasons",
        "Batman: Ano Um" to "Batman Year One",
        "Batman: O Cavaleiro das Trevas" to "Batman The Dark Knight Returns",
        "Batman: A Piada Mortal" to "Batman The Killing Joke",
        "Batman: A Corte das Corujas" to "Batman The Court of Owls",
        "Asilo Arkham" to "Arkham Asylum",
        "A Saga do Monstro do Pântano" to "Saga of the Swamp Thing",
        "Sandman: Prelúdios e Noturnos" to "Sandman Preludes and Nocturnes",
        "Sandman: Estação das Brumas" to "Sandman Season of Mists",
        "Lanterna Verde: A Noite Mais Densa" to "Green Lantern Blackest Night",
        "Ponto de Ignição" to "Flashpoint",
        "Superman: Entre a Foice e o Martelo" to "Superman Red Son",
        "Grandes Astros: Superman" to "All-Star Superman",
        "Mulher-Maravilha: Hiketeia" to "Wonder Woman The Hiketeia",
        "Hellblazer: Hábitos Perigosos" to "Hellblazer Dangerous Habits",
        "Y: O Último Homem" to "Y The Last Man",
        "Supergirl: Mulher do Amanhã" to "Supergirl Woman of Tomorrow",
        "Liga da Justiça: A Nova Fronteira" to "DC The New Frontier",
        "Reino do Amanhã" to "Kingdom Come",
        "Homem-Aranha: Azul" to "Spider-Man Blue",
        "Homem-Aranha: A Última Caçada de Kraven" to "Spider-Man Kraven's Last Hunt",
        "Homem-Aranha: História de Vida" to "Spider-Man Life Story",
        "Demolidor: A Queda de Murdock" to "Daredevil Born Again",
        "Demolidor: O Homem Sem Medo" to "Daredevil The Man Without Fear",
        "Guerra Civil" to "Civil War",
        "Capitão América: O Soldado Invernal" to "Captain America Winter Soldier",
        "Wolverine: Velho Logan" to "Wolverine Old Man Logan",
        "Surpreendentes X-Men" to "Astonishing X-Men",
        "X-Men: A Saga da Fênix Negra" to "X-Men The Dark Phoenix Saga",
        "Planeta Hulk" to "Planet Hulk",
        "Gavião Arqueiro: Minha Vida Como Uma Arma" to "Hawkeye My Life as a Weapon",
        "Thor: O Carniceiro dos Deuses" to "Thor God Butcher",
        "Homem de Ferro: Extremis" to "Iron Man Extremis",
        "Visão" to "The Vision Tom King",
        "V de Vingança" to "V for Vendetta",
        "Boa Noite Punpun" to "Goodnight Punpun",
        "Nausicaä do Vale do Vento" to "Nausicaa of the Valley of the Wind",
        "Invencível" to "Invincible",
        "Hellboy: Sementes da Destruição" to "Hellboy Seed of Destruction",
        "Sin City: A Cidade do Pecado" to "Sin City The Hard Goodbye",
        "Retalhos" to "Blankets Craig Thompson"
    )

    private val paginationQueries = mapOf(
        "Para começar" to listOf("Graphic Novel Clássica", "Batman", "Watchmen"),
        "Universo DC & Vertigo" to listOf("DC Comics", "Vertigo", "Liga da Justiça", "Superman"),
        "Universo Marvel" to listOf("Marvel Comics", "X-Men", "Vingadores", "Demolidor"),
        "Mangás Essenciais" to listOf("Seinen Manga", "Naoki Urasawa", "Takehiko Inoue", "Osamu Tezuka"),
        "Independentes & Autorais" to listOf("Image Comics", "Dark Horse", "Ed Brubaker", "Jeff Lemire"),
        "Quadrinhos Nacionais" to listOf("MSP Graphic", "Fábio Moon Gabriel Bá", "Marcelo D'Salete", "Quadrinhos na Cia")
    )

    internal fun select(seed: String, items: List<ComicEntry>): ComicEntry? {
        val words = CatalogSearchPolicy.normalize(PortugueseComicTranslator.translateTitleToPtBr(seed))
            .split(' ').filter { it.length > 2 && it !in setOf("das", "dos", "the", "uma", "como") }
        return CatalogSearchPolicy.deduplicateByCanonicalWork(items, "PT")
            .filter { c ->
                val title = CatalogSearchPolicy.normalize(PortugueseComicTranslator.translateTitleToPtBr(c.displayTitle))
                (words.isNotEmpty() && words.all { title.contains(it) } &&
                    (words.size > 1 || title.startsWith(words.single()))) ||
                    originalTitles[seed]?.let { original ->
                        val raw = CatalogSearchPolicy.normalize(c.title)
                        CatalogSearchPolicy.normalize(original).split(' ').filter { it.length > 2 && it !in setOf("the", "for", "and") }.all(raw::contains)
                    } == true
            }.maxByOrNull { c ->
                (if (PortugueseComicTranslator.canonicalWorkKey(c.displayTitle) == PortugueseComicTranslator.canonicalWorkKey(seed)) 1000 else 0) +
                    (if (CatalogSearchPolicy.isPortugueseEdition(c)) 100 else 0) +
                    (if (c.coverUrl.isNotBlank()) 10 else 0) + (if (c.pageCount > 0) 5 else 0)
            }
    }

    private fun persistCache(file: AtomicFile) {
        runCatching {
            val json = JSONObject()
            mutable.value.forEach { (key, comics) ->
                json.put(
                    key,
                    PersonalizedDiscovery.encodeRecommendationsWithByteBudget(
                        comics.map { RecommendedComic(it, "Seleção editorial", 1, "EDITORIAL") },
                        maxItems = 24,
                        maxBytesUtf8 = 56 * 1024
                    )
                )
            }
            val payload = json.toString().toByteArray(Charsets.UTF_8)
            if (payload.size <= MAX_CACHE_BYTES) {
                val output = file.startWrite()
                try { output.write(payload); file.finishWrite(output) }
                catch (error: Exception) { file.failWrite(output); throw error }
            }
        }
    }

    suspend fun load(context: Context, retryShelf: String? = null) = mutex.withLock {
        if (attempted && retryShelf == null) return@withLock
        runCatching { File(context.filesDir, "discovery-editorial-v2.json").delete() }
        val file = AtomicFile(File(context.filesDir, "discovery-editorial-v3.json"))
        if (mutable.value.isEmpty()) runCatching {
            check(file.baseFile.length() <= MAX_CACHE_BYTES)
            val bytes = file.openRead().use { it.readBytes() }
            check(bytes.size <= MAX_CACHE_BYTES)
            val json = JSONObject(String(bytes, Charsets.UTF_8))
            mutable.value = selections.keys.associateWith { title ->
                PersonalizedDiscovery.decodeCachedRecommendations(json.optString(title)).map { it.comic }
            }
        }
        val permits = Semaphore(3)
        val engine = MultiApiSearchEngine()
        for ((title, seeds) in selections) {
            if (retryShelf != null && title != retryShelf) continue
            val minSatisfied = minOf(10, (seeds.size * 3) / 4)
            if (retryShelf == null && (mutable.value[title]?.size ?: 0) >= minSatisfied) continue
            val existingKeys = mutable.value[title].orEmpty()
                .map { PortugueseComicTranslator.canonicalWorkKey(it.displayTitle) }
                .toHashSet()
            val pendingSeeds = seeds.filter { seed ->
                PortugueseComicTranslator.canonicalWorkKey(seed) !in existingKeys
            }
            for (wave in pendingSeeds.chunked(6)) {
                val entries = coroutineScope {
                    wave.map { seed -> async {
                        permits.withPermit {
                            var best: ComicEntry? = null
                            try {
                                withTimeoutOrNull(9500) {
                                    engine.searchOnlineProgressive(
                                        originalTitles[seed] ?: seed,
                                        searchLanguage = "PT",
                                        provider = "MULTI"
                                    ).collect { batch ->
                                        best = select(seed, listOfNotNull(best) + batch)
                                    }
                                }
                            } catch (cancel: CancellationException) { throw cancel }
                            catch (_: Exception) { /* Other editorial selections remain usable. */ }
                            best
                        }
                    } }.awaitAll().filterNotNull()
                }
                if (entries.isNotEmpty()) {
                    val merged = CatalogSearchPolicy.deduplicateByCanonicalWork(
                        mutable.value[title].orEmpty() + entries,
                        "PT"
                    )
                    mutable.value = mutable.value + (title to merged)
                }
            }
            if (!mutable.value[title].isNullOrEmpty()) {
                persistCache(file)
            }
        }
        if (retryShelf == null) attempted = true
    }

    /** Carrega página complementar sob demanda quando o leitor expande uma coleção editorial. */
    suspend fun loadMoreForShelf(context: Context, shelfTitle: String, page: Int): Boolean {
        val queries = paginationQueries[shelfTitle] ?: return false
        val term = queries[page.coerceAtLeast(0) % queries.size]
        val engine = MultiApiSearchEngine()
        var fetched = emptyList<ComicEntry>()
        try {
            withTimeoutOrNull(8000) {
                engine.searchOnlineProgressive(term, searchLanguage = "PT", provider = "MULTI").collect { batch ->
                    if (batch.isNotEmpty()) fetched = batch
                }
            }
            if (page > 0) {
                val extra = runCatching {
                    withTimeoutOrNull(5000) {
                        PortugueseBookSources.googlePortuguese(term, page = page).map(AdditionalCatalogSources::localize)
                    }
                }.getOrNull().orEmpty().filter(PortugueseSearchPolicy::accepts)
                fetched = fetched + extra
            }
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { }

        if (fetched.isEmpty()) return false
        return mutex.withLock {
            val current = mutable.value[shelfTitle].orEmpty()
            val merged = CatalogSearchPolicy.deduplicateByCanonicalWork(
                current + fetched.filter { it.coverUrl.isNotBlank() },
                "PT"
            ).take(48)
            val added = merged.size > current.size
            if (added) {
                mutable.value = mutable.value + (shelfTitle to merged)
                persistCache(AtomicFile(File(context.filesDir, "discovery-editorial-v3.json")))
            }
            added
        }
    }
}
