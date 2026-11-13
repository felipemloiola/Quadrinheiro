package com.example.data

import com.example.model.CatalogPagePolicy
import com.example.model.ComicEntry
import kotlinx.coroutines.CancellationException
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject

/** Page counts belong to an edition: never borrow a total from a title search. */
internal object CatalogPageCountResolver {
    private fun count(raw: Int): Int? = raw.takeIf { it in 1..CatalogPagePolicy.MAX_PAGES }
    private fun googleIsbns(volume: JSONObject): List<String> {
        val ids = volume.optJSONObject("volumeInfo")?.optJSONArray("industryIdentifiers")
        return (0 until (ids?.length() ?: 0)).mapNotNull {
            val id = ids!!.optJSONObject(it) ?: return@mapNotNull null
            if (id.optString("type") !in setOf("ISBN_10", "ISBN_13")) null
            else CatalogSearchPolicy.canonicalIsbn(id.optString("identifier"))
        }
    }
    private fun libraryIsbns(edition: JSONObject): List<String> = listOf("isbn_13", "isbn_10").flatMap { key ->
        val ids = edition.optJSONArray(key)
        (0 until (ids?.length() ?: 0)).mapNotNull { CatalogSearchPolicy.canonicalIsbn(ids!!.optString(it)) }
    }

    suspend fun resolve(
        comic: ComicEntry,
        details: CatalogDetails? = null,
        fetch: suspend (HttpUrl) -> JSONObject = AdditionalCatalogSources::pageMetadata
    ): Int? {
        count(comic.pageCount)?.let { return it }
        count(details?.pageCount ?: 0)?.let { return it }
        suspend fun request(url: HttpUrl): JSONObject? = try { fetch(url) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { null }

        val ref = comic.externalId.ifBlank { comic.id }
        var isbn = CatalogSearchPolicy.canonicalIsbn(comic.isbn)
            ?: details?.isbn?.let(CatalogSearchPolicy::canonicalIsbn)
        if (ref.startsWith("gb-")) {
            val id = ref.removePrefix("gb-")
            if (id.matches(Regex("[A-Za-z0-9_-]{1,100}"))) {
                val volume = request("https://www.googleapis.com/books/v1/volumes".toHttpUrl().newBuilder().addPathSegment(id).build())
                if (volume?.optString("id") == id) {
                    count(volume.optJSONObject("volumeInfo")?.optInt("pageCount") ?: 0)?.let { return it }
                    isbn = isbn ?: googleIsbns(volume).firstOrNull()
                }
            }
        } else if (ref.matches(Regex("ol-OL[0-9]+M"))) {
            val id = ref.removePrefix("ol-")
            val edition = request("https://openlibrary.org/books/$id.json".toHttpUrl())
            if (edition?.optString("key") == "/books/$id") {
                count(edition.optInt("number_of_pages"))?.let { return it }
                isbn = isbn ?: libraryIsbns(edition).firstOrNull()
            }
        }
        val editionIsbn = isbn ?: return null
        // Search responses sometimes omit pageCount; first try the exact ISBN in Open Library.
        val edition = request("https://openlibrary.org/isbn/$editionIsbn.json".toHttpUrl())
        if (edition != null && editionIsbn in libraryIsbns(edition)) {
            count(edition.optInt("number_of_pages"))?.let { return it }
        }
        val volumes = request("https://www.googleapis.com/books/v1/volumes".toHttpUrl().newBuilder()
            .addQueryParameter("q", "isbn:$editionIsbn").addQueryParameter("maxResults", "5").build())?.optJSONArray("items")
        for (i in 0 until (volumes?.length() ?: 0)) {
            val volume = volumes!!.optJSONObject(i) ?: continue
            if (editionIsbn !in googleIsbns(volume)) continue
            count(volume.optJSONObject("volumeInfo")?.optInt("pageCount") ?: 0)?.let { return it }
        }
        return null
    }
}
