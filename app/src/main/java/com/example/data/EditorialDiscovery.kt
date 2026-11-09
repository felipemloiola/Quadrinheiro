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
    val selections = linkedMapOf(
        "Para começar" to listOf("Batman: Ano Um", "Watchmen", "Maus"),
        "Universo DC" to listOf("Batman: O Longo Dia das Bruxas", "Superman: As Quatro Estações"),
        "Universo Marvel" to listOf("Marvels", "Homem-Aranha: Azul", "Demolidor: A Queda de Murdock")
    )
    private val mutable = MutableStateFlow<Map<String, List<ComicEntry>>>(emptyMap())
    val shelves = mutable.asStateFlow()
    private val mutex = Mutex()
    private var attempted = false

    internal fun select(seed: String, items: List<ComicEntry>): ComicEntry? {
        val words = CatalogSearchPolicy.normalize(PortugueseComicTranslator.translateTitleToPtBr(seed))
            .split(' ').filter { it.length > 2 && it !in setOf("das", "dos", "the") }
        return CatalogSearchPolicy.deduplicateByCanonicalWork(items, "PT")
            .filter { c ->
                val title = CatalogSearchPolicy.normalize(PortugueseComicTranslator.translateTitleToPtBr(c.displayTitle))
                words.isNotEmpty() && words.all { title.contains(it) } &&
                    (words.size > 1 || title.startsWith(words.single()))
            }.maxByOrNull { c ->
                (if(PortugueseComicTranslator.canonicalWorkKey(c.displayTitle) == PortugueseComicTranslator.canonicalWorkKey(seed)) 1000 else 0) +
                    (if (CatalogSearchPolicy.isPortugueseEdition(c)) 100 else 0) +
                    (if(c.coverUrl.isNotBlank()) 10 else 0) + (if(c.pageCount > 0) 5 else 0)
            }
    }

    suspend fun load(context: Context, retry: Boolean = false) = mutex.withLock {
        if (attempted && !retry) return@withLock
        attempted = true
        val file = AtomicFile(File(context.filesDir, "discovery-editorial-v1.json"))
        if (mutable.value.isEmpty()) runCatching {
            check(file.baseFile.length() <= 192 * 1024)
            val bytes = file.openRead().use { it.readBytes() }
            check(bytes.size <= 192 * 1024)
            val json = JSONObject(String(bytes, Charsets.UTF_8))
            mutable.value = selections.keys.associateWith { title ->
                PersonalizedDiscovery.decodeCachedRecommendations(json.optString(title)).map { it.comic }
            }
        }
        val permits = Semaphore(2)
        val engine = MultiApiSearchEngine()
        for ((title, seeds) in selections) {
            if (mutable.value[title]?.isNotEmpty() == true) continue
            val entries = coroutineScope {
                seeds.map { seed -> async {
                    permits.withPermit {
                        var best: ComicEntry? = null
                        try {
                            withTimeoutOrNull(6500) {
                                engine.searchOnlineProgressive(seed, searchLanguage = "PT").collect { batch ->
                                    best = select(seed, listOfNotNull(best) + batch)
                                }
                            }
                        } catch (cancel: CancellationException) { throw cancel }
                        catch (_: Exception) { /* Other editorial selections remain usable. */ }
                        best
                    }
                } }.awaitAll().filterNotNull()
            }
            mutable.value = mutable.value + (title to CatalogSearchPolicy.deduplicateByCanonicalWork(entries, "PT"))
            if (entries.isNotEmpty()) runCatching {
                val json = JSONObject()
                mutable.value.forEach { (key, comics) ->
                    json.put(key, PersonalizedDiscovery.encodeDurableAccountRecommendations(comics.map {
                        RecommendedComic(it, "Seleção editorial", 1, "EDITORIAL")
                    }))
                }
                val output = file.startWrite()
                try { output.write(json.toString().toByteArray(Charsets.UTF_8)); file.finishWrite(output) }
                catch (error: Exception) { file.failWrite(output); throw error }
            }
        }
    }
}
