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
        "Hal Jordan" to "Lanterna Verde", "John Stewart" to "Lanterna Verde", "Kyle Rayner" to "Lanterna Verde", "Guy Gardner" to "Lanterna Verde",
        "Raven" to "Ravena", "Starfire" to "Estelar", "Beast Boy" to "Mutano",
        "Swamp Thing" to "Monstro do Pântano", "Clayface" to "Cara-de-Barro", "Green Lantern" to "Lanterna Verde",
        "Wonder Woman" to "Mulher-Maravilha", "Spider-Man" to "Homem-Aranha", "Daredevil" to "Demolidor",
        "Storm" to "Tempestade", "Black Panther" to "Pantera Negra", "Captain America" to "Capitão América",
        "Iron Man" to "Homem de Ferro", "Black Widow" to "Viúva Negra", "Scarlet Witch" to "Feiticeira Escarlate",
        "Doctor Strange" to "Doutor Estranho", "Doctor Fate" to "Senhor Destino", "Martian Manhunter" to "Caçador de Marte",
        "Green Arrow" to "Arqueiro Verde", "Hawkeye" to "Gavião Arqueiro", "Black Canary" to "Canário Negro",
        "The Flash" to "Flash", "Flash" to "Flash", "Nightwing" to "Asa Noturna", "Red Hood" to "Capuz Vermelho", "Red Robin" to "Robin Vermelho", "Joker" to "Coringa",
        "Harley Quinn" to "Arlequina", "Catwoman" to "Mulher-Gato", "Poison Ivy" to "Hera Venenosa",
        "Two-Face" to "Duas-Caras", "Riddler" to "Charada", "Penguin" to "Pinguim", "Scarecrow" to "Espantalho",
        "Deadshot" to "Pistoleiro", "Deathstroke" to "Exterminador", "Silver Surfer" to "Surfista Prateado",
        "Human Torch" to "Tocha Humana", "Invisible Woman" to "Mulher Invisível", "Thing" to "Coisa",
        "Beast" to "Fera", "Cyclops" to "Ciclope", "Nightcrawler" to "Noturno", "Rogue" to "Vampira",
        "Colossus" to "Colossus", "Iceman" to "Homem de Gelo", "Luke Cage" to "Luke Cage", "Iron Fist" to "Punho de Ferro",
        "Sandman" to "Sandman", "Dream" to "Sandman", "Morpheus" to "Sandman", "Sandman (Marvel)" to "Homem-Areia", "Flint Marko" to "Homem-Areia",
        "Punisher" to "Justiceiro", "Wolverine" to "Wolverine", "Deadpool" to "Deadpool", "Constantine" to "Constantine", "John Constantine" to "Constantine",
        "Black Bolt" to "Raio Negro", "Aquaman" to "Aquaman", "Supergirl" to "Supergirl", "Superboy" to "Superboy")
    internal fun normalized(s: String) = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").replace(Regex("[^a-z0-9 ]"), " ").replace(Regex("\\s+"), " ").trim()

    internal fun display(name: String, real: String, publisher: String = ""): String {
        val pt = when {
            name.equals("Sandman", true) && (publisher.contains("Marvel", true) || real.contains("Marko", true) || real.contains("Baker", true)) -> "Homem-Areia"
            name.equals("Sandman", true) -> "Sandman"
            else -> portuguese[name] ?: name
        }
        val cleanReal = real.trim()
        val civil = when {
            name.equals("Batman", true) && (cleanReal.contains("Bruce", true) || cleanReal.isBlank()) -> "Bruce Wayne"
            name.equals("Hal Jordan", true) -> "Hal Jordan"
            name.equals("John Stewart", true) -> "John Stewart"
            name.equals("Kyle Rayner", true) -> "Kyle Rayner"
            name.equals("Guy Gardner", true) -> "Guy Gardner"
            name.equals("The Flash", true) || name.equals("Flash", true) -> when {
                cleanReal.contains("Wally", true) -> "Wally West"
                cleanReal.contains("Barry", true) || cleanReal.isBlank() -> "Barry Allen"
                else -> cleanReal
            }
            name.equals("Barry Allen", true) -> "Barry Allen"
            name.equals("Wally West", true) -> "Wally West"
            name.equals("Spider-Man", true) || name.equals("Homem-Aranha", true) -> when {
                cleanReal.contains("Miles", true) -> "Miles Morales"
                cleanReal.contains("Peter", true) || cleanReal.isBlank() -> "Peter Parker"
                else -> cleanReal
            }
            name.equals("Peter Parker", true) -> "Peter Parker"
            name.equals("Miles Morales", true) -> "Miles Morales"
            name.equals("Daredevil", true) || name.equals("Demolidor", true) -> "Matt Murdock"
            name.equals("Swamp Thing", true) || name.equals("Monstro do Pântano", true) -> "Alec Holland"
            name.equals("Constantine", true) || name.equals("John Constantine", true) -> "John Constantine"
            name.equals("Punisher", true) || name.equals("Justiceiro", true) -> "Frank Castle"
            name.equals("Dick Grayson", true) -> "Dick Grayson"
            name.equals("Jason Todd", true) -> "Jason Todd"
            name.equals("Tim Drake", true) -> "Tim Drake"
            name.equals("Damian Wayne", true) -> "Damian Wayne"
            name.equals("Sandman", true) && pt == "Homem-Areia" -> if (cleanReal.isNotBlank()) cleanReal else "Flint Marko"
            name.equals("Sandman", true) && (cleanReal.contains("Morpheus", true) || cleanReal.contains("Dream", true) || cleanReal.contains("Sonho", true) || cleanReal.isBlank()) -> "Morpheus"
            cleanReal.isNotBlank() && !cleanReal.equals(pt, true) && !cleanReal.equals(name, true) -> cleanReal
            else -> ""
        }
        return if (civil.isNotBlank() && !pt.contains(civil, true)) "$pt ($civil)" else pt
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
            val pub = j.optString("publisher").ifBlank { bio?.optString("publisher").orEmpty() }
            CharacterResult(j.optString("id"),display(name,real,pub),name,image,aliases,pub)
        }
    }
    internal fun match(list: List<CharacterResult>, query: String): List<CharacterResult> {
        val q = normalized(query.substringBefore('('))
        fun popularity(c: CharacterResult): Int = when {
            c.originalName == "Batman" && c.name.contains("Bruce") -> 1000
            c.originalName == "Hal Jordan" -> 980
            c.originalName == "Superman" -> 960
            c.originalName == "Spider-Man" && c.name.contains("Peter") -> 950
            c.originalName in listOf("Wonder Woman", "Storm", "Swamp Thing", "Clayface", "Green Arrow", "Daredevil", "Wolverine", "The Flash", "Joker", "Harley Quinn", "Iron Man", "Captain America", "Deadpool", "Punisher", "Sandman") -> 900
            else -> 0
        }
        fun score(c: CharacterResult): Int {
            if (q.isBlank()) return 100 + popularity(c)
            val normName = normalized(c.name)
            val normBase = normalized(c.name.substringBefore('('))
            val normCivil = normalized(c.name.substringAfter('(', "").substringBefore(')'))
            val normOrig = normalized(c.originalName)
            val normPt = normalized(portuguese[c.originalName] ?: "")
            val primaryFields = listOf(normBase, normOrig, normPt, normCivil).filter { it.isNotBlank() }
            return when {
                primaryFields.any { it == q } || normName == q -> 2000
                primaryFields.any { it.startsWith(q) } -> 1500
                primaryFields.any { " $it ".contains(" $q ") } || " $normName ".contains(" $q ") -> 1200
                primaryFields.any { it.contains(q) } || normName.contains(q) -> 900
                q.length >= 4 && c.aliases.any { normalized(it) == q } -> 600
                else -> 0
            }
        }
        return list
            .filter { c ->
                val img = c.image.lowercase()
                img.startsWith("https://") && !img.contains("blank") && !img.contains("question_mark") && !img.contains("default")
            }
            .mapNotNull { c ->
                val s = score(c)
                if (s > 0) c to s else null
            }
            .distinctBy { normalized(it.first.originalName) + "|" + normalized(it.first.name) }
            .sortedWith(
                compareByDescending<Pair<CharacterResult, Int>> { it.second }
                    .thenByDescending { popularity(it.first) }
                    .thenBy { it.first.name }
            )
            .map { it.first }
            .take(if (q.isBlank()) 16 else 24)
    }

    internal fun searchLoaded(query: String): List<CharacterResult> = match(entries.orEmpty(), query)

    private var complete = false
    private fun local(context: Context): List<CharacterResult> {
        entries?.let { return it }
        val file = File(context.cacheDir, "character_directory_v2.json")
        val cached = if (file.exists() && file.length() < 2000000) runCatching { parse(file.readText()) }.getOrNull() else null
        complete = cached != null
        return (cached ?: context.assets.open("character-seed.json").bufferedReader().use { parse(it.readText()) }).also { entries = it }
    }
    suspend fun preload(context: Context) = withContext(Dispatchers.IO) {
        local(context)
        mutex.withLock {
            val file = File(context.cacheDir, "character_directory_v2.json")
            if (complete && System.currentTimeMillis() - file.lastModified() < 7 * 86400000L) return@withLock
            try {
                val client = MultiApiSearchEngine.sharedHttpClient.newBuilder().callTimeout(4, java.util.concurrent.TimeUnit.SECONDS).build()
                client.newCall(Request.Builder().url("https://quadrinheiro-catalog.uadrinheiro.workers.dev/characters.json").build()).execute().use { r ->
                    check(r.isSuccessful); val raw = r.body!!.string(); require(raw.length < 2000000)
                    val parsed = parse(raw); entries = parsed; complete = true; file.writeText(raw)
                }
            } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel } catch (_: Exception) { }
        }
    }
    suspend fun search(context: Context, query: String): List<CharacterResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        val immediate = match(local(context), trimmed)
        if (trimmed.isBlank()) return@withContext immediate
        if (!complete) preload(context)
        val indexed = match(entries.orEmpty(), trimmed)
        val remote = try {
            val cv = runCatching { ComicVineCatalog.characters(trimmed) }.getOrDefault(emptyList())
            val sh = runCatching { ComicVineCatalog.superheroCharacters(trimmed) }.getOrDefault(emptyList())
            match(cv + sh, trimmed)
        } catch (cancel: kotlinx.coroutines.CancellationException) {
            throw cancel
        } catch (_: Exception) {
            emptyList()
        }
        (immediate + indexed + remote).distinctBy { normalized(it.originalName) + "|" + normalized(it.name) }.take(24)
    }
}
