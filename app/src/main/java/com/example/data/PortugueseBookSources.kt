package com.example.data

import com.example.model.ComicEntry
import kotlinx.coroutines.suspendCancellableCoroutine
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

/** Public book metadata only. Provider credentials are not needed or read here. */
internal object PortugueseBookSources {
    private val client by lazy { MultiApiSearchEngine.sharedHttpClient.newBuilder().callTimeout(4, TimeUnit.SECONDS).build() }
    private suspend fun fetch(url: HttpUrl): JSONObject {
        val raw = suspendCancellableCoroutine<String> { continuation ->
            val call = client.newCall(Request.Builder().url(url).header("Accept", "application/json").build())
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) { if (continuation.isActive) continuation.resumeWithException(e) }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val text = response.use {
                            check(it.isSuccessful) { "Fonte temporariamente indisponível" }
                            val source = it.body?.source() ?: error("Resposta vazia")
                            val buffer = Buffer()
                            while (buffer.size <= 524288L) {
                                val count = source.read(buffer, minOf(8192L, 524289L - buffer.size))
                                if (count < 0L) break
                                check(count > 0L)
                            }
                            check(buffer.size <= 524288L) { "Resposta excede o limite" }
                            buffer.readUtf8()
                        }
                        if (continuation.isActive) continuation.resume(text)
                    } catch (e: Exception) { if (continuation.isActive) continuation.resumeWithException(e) }
                }
            })
        }
        return JSONObject(raw)
    }
    private fun text(j: JSONObject, key: String) = j.optString(key).takeUnless { it == "null" }.orEmpty().trim()
    private fun comicGenre(raw: String): Boolean = Regex("comic|graphic novel|manga|quadrinho", RegexOption.IGNORE_CASE).containsMatchIn(raw)
    private fun cover(raw: String) = raw.replace("http://", "https://").takeIf { it.startsWith("https://") && it.length <= 2000 }.orEmpty()

    suspend fun appleBrazil(query: String, page: Int = 0): List<ComicEntry> {
        if (page > 0) return emptyList()
        return parseApple(fetch("https://itunes.apple.com/search".toHttpUrl().newBuilder()
            .addQueryParameter("term", query.take(100)).addQueryParameter("media", "ebook").addQueryParameter("entity", "ebook")
            .addQueryParameter("country", "BR").addQueryParameter("limit", "16").build()))
    }

    suspend fun googlePortuguese(query: String, page: Int = 0): List<ComicEntry> = parseGoogle(fetch("https://www.googleapis.com/books/v1/volumes".toHttpUrl().newBuilder()
        .addQueryParameter("q", query.take(100)).addQueryParameter("maxResults", "16")
        .addQueryParameter("startIndex", (page.coerceAtLeast(0) * 16).toString())
        .addQueryParameter("printType", "books")
        .addQueryParameter("langRestrict", "pt").build()))

    internal fun parseApple(root: JSONObject): List<ComicEntry> {
        val rows = root.optJSONArray("results") ?: return emptyList()
        return (0 until rows.length()).mapNotNull { index ->
            val j = rows.optJSONObject(index) ?: return@mapNotNull null
            val genres = j.optJSONArray("genres")
            val category = (0 until (genres?.length() ?: 0)).joinToString(" ") { genres!!.optString(it) }
            val id = j.optLong("trackId"); val title = text(j, "trackName")
            if (id <= 0 || title.isBlank() || !comicGenre(category)) return@mapNotNull null
            val key = "ab-BR-$id"
            ComicEntry(id = key, externalId = key, title = title.take(300), authors = text(j, "artistName").take(500), publisher = "",
                genre = "Quadrinhos", format = "Edição", releaseYear = text(j, "releaseDate").take(4).toIntOrNull() ?: 0,
                pageCount = 0, currentPage = 0, coverUrl = cover(text(j, "artworkUrl100").replace("100x100bb", "600x600bb")),
                synopsis = ComicVineCatalog.plain(text(j, "description")).take(8000), sourceLabel = "Catálogo online", isOnlineResult = true)
        }.take(16)
    }

    internal fun parseGoogle(root: JSONObject): List<ComicEntry> {
        val rows = root.optJSONArray("items") ?: return emptyList()
        return (0 until rows.length()).mapNotNull { index ->
            val j = rows.optJSONObject(index) ?: return@mapNotNull null
            val v = j.optJSONObject("volumeInfo") ?: return@mapNotNull null
            if(v.optString("language").lowercase().substringBefore('-') !in setOf("", "pt", "en")) return@mapNotNull null
            val categories = v.optJSONArray("categories")
            val category = (0 until (categories?.length() ?: 0)).joinToString(" ") { categories!!.optString(it) }
            val id = text(j, "id"); val title = text(v, "title")
            if (id.isBlank() || id.length > 100 || title.isBlank() || !comicGenre(category)) return@mapNotNull null
            val subtitle = text(v, "subtitle")
            val fullTitle = if (subtitle.isNotBlank() && !title.contains(subtitle, true)) "$title: $subtitle" else title
            val authors = v.optJSONArray("authors")
            val identifiers = v.optJSONArray("industryIdentifiers")
            val isbn = (0 until (identifiers?.length() ?: 0)).mapNotNull { identifiers!!.optJSONObject(it) }
                .filter { text(it, "type") in setOf("ISBN_10", "ISBN_13") }
                .firstNotNullOfOrNull { CatalogSearchPolicy.canonicalIsbn(text(it, "identifier")) }.orEmpty()
            val key = "gb-$id"
            ComicEntry(id = key, externalId = key, title = fullTitle.take(300),
                authors = (0 until (authors?.length() ?: 0)).joinToString(", ") { authors!!.optString(it) }.take(500),
                publisher = text(v, "publisher").take(160), genre = "Quadrinhos", format = "Edição",
                releaseYear = text(v, "publishedDate").take(4).toIntOrNull() ?: 0,
                pageCount = v.optInt("pageCount").takeIf { it in 1..20000 } ?: 0, currentPage = 0,
                coverUrl = cover(v.optJSONObject("imageLinks")?.optString("thumbnail").orEmpty()),
                synopsis = ComicVineCatalog.plain(text(v, "description")).take(8000), isbn = isbn,
                sourceLabel = "Catálogo online", isOnlineResult = true)
        }.take(16)
    }
}
