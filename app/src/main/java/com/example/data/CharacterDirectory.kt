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

data class CharacterResult(val id: String, val name: String, val originalName: String, val image: String, val aliases: List<String>, val publisher: String = "")

/** Public Superhero API, fetched once; local bilingual matching never downloads portraits in bulk. */
object CharacterDirectory {
    private val mutex = Mutex()
    @Volatile private var entries: List<CharacterResult>? = null
    internal val portuguese = mapOf(
        "Hal Jordan" to "Lanterna Verde", "John Stewart" to "Lanterna Verde", "Raven" to "Ravena", "Starfire" to "Estelar", "Beast Boy" to "Mutano",
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

    internal fun display(name: String, real: String): String {
        val pt = portuguese[name] ?: name
        val civil = when { name == "Batman" && real.contains("Bruce", true) -> "Bruce Wayne"; name == "Hal Jordan" -> "Hal Jordan"; else -> real }
        return if (civil.isNotBlank() && civil != pt) "$pt ($civil)" else pt
    }
    internal fun parse(raw: String): List<CharacterResult> {
        val array = if(raw.trimStart().startsWith("{")) org.json.JSONObject(raw).getJSONArray("items") else JSONArray(raw)
        return (0 until array.length()).mapNotNull { index ->
            val j=array.optJSONObject(index) ?: return@mapNotNull null
            val name=j.optString("name");val bio=j.optJSONObject("biography");val real=j.optString("realName").ifBlank { bio?.optString("fullName").orEmpty() }
            val image=j.optString("image").ifBlank { j.optJSONObject("images")?.optString("md").orEmpty() }
            if(name.isBlank() || !image.startsWith("https://")) return@mapNotNull null
            val a=j.optJSONArray("aliases") ?: bio?.optJSONArray("aliases")
            val aliases=(0 until (a?.length() ?: 0)).map { a!!.optString(it) } + listOf(name, portuguese[name] ?: name, real)
            CharacterResult(j.optString("id"),display(name,real),name,image,aliases,j.optString("publisher").ifBlank { bio?.optString("publisher").orEmpty() })
        }
    }
    internal fun match(list: List<CharacterResult>, query: String): List<CharacterResult> {
        val q=normalized(query.substringBefore('('))
        fun popularity(c:CharacterResult):Int = when {
            c.originalName=="Batman" && c.name.contains("Bruce") -> 1000
            c.originalName=="Hal Jordan" -> 980
            c.originalName=="Superman" -> 960
            c.originalName=="Spider-Man" && c.name.contains("Peter") -> 950
            c.originalName in listOf("Wonder Woman","Storm","Swamp Thing","Clayface","Green Arrow","Daredevil") -> 900
            else -> 0
        }
        return list.filter { q.isBlank() || (it.aliases+it.name).any { normalized(it).contains(q) } }
            .distinctBy { normalized(it.originalName)+normalized(it.name) }
            .sortedWith(compareByDescending<CharacterResult> { if(normalized(it.originalName)==q || normalized(portuguese[it.originalName] ?: "")==q) 2000 else 0 }.thenByDescending(::popularity).thenBy { it.name }).take(if(q.isBlank()) 16 else 32)
    }
    private var complete=false
    private fun local(context:Context):List<CharacterResult> {
        entries?.let { return it }
        val file=File(context.cacheDir,"character_directory_v2.json")
        val cached=if(file.exists() && file.length() < 2000000) runCatching { parse(file.readText()) }.getOrNull() else null
        complete=cached!=null
        return (cached ?: context.assets.open("character-seed.json").bufferedReader().use { parse(it.readText()) }).also { entries=it }
    }
    suspend fun preload(context:Context) = withContext(Dispatchers.IO) {
        local(context)
        mutex.withLock {
            val file=File(context.cacheDir,"character_directory_v2.json")
            if(complete && System.currentTimeMillis()-file.lastModified()<7*86400000L) return@withLock
            try {
                val client=MultiApiSearchEngine.sharedHttpClient.newBuilder().callTimeout(4,java.util.concurrent.TimeUnit.SECONDS).build()
                client.newCall(Request.Builder().url("https://quadrinheiro-catalog.uadrinheiro.workers.dev/characters.json").build()).execute().use { r ->
                    check(r.isSuccessful);val raw=r.body!!.string();require(raw.length<2000000)
                    val parsed=parse(raw);entries=parsed;complete=true;file.writeText(raw)
                }
            } catch(cancel:kotlinx.coroutines.CancellationException){throw cancel} catch(_:Exception) { }
        }
    }
    suspend fun search(context: Context, query: String): List<CharacterResult> = withContext(Dispatchers.IO) {
        val immediate=match(local(context),query)
        if(immediate.isNotEmpty()) return@withContext immediate
        if(!complete) preload(context)
        val indexed=match(entries.orEmpty(),query)
        if(indexed.isNotEmpty()) return@withContext indexed
        try { ComicVineCatalog.characters(query).take(24) } catch(cancel:kotlinx.coroutines.CancellationException){throw cancel} catch(_:Exception){emptyList()}
    }
}
