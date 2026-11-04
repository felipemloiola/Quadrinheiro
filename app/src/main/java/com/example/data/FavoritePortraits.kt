package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject
import java.io.IOException

internal object FavoritePortraits {
    private val cvId = Regex("cv-([1-9][0-9]{0,11})")
    private val scales = Regex("""/(?:scale_[^/]+|screen_[^/]+|original)/""")
    private val hosts = setOf("comicvine.gamespot.com", "www.comicvine.com", "comicvine1.cbsistatic.com", "static.comicvine.com")
    private fun normalized(text: String) = CatalogSearchPolicy.normalize(text)
    private fun snapshot(raw: String): JSONObject? = runCatching { JSONObject(raw) }.getOrNull()
    internal fun linkedId(raw: String): String? = snapshot(raw)?.optString("id")?.let { cvId.matchEntire(it)?.groupValues?.get(1) }
    internal fun exactId(display: String, original: String, candidates: List<CharacterResult>): String? {
        fun id(c: CharacterResult) = cvId.matchEntire(c.id)?.groupValues?.get(1)
        val valid = candidates.filter { id(it) != null }.distinctBy { it.id }
        val full = normalized(display)
        val exact = valid.filter { full.isNotBlank() && normalized(it.name) == full }
        if (exact.size == 1) return id(exact.single())
        val bases = listOf(display.substringBefore('('), original).map(::normalized).filter { it.isNotBlank() }.toSet()
        val civil = normalized(display.substringAfter('(', "").substringBefore(')'))
        val matches = valid.filter { c ->
            (normalized(c.originalName) in bases || normalized(c.name.substringBefore('(')) in bases) &&
                (civil.isBlank() || normalized(c.name.substringAfter('(', "").substringBefore(')')) == civil || c.aliases.any { normalized(it) == civil })
        }
        return matches.singleOrNull()?.let(::id)
    }
    private fun key(raw: String): String {
        val u = raw.toHttpUrlOrNull() ?: return raw
        return u.host + u.encodedPath.replace(scales, "/") + (u.query?.let { "?$it" } ?: "")
    }
    private fun publicImage(raw: String): Boolean {
        val u = raw.toHttpUrlOrNull() ?: return false
        return raw.length <= 2000 && u.scheme == "https" && u.username.isEmpty() && u.password.isEmpty() &&
            u.queryParameterNames.none { it.lowercase() in setOf("api_key", "apikey", "api-key", "token", "access_token") }
    }
    private fun providerImage(raw: String): Boolean {
        if (!publicImage(raw)) return false
        val u = raw.toHttpUrlOrNull() ?: return false
        return u.host in hosts && u.port == 443 && u.query == null && u.fragment == null &&
            (u.encodedPath.startsWith("/a/uploads/") || u.encodedPath.startsWith("/uploads/"))
    }
    internal fun merge(current: String, alternatives: List<String>): List<String> =
        (listOf(current).filter { it.isNotBlank() } + alternatives.filter(::publicImage)).distinctBy(::key).take(6)
    internal fun resourceImages(detail: CatalogDetails, id: String): List<String> {
        if (detail.type != "character" || detail.id != id) return emptyList()
        return (detail.covers + detail.image).filter(::providerImage).distinctBy(::key).take(6)
    }
    suspend fun choices(display: String, rawSnapshot: String): List<String> = withContext(Dispatchers.IO) {
        withTimeoutOrNull(7000L) {
            val original = snapshot(rawSnapshot)?.optString("name").orEmpty().ifBlank { display.substringBefore('(').trim() }
            val linked = linkedId(rawSnapshot)
            if (linked == null && original.trim().length < 2) return@withTimeoutOrNull emptyList<String>()
            val id = linked ?: exactId(display, original, ComicVineCatalog.characters(original))
                ?: return@withTimeoutOrNull emptyList<String>()
            resourceImages(ComicVineCatalog.details("character", id), id)
        } ?: throw IOException("Imagens temporariamente indisponíveis")
    }
}
