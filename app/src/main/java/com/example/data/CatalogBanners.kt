package com.example.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal enum class BannerErrorKind {
    RATE_LIMITED,
    MISSING_CONFIG,
    SERVICE_UNAVAILABLE,
    NETWORK_ERROR
}

internal class BannerSearchException(
    val kind: BannerErrorKind,
    override val message: String,
    cause: Throwable? = null
) : IOException(message, cause)

internal data class BannerArtwork(val url: String, val width: Int, val height: Int, val title: String)

internal object CatalogBanners {
    private const val ENDPOINT = "https://quadrinheiro-catalog.uadrinheiro.workers.dev"
    private val hosts = setOf("comicvine.gamespot.com", "www.comicvine.com", "comicvine1.cbsistatic.com", "static.comicvine.com")
    private val client by lazy { MultiApiSearchEngine.sharedHttpClient.newBuilder().followRedirects(false).followSslRedirects(false).callTimeout(25, TimeUnit.SECONDS).build() }
    private data class BannerCacheValue(val items: List<BannerArtwork>, val savedAt: Long)
    private val cache = object : LinkedHashMap<String, BannerCacheValue>(16, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, BannerCacheValue>?) = size > 16
    }

    internal fun allowed(raw: String): Boolean {
        if (raw.length > 2000) return false
        val u = raw.toHttpUrlOrNull() ?: return false
        return u.scheme == "https" && u.host in hosts && u.port == 443 && u.username.isEmpty() && u.password.isEmpty() &&
            u.query == null && u.fragment == null && (u.encodedPath.startsWith("/a/uploads/") || u.encodedPath.startsWith("/uploads/"))
    }

    internal fun parse(root: JSONObject): List<BannerArtwork> {
        val rows = root.optJSONArray("artwork") ?: return emptyList()
        return (0 until minOf(rows.length(), 24)).mapNotNull { index ->
            val row = rows.optJSONObject(index) ?: return@mapNotNull null
            val url = row.optString("url"); val w = row.optInt("width"); val h = row.optInt("height")
            if (!allowed(url) || w !in 480..32768 || h !in 200..32768 || w.toDouble() / h !in 1.6..4.0) return@mapNotNull null
            BannerArtwork(url, w, h, row.optString("title").take(300))
        }.distinctBy {
            val u = it.url.toHttpUrl()
            u.host + u.encodedPath.replace(Regex("""/(?:scale_[^/]+|screen_[^/]+|original)/"""), "/")
        }.take(6)
    }

    private suspend fun read(url: String, limit: Long): ByteArray = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(Request.Builder().url(url).get().build())
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) {
                    continuation.resumeWithException(
                        BannerSearchException(BannerErrorKind.NETWORK_ERROR, "Sem conexão com o serviço de artes. Verifique sua internet e tente novamente.", e)
                    )
                }
            }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val redirectUrl = if (response.code in listOf(301, 302, 307, 308)) {
                        response.header("Location")?.let { loc -> response.request.url.resolve(loc)?.toString() }?.takeIf(::allowed)
                    } else null
                    if (redirectUrl != null) {
                        response.close()
                        val nextCall = client.newCall(Request.Builder().url(redirectUrl).get().build())
                        continuation.invokeOnCancellation { nextCall.cancel() }
                        nextCall.enqueue(object : Callback {
                            override fun onFailure(call: Call, e: IOException) {
                                if (continuation.isActive) continuation.resumeWithException(e)
                            }
                            override fun onResponse(call: Call, redirected: Response) {
                                try {
                                    val bytes = consumeResponseBody(redirected, limit)
                                    if (continuation.isActive) continuation.resume(bytes)
                                } catch (e: Exception) {
                                    if (continuation.isActive) continuation.resumeWithException(e)
                                }
                            }
                        })
                        return
                    }
                    val bytes = consumeResponseBody(response, limit)
                    if (continuation.isActive) continuation.resume(bytes)
                } catch (e: Exception) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }
            }
        })
    }

    private fun consumeResponseBody(response: Response, limit: Long): ByteArray = response.use {
        if (!it.isSuccessful) {
            val errBody = runCatching { it.body?.string().orEmpty() }.getOrDefault("")
            val errCode = runCatching { JSONObject(errBody).optString("error") }.getOrDefault("")
            when {
                it.code == 429 || errCode == "provider-limit" ->
                    throw BannerSearchException(BannerErrorKind.RATE_LIMITED, "Limite temporário da API de artes atingido. Aguarde alguns instantes e tente novamente.")
                errCode == "missing-config" ->
                    throw BannerSearchException(BannerErrorKind.MISSING_CONFIG, "Configuração do provedor de artes ausente no serviço.")
                else ->
                    throw BannerSearchException(BannerErrorKind.SERVICE_UNAVAILABLE, "Serviço de busca de artes temporariamente indisponível.")
            }
        }
        val body = it.body ?: error("Resposta vazia")
        check(body.contentLength() <= limit) { "Imagem acima do limite permitido." }
        val source = body.source(); val buffer = Buffer()
        while (buffer.size <= limit) {
            val count = source.read(buffer, minOf(8192L, limit + 1 - buffer.size))
            if (count < 0L) break
            check(count > 0L)
        }
        check(buffer.size <= limit) { "Resposta acima do limite permitido." }
        buffer.readByteArray()
    }

    private suspend fun fallbackViaCatalogDetails(query: String, kind: String): List<BannerArtwork> = coroutineScope {
        val terms = CatalogSearchPolicy.searchTerms(query).ifEmpty { listOf(query) }
        val details = mutableListOf<CatalogDetails>()
        if (kind == "character") {
            val chars = terms.flatMap { term ->
                runCatching { ComicVineCatalog.characters(term) }.getOrDefault(emptyList())
            }.distinctBy { it.id }.take(3)
            for (c in chars) {
                val id = c.id.removePrefix("cv-")
                if (id.all { it.isDigit() } && id.isNotEmpty()) {
                    runCatching { ComicVineCatalog.details("character", id) }.getOrNull()?.let { details += it }
                }
            }
        } else {
            val comics = terms.flatMap { term ->
                runCatching { ComicVineCatalog.search(term, 0).items }.getOrDefault(emptyList())
            }.distinctBy { it.id }.take(3)
            for (c in comics) {
                val rawId = c.id.removePrefix("cv-").removePrefix("cvvol-")
                if (rawId.all { it.isDigit() } && rawId.isNotEmpty()) {
                    val dType = if (c.id.startsWith("cvvol-")) "volume" else "issue"
                    runCatching { ComicVineCatalog.details(dType, rawId) }.getOrNull()?.let { details += it }
                }
            }
        }
        val candidatePairs = details.flatMap { detail ->
            val title = detail.title.ifBlank { query }
            ComicBannerResolver.candidates(detail).map { url -> url to title }
        }.distinctBy { (url, _) ->
            val u = url.toHttpUrl()
            u.host + u.encodedPath.replace(Regex("""/(?:scale_[^/]+|screen_[^/]+|original)/"""), "/")
        }.take(8)

        candidatePairs.map { (url, title) ->
            async { ComicBannerResolver.probeLandscapeArtwork(url, title) }
        }.awaitAll().filterNotNull().take(6)
    }

    suspend fun search(query: String, kind: String): List<BannerArtwork> = withContext(Dispatchers.IO) {
        val q = query.trim().take(100)
        require(kind in setOf("volume", "character"))
        if (q.length < 2) return@withContext emptyList()
        val key = "$kind:${q.lowercase(Locale.ROOT)}"
        val saved = synchronized(cache) { cache[key] }
        val age = saved?.let { System.currentTimeMillis() - it.savedAt }
        if (saved != null && age != null && age in 0 until (if (saved.items.isEmpty()) 30000L else 600000L)) return@withContext saved.items

        val url = ENDPOINT.toHttpUrl().newBuilder().addPathSegment("banner").addQueryParameter("query", q).addQueryParameter("kind", kind).build()
        var primaryError: Exception? = null
        val primaryItems = try {
            parse(JSONObject(read(url.toString(), 65536L).toString(Charsets.UTF_8)))
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (e: Exception) {
            primaryError = e
            emptyList()
        }

        val items = if (primaryItems.isNotEmpty()) {
            primaryItems
        } else {
            val fallback = try {
                fallbackViaCatalogDetails(q, kind)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                emptyList()
            }
            if (fallback.isNotEmpty()) {
                fallback
            } else if (primaryError != null) {
                throw primaryError
            } else {
                emptyList()
            }
        }
        synchronized(cache) { cache[key] = BannerCacheValue(items, System.currentTimeMillis()) }
        items
    }

    internal fun decode(bytes: ByteArray): Bitmap {
        require(bytes.isNotEmpty() && bytes.size <= 6 * 1024 * 1024)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth in 480..32768 && bounds.outHeight in 200..32768 && bounds.outWidth.toDouble() / bounds.outHeight in 1.6..4.0) { "A imagem não é horizontal." }
        var sample = 1
        while (bounds.outWidth / sample > 1800 || bounds.outHeight / sample > 1800) sample *= 2
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 }) ?: error("Imagem inválida")
    }

    suspend fun bitmap(url: String): Bitmap = withContext(Dispatchers.IO) {
        require(allowed(url)) { "Imagem não permitida." }
        decode(read(url, 6L * 1024 * 1024))
    }

    suspend fun bitmapFromBannerValue(bannerValue: String): Bitmap = withContext(Dispatchers.IO) {
        val trimmed = bannerValue.trim()
        if (trimmed.startsWith("data:image/", ignoreCase = true) && trimmed.contains(";base64,")) {
            val base64Part = trimmed.substringAfter(";base64,")
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            require(bytes.isNotEmpty() && bytes.size <= 6 * 1024 * 1024) { "Imagem inválida." }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            require(bounds.outWidth in 240..32768 && bounds.outHeight in 120..32768) { "Imagem inválida." }
            var sample = 1
            while (bounds.outWidth / sample > 1800 || bounds.outHeight / sample > 1800) sample *= 2
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }) ?: error("Imagem inválida.")
        } else {
            bitmap(trimmed)
        }
    }
}

