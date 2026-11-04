package com.example.data

import android.graphics.BitmapFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal object ComicBannerResolver {
    private val hosts = setOf("comicvine.gamespot.com", "www.comicvine.com", "comicvine1.cbsistatic.com", "static.comicvine.com")
    private val client by lazy { MultiApiSearchEngine.sharedHttpClient.newBuilder().cache(null).followRedirects(false).followSslRedirects(false).callTimeout(1800, TimeUnit.MILLISECONDS).build() }
    private val probes = Semaphore(2)
    private data class BannerResult(val url: String, val savedAt: Long)
    private val cache = object : LinkedHashMap<String, BannerResult>(24, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, BannerResult>?) = size > 24
    }
    private fun allowed(raw: String): Boolean {
        val u = raw.toHttpUrlOrNull() ?: return false
        if (raw.length > 2000 || u.scheme != "https" || u.port != 443 || u.username.isNotEmpty() || u.password.isNotEmpty() || u.query != null || u.fragment != null) return false
        return if (u.host in hosts) u.encodedPath.startsWith("/a/uploads/") || u.encodedPath.startsWith("/uploads/")
            else (u.host == "metron.cloud" || u.host.endsWith(".metron.cloud")) && !u.encodedPath.startsWith("/api/")
    }
    internal fun candidates(detail: CatalogDetails): List<String> =
        (listOf(detail.banner) + detail.covers + if (detail.type.endsWith("character")) listOf(detail.image) else emptyList())
            .filter(::allowed).distinctBy {
                val u = it.toHttpUrlOrNull()!!
                u.host + u.encodedPath.replace(Regex("""/(?:scale_[^/]+|screen_[^/]+|original)/"""), "/")
            }.take(6)
    internal fun creditedCharacter(detail: CatalogDetails, title: String): CatalogCredit? {
        if (detail.type !in setOf("issue", "volume", "metron-issue")) return null
        val ids = if (detail.type == "metron-issue") Regex("mt-[1-9][0-9]{0,11}") else Regex("[1-9][0-9]{0,11}")
        val normalizedTitle = " ${CatalogSearchPolicy.normalize(title)} "
        return detail.characters.filter { ids.matches(it.id) }.mapNotNull { credit ->
            val base = credit.name.substringBefore('(').trim()
            val names = mutableListOf(base)
            CharacterDirectory.portuguese.forEach { (en, pt) ->
                if (CatalogSearchPolicy.normalize(base) in listOf(CatalogSearchPolicy.normalize(en), CatalogSearchPolicy.normalize(pt))) { names.add(en); names.add(pt) }
            }
            val strength = names.map(CatalogSearchPolicy::normalize).filter { it.length >= 3 && normalizedTitle.contains(" $it ") }.maxOfOrNull { it.length }
            strength?.let { credit to it }
        }.maxByOrNull { it.second }?.first
    }
    internal fun landscape(width: Int, height: Int): Boolean = width in 480..32768 && height in 200..32768 && width.toDouble() / height in 1.6..4.0
    internal fun landscape(bytes: ByteArray): Boolean {
        if (bytes.isEmpty() || bytes.size > 65536) return false
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        return landscape(bounds.outWidth, bounds.outHeight)
    }
    private suspend fun prefix(url: String): ByteArray = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(Request.Builder().url(url).header("Range", "bytes=0-65535").header("Accept", "image/*").build())
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { if (continuation.isActive) continuation.resumeWithException(e) }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val bytes = response.use {
                        val body = it.body
                        if (!it.isSuccessful || body == null || !it.header("Content-Type").orEmpty().startsWith("image/", true)) ByteArray(0)
                        else {
                            val source = body.source(); val buffer = Buffer()
                            while (buffer.size < 65536L) {
                                val count = source.read(buffer, minOf(8192L, 65536L - buffer.size))
                                if (count < 0L) break
                                check(count > 0L)
                            }
                            buffer.readByteArray()
                        }
                    }
                    if (continuation.isActive) continuation.resume(bytes)
                } catch (e: Exception) { if (continuation.isActive) continuation.resumeWithException(e) }
            }
        })
    }
    private suspend fun first(detail: CatalogDetails): String? = coroutineScope {
        for (batch in candidates(detail).chunked(2)) {
            val checked = batch.map { url -> async {
                val valid = probes.withPermit {
                    try { landscape(prefix(url)) } catch (cancel: CancellationException) { throw cancel } catch (_: Exception) { false }
                }
                url to valid
            } }.awaitAll()
            checked.firstOrNull { it.second }?.let { return@coroutineScope it.first }
        }
        null
    }
    suspend fun resolve(detail: CatalogDetails, title: String, force: Boolean = false): String = withContext(Dispatchers.IO) {
        val key = "${detail.type}:${detail.id}"
        val saved = synchronized(cache) { cache[key] }
        if (!force && saved != null) {
            val age = System.currentTimeMillis() - saved.savedAt
            if (age in 0 until (if (saved.url.isBlank()) 60000L else 600000L)) return@withContext saved.url
        }
        val found = withTimeoutOrNull(6000L) {
            first(detail) ?: creditedCharacter(detail, title)?.let { credit ->
                val character = ComicVineCatalog.details("character", credit.id)
                first(character)
            }.orEmpty()
        }.orEmpty()
        synchronized(cache) { cache[key] = BannerResult(found, System.currentTimeMillis()) }
        found
    }
}
