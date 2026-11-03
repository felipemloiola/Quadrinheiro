package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.text.Normalizer

data class CharacterResult(val id: String, val name: String, val originalName: String, val image: String, val aliases: List<String>)

/** Public Superhero API, fetched once; local bilingual matching never downloads portraits in bulk. */
object CharacterDirectory {
    private val mutex = Mutex()
    @Volatile private var entries: List<CharacterResult>? = null
    internal val portuguese = mapOf(
        "Swamp Thing" to "Monstro do Pântano", "Clayface" to "Cara-de-Barro", "Green Lantern" to "Lanterna Verde",
        "Wonder Woman" to "Mulher-Maravilha", "Spider-Man" to "Homem-Aranha", "Daredevil" to "Demolidor",
        "Storm" to "Tempestade", "Black Panther" to "Pantera Negra", "Captain America" to "Capitão América",
        "Iron Man" to "Homem de Ferro", "Black Widow" to "Viúva Negra", "Scarlet Witch" to "Feiticeira Escarlate",
        "Doctor Strange" to "Doutor Estranho", "Doctor Fate" to "Senhor Destino", "Martian Manhunter" to "Caçador de Marte",
        "Green Arrow" to "Arqueiro Verde", "Hawkeye" to "Gavião Arqueiro", "Black Canary" to "Canário Negro",
        "The Flash" to "Flash", "Nightwing" to "Asa Noturna", "Red Hood" to "Capuz Vermelho", "Joker" to "Coringa",
        "Harley Quinn" to "Arlequina", "Catwoman" to "Mulher-Gato", "Poison Ivy" to "Hera Venenosa",
        "Two-Face" to "Duas-Caras", "Riddler" to "Charada", "Penguin" to "Pinguim", "Scarecrow" to "Espantalho",
        "Deadshot" to "Pistoleiro", "Deathstroke" to "Exterminador", "Silver Surfer" to "Surfista Prateado",
        "Human Torch" to "Tocha Humana", "Invisible Woman" to "Mulher Invisível", "Thing" to "Coisa",
        "Beast" to "Fera", "Cyclops" to "Ciclope", "Nightcrawler" to "Noturno", "Rogue" to "Vampira",
        "Colossus" to "Colossus", "Iceman" to "Homem de Gelo", "Luke Cage" to "Luke Cage", "Iron Fist" to "Punho de Ferro",
        "Sandman" to "Homem-Areia", "Black Bolt" to "Raio Negro", "Aquaman" to "Aquaman", "Supergirl" to "Supergirl")
    internal fun normalized(s: String) = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").replace(Regex("[^a-z0-9 ]"), " ").replace(Regex("\\s+"), " ").trim()
    internal fun parse(raw: String): List<CharacterResult> {
        val a = JSONArray(raw)
        return (0 until a.length()).mapNotNull { index ->
            val j = a.optJSONObject(index) ?: return@mapNotNull null
            val name = j.optString("name"); val image = j.optJSONObject("images")?.optString("md").orEmpty()
            if (name.isBlank() || !image.startsWith("https://")) return@mapNotNull null
            val bio = j.optJSONObject("biography"); val full = bio?.optString("fullName").orEmpty()
            val pt = portuguese[name] ?: name
            val display = if (full.isNotBlank() && !pt.contains(full, true)) "$pt ($full)" else pt
            val aliases = bio?.optJSONArray("aliases").let { arr -> (0 until (arr?.length() ?: 0)).map { arr!!.optString(it) } }
            CharacterResult(j.optString("id"), display.take(100), name, image, aliases + listOf(name, pt, full))
        }
    }
    internal fun match(list: List<CharacterResult>, query: String): List<CharacterResult> {
        val q = normalized(query)
        return list.filter { q.isBlank() || (it.aliases + it.name).any { text -> normalized(text).contains(q) } }
            .sortedWith(compareByDescending<CharacterResult> { normalized(it.originalName) == q || normalized(it.name) == q }
                .thenBy { it.name }).take(40)
    }
    suspend fun search(context: Context, query: String): List<CharacterResult> = withContext(Dispatchers.IO) {
        if (query.isNotBlank()) {
            try { ComicVineCatalog.characters(query).takeIf { it.isNotEmpty() }?.let { return@withContext it } }
            catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (_: Exception) { /* Public cached directory remains available offline. */ }
            try { ComicVineCatalog.superheroCharacters(query).takeIf { it.isNotEmpty() }?.let { return@withContext it } }
            catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (_: Exception) { /* Last fallback is the bounded public directory. */ }
        }
        val all = entries ?: mutex.withLock {
            entries ?: run {
                val file = File(context.cacheDir, "character_directory_v1.json")
                val cached = if (file.exists() && file.length() in 1..3_000_000L) runCatching { file.readText() }.getOrNull() else null
                val fresh = cached != null && System.currentTimeMillis() - file.lastModified() < 7 * 86400000L
                val raw = if (fresh) cached!! else try {
                    val client = MultiApiSearchEngine.sharedHttpClient.newBuilder().callTimeout(6, java.util.concurrent.TimeUnit.SECONDS).build()
                    client.newCall(Request.Builder().url("https://akabab.github.io/superhero-api/api/all.json").build()).execute().use { response ->
                        check(response.isSuccessful) { "Catálogo de personagens indisponível" }
                        val body = response.body ?: error("Resposta vazia")
                        require(body.contentLength() <= 3_000_000L)
                        body.string().also { require(it.length <= 3_000_000); parse(it); file.writeText(it) }
                    }
                } catch (e: Exception) { cached ?: throw e }
                parse(raw).also { entries = it }
            }
        }
        match(all, query)
    }
}
