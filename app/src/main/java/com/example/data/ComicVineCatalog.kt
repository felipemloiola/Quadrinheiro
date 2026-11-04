package com.example.data

import com.example.model.ComicEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

data class CatalogCredit(val id: String, val name: String, val role: String = "")
data class CatalogDetails(val id: String, val type: String, val title: String, val image: String, val banner: String,
    val description: String, val publisher: String, val date: String, val url: String, val covers: List<String>,
    val characters: List<CatalogCredit>, val creators: List<CatalogCredit>, val issues: List<ComicEntry>,
    val realName: String = "", val aliases: String = "", val powers: List<String> = emptyList(), val appearances: Int = 0,
    val creditsScope: String = "issue",val pageCount:Int=0,val isbn:String="",val format:String="")

/** The app only talks to our bounded catalog service. Provider credentials never enter the APK. */
object ComicVineCatalog {
    private const val ENDPOINT = "https://quadrinheiro-catalog.uadrinheiro.workers.dev"
    private val client by lazy { MultiApiSearchEngine.sharedHttpClient.newBuilder().callTimeout(25, TimeUnit.SECONDS).build() }
    private val memory = object : LinkedHashMap<String, CatalogDetails>(24, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CatalogDetails>?) = size > 24
    }
    private suspend fun request(op: String, q: String = "", id: String = "", page: Int = 0, kind: String = "issue"): JSONObject = withContext(Dispatchers.IO) {
        val url = ENDPOINT.toHttpUrl().newBuilder().addQueryParameter("op", op).addQueryParameter("q", q.take(100))
            .addQueryParameter("id", id).addQueryParameter("page", page.toString()).addQueryParameter("kind",kind).addQueryParameter("v","52").build()
        val body = suspendCancellableCoroutine<String> { continuation ->
            val call = client.newCall(Request.Builder().url(url).get().build())
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) { if (continuation.isActive) continuation.resumeWithException(e) }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        response.use {
                            check(it.isSuccessful) { "Catálogo temporariamente indisponível" }
                            val raw = it.body?.string() ?: error("Resposta vazia")
                            check(raw.length <= 800000)
                            if (continuation.isActive) continuation.resume(raw)
                        }
                    } catch (e: Exception) { if (continuation.isActive) continuation.resumeWithException(e) }
                }
            })
        }
        JSONObject(body)
    }
    internal fun comic(j: JSONObject): ComicEntry {
        val id = j.optString("id")
        val volume = j.optString("volume").ifBlank { j.optString("name") }
        val issue = j.optString("issue").toIntOrNull() ?: 0
        val standalone = issue <= 1 && Regex("year one|ano um|killing joke|piada mortal|long halloween|longo dia das bruxas|dark victory|vitória sombria|arkham asylum|asilo arkham", RegexOption.IGNORE_CASE).containsMatchIn(volume)
        val name = j.optString("name")
        val title = if (volume.isNotBlank() && name.isNotBlank() && !volume.equals(name, true)) "$volume: $name" else volume.ifBlank { name.ifBlank { "HQ" } }
        val volumeEntry=j.optString("type")=="volume"
        val key=if(j.optString("type")=="metron-issue") "mt-$id" else if(volumeEntry) "cv-volume-$id" else "cv-$id"
        return ComicEntry(id = key, externalId = key, title = if(j.optString("type")=="metron-issue" && issue>0 && j.optString("format")=="Edição") "$title #$issue" else title, authors = "", publisher = j.optString("publisher"),
            coverUrl = j.optString("image"), releaseYear = j.optString("date").take(4).toIntOrNull() ?: 2026,
            synopsis = plain(j.optString("description")), sourceLabel = if(key.startsWith("mt-")) "Metron" else "Comic Vine", isOnlineResult=true, seriesName = if (standalone) "" else volume, issueNumber = if (standalone) 0 else issue,
            format = j.optString("format").ifBlank {if (standalone) "Graphic Novel" else if(volumeEntry) "Coleção" else "Edição"}, pageCount = j.optInt("pageCount",0),isbn=j.optString("isbn"))
    }
    internal fun plain(html: String): String = android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
    suspend fun search(query: String, page: Int = 0): MultiApiSearchEngine.CatalogPage {
        val j = request("search", query, page = page)
        return MultiApiSearchEngine.CatalogPage(j.objects("items").map(::comic), j.optBoolean("hasMore"))
    }
    suspend fun catalog(page: Int): MultiApiSearchEngine.CatalogPage {
        val j = request("catalog", page = page)
        return MultiApiSearchEngine.CatalogPage(j.objects("items").map(::comic), j.optBoolean("hasMore"))
    }
    suspend fun characters(query: String): List<CharacterResult> {
        val normalized = CharacterDirectory.normalized(query)
        val english = CharacterDirectory.portuguese.entries.firstOrNull { CharacterDirectory.normalized(it.value) == normalized }?.key ?: query
        val j = request("characters", english.ifBlank { "Batman" })
        return j.objects("items").map { c -> val name = c.optString("name"); val pt = CharacterDirectory.portuguese[name] ?: name
            val real = c.optString("realName")
            CharacterResult("cv-"+c.optString("id"), if (real.isNotBlank() && !pt.contains(real, true)) "$pt ($real)" else pt,
                name, c.optString("image"), listOf(name, pt, real)) }
    }
    suspend fun superheroCharacters(query: String): List<CharacterResult> = request("superhero", query).objects("items").map { c ->
        val name = c.optString("name"); val pt = CharacterDirectory.portuguese[name] ?: name; val real = c.optString("realName")
        CharacterResult("sh-"+c.optString("id"), if (real.isNotBlank() && !pt.contains(real, true)) "$pt ($real)" else pt,
            name, c.optString("image"), listOf(name, pt, real))
    }
    suspend fun details(type: String, id: String): CatalogDetails {
        val operation=if(id.startsWith("mt-")) "metron-$type" else type
        val resourceId=id.removePrefix("mt-")
        val key = "$operation:$resourceId"
        synchronized(memory) { memory[key] }?.let { return it }
        val j = request(operation, id = resourceId)
        val publisher = j.optString("publisher").ifBlank {
            if (type == "issue" && j.optString("volumeId").isNotBlank()) runCatching { request("volume", id = j.optString("volumeId")).optString("publisher") }.getOrDefault("") else ""
        }
        fun credits(key: String) = j.objects(key).map { CatalogCredit(it.optString("id"), it.optString("name"), it.optString("role")) }
        fun strings(key: String): List<String> = j.optJSONArray(key)?.let { a -> (0 until a.length()).map { a.optString(it) } }.orEmpty()
        val combinedCovers = (strings("covers") + strings("portraits") + strings("artwork")).filter { it.isNotBlank() }.distinct().take(12)
        return CatalogDetails(resourceId, operation, j.optString("volume").ifBlank { j.optString("name") }, j.optString("image"), j.optString("banner"),
            plain(j.optString("description")), publisher, j.optString("date"), j.optString("url"), combinedCovers,
            credits("characters"), credits("creators"), j.objects("issues").map(::comic), j.optString("realName"), j.optString("aliases"), strings("powers"), j.optInt("appearances"),j.optString("creditsScope","issue"),j.optInt("pageCount",0),j.optString("isbn"),j.optString("format"))
            .also { synchronized(memory) { memory[key] = it } }
    }
    suspend fun resolve(comic: ComicEntry): CatalogDetails? {
        val key=comic.externalId.ifBlank { comic.id }
        if(key.startsWith("mt-"))return details("metron-issue",key.removePrefix("mt-"))
        if(key.startsWith("cv-volume-"))return details("volume",key.removePrefix("cv-volume-"))
        val id = comic.externalId.removePrefix("cv-").takeIf { comic.externalId.startsWith("cv-") }
            ?: comic.id.removePrefix("cv-").takeIf { comic.id.startsWith("cv-") }
        // Never attach credits from a guessed edition to a locally cataloged book.
        return id?.takeIf { it.all(Char::isDigit) }?.let { details("issue", it) }
    }
    suspend fun translated(details: CatalogDetails): CatalogDetails {
        if(details.description.isBlank()) return details
        val pt=request("translated",id=details.id,kind=details.type).optString("descriptionPt")
        return if(pt.isBlank()) details else details.copy(description=pt).also { synchronized(memory){memory["${details.type}:${details.id}"]=it} }
    }
    suspend fun coverChoices(comic:ComicEntry):List<String>{
        val ref=comic.externalId.ifBlank { comic.id };val volume=ref.startsWith("cv-volume-")
        if(ref.startsWith("mt-"))return request("covers",id=ref.removePrefix("mt-"),kind="metron-issue").optJSONArray("items")?.let {a->(0 until a.length()).map {a.optString(it)} }.orEmpty()
        val id=ref.removePrefix(if(volume) "cv-volume-" else "cv-")
        return request("covers",id=id,kind=if(volume) "volume" else "issue").optJSONArray("items")?.let { a -> (0 until a.length()).map { a.optString(it) }.distinct().take(6) }.orEmpty()
    }
    suspend fun portraits(name: String): List<String> = request("portraits",q=name.substringBefore('(').trim()).optJSONArray("items")?.let { a -> (0 until a.length()).map { a.optString(it) }.filter { it.startsWith("https://") }.distinct().take(6) }.orEmpty()
    private fun JSONObject.objects(key: String): List<JSONObject> = optJSONArray(key)?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it) } }.orEmpty()
}
