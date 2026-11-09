package com.example.data

import com.example.model.ComicEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal object AdditionalCatalogSources {
    private val client by lazy { MultiApiSearchEngine.sharedHttpClient.newBuilder().callTimeout(4, TimeUnit.SECONDS).build() }
    private val openLibraryRate = Mutex()
    private var nextOpenLibrary = 0L
    private val uuid = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    private val editionId = Regex("OL[0-9]+M")
    private fun text(j: JSONObject?, key: String) = (j?.opt(key) as? String).orEmpty().trim()
    private fun strings(j: JSONObject, key: String): List<String> = j.optJSONArray(key)?.let { a -> (0 until a.length()).mapNotNull { (a.opt(it) as? String)?.trim()?.takeIf(String::isNotBlank) } }.orEmpty()
    private fun localized(j: JSONObject?): String {
        if (j == null) return ""
        return listOf("pt-br", "pt_br", "pt", "en", "en_us", "en_jp", "ja-ro", "ja_jp", "ja").firstNotNullOfOrNull { text(j, it).takeIf(String::isNotBlank) }
            ?: j.keys().asSequence().map { text(j, it) }.firstOrNull(String::isNotBlank).orEmpty()
    }
    private fun pt(j: JSONObject?) = text(j, "pt-br").ifBlank { text(j, "pt_br") }.ifBlank { text(j, "pt") }
    private fun image(raw: String) = raw.takeIf { it.startsWith("https://") && it.length <= 2000 }.orEmpty()
    private suspend fun fetch(url: HttpUrl, accept: String = "application/json"): JSONObject {
        val raw = suspendCancellableCoroutine<String> { continuation ->
            val call = client.newCall(Request.Builder().url(url).header("Accept", accept)
                .header("User-Agent", "Quadrinheiro/1.0 (+https://github.com/felipemloiola/Quadrinheiro)").build())
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) { if (continuation.isActive) continuation.resumeWithException(e) }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val body = response.use {
                            check(it.isSuccessful) { "Fonte temporariamente indisponível" }
                            val b = it.body ?: error("Resposta vazia"); check(b.contentLength() <= 524288L)
                            val source = b.source(); val buffer = Buffer()
                            while (buffer.size <= 524288L) {
                                val n = source.read(buffer, minOf(8192L, 524289L - buffer.size))
                                if (n < 0L) break
                                check(n > 0L)
                            }
                            check(buffer.size <= 524288L); buffer.readUtf8()
                        }
                        if (continuation.isActive) continuation.resume(body)
                    } catch (e: Exception) { if (continuation.isActive) continuation.resumeWithException(e) }
                }
            })
        }
        return JSONObject(raw)
    }
    private suspend fun openLibraryFetch(url: HttpUrl): JSONObject {
        openLibraryRate.withLock {
            val wait = nextOpenLibrary - System.nanoTime()
            if (wait > 0L) delay((wait + 999999L) / 1000000L)
            nextOpenLibrary = System.nanoTime() + 1100000000L
        }
        return fetch(url)
    }
    internal fun enabledSources(provider: String, language: String): List<String> {
        if (provider == "VERIFIED_COMICS") return listOf("catalog", "kitsu", "mangadex")
        if (provider == "MANGA") return listOf("kitsu", "mangadex")
        val books = mutableListOf("catalog", "apple-br", "google-pt", "open-library")
        if (language != "PT") books.add("apple-us")
        if (provider != "BOOKS" && provider != "COMICS") books.addAll(listOf("kitsu", "mangadex"))
        return books
    }
    suspend fun load(source: String, query: String, language: String, page: Int = 0): List<ComicEntry> = withContext(Dispatchers.IO) {
        val q = query.trim().take(100)
        val safePage = page.coerceIn(0, 50)
        when (source) {
            "catalog" -> ComicVineCatalog.search(q, page = safePage, language = language).items
            "apple-br" -> PortugueseBookSources.appleBrazil(q, page = safePage)
            "google-pt" -> PortugueseBookSources.googlePortuguese(q, page = safePage)
            "apple-us" -> if (safePage > 0) emptyList() else parseAppleUs(fetch("https://itunes.apple.com/search".toHttpUrl().newBuilder().addQueryParameter("term", q).addQueryParameter("media", "ebook").addQueryParameter("entity", "ebook").addQueryParameter("country", "US").addQueryParameter("limit", "16").build()))
            "kitsu" -> parseKitsu(fetch("https://kitsu.io/api/edge/manga".toHttpUrl().newBuilder().addQueryParameter("filter[text]", q).addQueryParameter("page[limit]", "12").addQueryParameter("page[offset]", (safePage * 12).toString()).build(), "application/vnd.api+json"))
            "mangadex" -> parseMangaDex(fetch("https://api.mangadex.org/manga".toHttpUrl().newBuilder().addQueryParameter("title", q).addQueryParameter("limit", "12").addQueryParameter("offset", (safePage * 12).toString()).addQueryParameter("includes[]", "cover_art").addQueryParameter("includes[]", "author").addQueryParameter("includes[]", "artist").addQueryParameter("contentRating[]", "safe").addQueryParameter("contentRating[]", "suggestive").build()))
            "open-library" -> {
                val root = openLibraryFetch("https://openlibrary.org/search.json".toHttpUrl().newBuilder().addQueryParameter("title", q).addQueryParameter("lang", "pt").addQueryParameter("page", (safePage + 1).toString()).addQueryParameter("limit", "8").addQueryParameter("fields", "key,title,author_name,subject,editions,editions.key").build())
                val selections = selectOpenLibraryEditions(root, maxEditions = 3)
                selections.mapNotNull { (work, edId) ->
                    runCatching {
                        parseOpenLibrary(work, openLibraryFetch("https://openlibrary.org/books/$edId.json".toHttpUrl()), edId, language == "PT")
                    }.getOrNull()
                }
            }
            else -> error("Fonte desconhecida")
        }
    }
    internal fun parseAppleUs(root: JSONObject): List<ComicEntry> = PortugueseBookSources.parseApple(root).map { book ->
        book.copy(id = book.id.replaceFirst("ab-BR-", "ab-US-"), externalId = book.externalId.replaceFirst("ab-BR-", "ab-US-"))
    }
    internal fun parseKitsu(root: JSONObject): List<ComicEntry> {
        val data = root.optJSONArray("data") ?: return emptyList()
        return (0 until minOf(data.length(), 12)).mapNotNull { i ->
            val j = data.optJSONObject(i) ?: return@mapNotNull null
            val a = j.optJSONObject("attributes") ?: return@mapNotNull null
            val id = text(j, "id"); val titles = a.optJSONObject("titles")
            val title = pt(titles).ifBlank { text(a, "canonicalTitle") }.ifBlank { localized(titles) }
            if (!id.matches(Regex("[0-9]{1,12}")) || title.isBlank() || a.optBoolean("nsfw") || text(a, "ageRating") == "R18") return@mapNotNull null
            val poster = a.optJSONObject("posterImage"); val key = "kt-$id"
            ComicEntry(key, key, title.take(300), titlePtBr = pt(titles).take(300), authors = "", publisher = "", genre = "Mangá", format = "Série de mangá",
                releaseYear = text(a, "startDate").take(4).toIntOrNull() ?: 0, pageCount = 0, currentPage = 0,
                coverUrl = image(text(poster, "medium").ifBlank { text(poster, "small") }),
                synopsis = ComicVineCatalog.plain(text(a, "synopsis")).take(8000), sourceLabel = "Catálogo online", isOnlineResult = true)
        }
    }
    internal fun parseMangaDex(root: JSONObject): List<ComicEntry> {
        val data = root.optJSONArray("data") ?: return emptyList()
        return (0 until minOf(data.length(), 12)).mapNotNull { i ->
            val j = data.optJSONObject(i) ?: return@mapNotNull null
            val a = j.optJSONObject("attributes") ?: return@mapNotNull null
            val id = text(j, "id"); val titles = a.optJSONObject("title"); val title = localized(titles)
            if (!uuid.matches(id) || title.isBlank() || text(a, "contentRating") !in setOf("safe", "suggestive")) return@mapNotNull null
            val relationships = j.optJSONArray("relationships")
            val rel = (0 until (relationships?.length() ?: 0)).mapNotNull { relationships!!.optJSONObject(it) }
            val file = rel.firstOrNull { text(it, "type") == "cover_art" }?.optJSONObject("attributes")?.let { text(it, "fileName") }.orEmpty()
            val cover = if (file.matches(Regex("[A-Za-z0-9._-]{1,180}"))) "https://uploads.mangadex.org/covers/$id/$file.256.jpg" else ""
            val creators = rel.filter { text(it, "type") in setOf("author", "artist") }.map { text(it.optJSONObject("attributes"), "name") }.filter(String::isNotBlank).distinct().joinToString(", ")
            val description = a.optJSONObject("description"); val key = "md-$id"
            ComicEntry(key, key, title.take(300), titlePtBr = pt(titles).take(300), authors = creators.take(500), publisher = "", genre = "Mangá", format = "Série de mangá",
                releaseYear = a.optInt("year").takeIf { it > 0 } ?: 0, pageCount = 0, currentPage = 0, coverUrl = cover,
                synopsis = ComicVineCatalog.plain(localized(description)).take(8000), synopsisPtBr = ComicVineCatalog.plain(pt(description)).take(8000),
                sourceLabel = "Catálogo online", isOnlineResult = true)
        }
    }
    internal fun selectOpenLibraryEditions(root: JSONObject, maxEditions: Int = 3): List<Pair<JSONObject, String>> {
        val docs = root.optJSONArray("docs") ?: return emptyList()
        val out = mutableListOf<Pair<JSONObject, String>>()
        val seenIds = HashSet<String>()
        for (i in 0 until minOf(docs.length(), 8)) {
            val work = docs.optJSONObject(i) ?: continue
            if (!text(work, "key").removePrefix("/works/").matches(Regex("OL[0-9]+W"))) continue
            if (!strings(work, "subject").any { Regex("comic|graphic novel|manga|quadrinho", RegexOption.IGNORE_CASE).containsMatchIn(it.replace('_', ' ')) }) continue
            val editions = work.optJSONObject("editions")?.optJSONArray("docs") ?: continue
            for (n in 0 until editions.length()) {
                val id = text(editions.optJSONObject(n), "key").removePrefix("/books/")
                if (editionId.matches(id) && seenIds.add(id)) {
                    out.add(work to id)
                    break
                }
            }
            if (out.size >= maxEditions) break
        }
        return out
    }
    internal fun selectOpenLibrary(root: JSONObject): Pair<JSONObject, String>? = selectOpenLibraryEditions(root, maxEditions = 1).firstOrNull()
    internal fun parseOpenLibrary(work: JSONObject, edition: JSONObject, requested: String, ptOnly: Boolean): ComicEntry? {
        if (!editionId.matches(requested) || text(edition, "key").removePrefix("/books/") != requested) return null
        val works = edition.optJSONArray("works")
        if (works != null && works.length() > 0 && (0 until works.length()).none { text(works.optJSONObject(it), "key").removePrefix("/works/") == text(work, "key").removePrefix("/works/") }) return null
        val languages = edition.optJSONArray("languages")
        if (ptOnly && (0 until (languages?.length() ?: 0)).none { text(languages!!.optJSONObject(it), "key") == "/languages/por" }) return null
        val title = text(edition, "title"); if (title.isBlank()) return null
        val isbn = (strings(edition, "isbn_13") + strings(edition, "isbn_10")).firstNotNullOfOrNull(CatalogSearchPolicy::canonicalIsbn).orEmpty()
        val year = Regex("(?:18|19|20)[0-9]{2}").find(text(edition, "publish_date"))?.value?.toIntOrNull() ?: 0
        val covers = edition.optJSONArray("covers")
        val cover = (0 until (covers?.length() ?: 0)).map { covers!!.optLong(it) }.firstOrNull { it > 0 }
        val description = text(edition, "description").ifBlank { text(edition.optJSONObject("description"), "value") }
        val key = "ol-$requested"
        return ComicEntry(key, key, title.take(300), authors = strings(work, "author_name").joinToString(", ").take(500), publisher = strings(edition, "publishers").joinToString(", ").take(160),
            genre = "Quadrinhos", format = text(edition, "physical_format").ifBlank { "Edição" }.take(60), releaseYear = year,
            pageCount = edition.optInt("number_of_pages").takeIf { it in 1..20000 } ?: 0, currentPage = 0, isbn = isbn,
            coverUrl = cover?.let { "https://covers.openlibrary.org/b/id/$it-L.jpg" }.orEmpty(), synopsis = ComicVineCatalog.plain(description).take(8000),
            sourceLabel = "Catálogo online", isOnlineResult = true)
    }
    internal fun localize(raw: ComicEntry): ComicEntry {
        val translated = PortugueseComicTranslator.localizeComicMetadataOnly(raw)
        return translated.copy(authors = raw.authors, publisher = raw.publisher, releaseYear = raw.releaseYear, pageCount = raw.pageCount, currentPage = 0,
            isbn = raw.isbn, synopsis = raw.synopsis, synopsisPtBr = raw.synopsisPtBr,
            format = if (raw.format == "Série de mangá") raw.format else translated.format,
            issueNumber = if (raw.format == "Série de mangá") 0 else translated.issueNumber)
    }
}
