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

    private val extendedHosts = hosts + setOf(
        "metron.cloud",
        "static.metron.cloud",
        "cdn.jsdelivr.net",
        "uploads.mangadex.org",
        "covers.openlibrary.org",
        "books.google.com",
        "books.googleusercontent.com",
        "is1-ssl.mzstatic.com",
        "is2-ssl.mzstatic.com",
        "is3-ssl.mzstatic.com",
        "is4-ssl.mzstatic.com",
        "is5-ssl.mzstatic.com"
    )

    internal fun allowed(raw: String): Boolean {
        if (raw.length > 2000) return false
        val u = raw.toHttpUrlOrNull() ?: return false
        return u.scheme == "https" && u.host in hosts && u.port == 443 && u.username.isEmpty() && u.password.isEmpty() &&
            u.query == null && u.fragment == null && (u.encodedPath.startsWith("/a/uploads/") || u.encodedPath.startsWith("/uploads/"))
    }

    internal fun allowedBannerSource(raw: String): Boolean {
        if (raw.length > 2000) return false
        val u = raw.toHttpUrlOrNull() ?: return false
        if (u.scheme != "https" || u.port != 443 || u.username.isNotEmpty() || u.password.isNotEmpty() || u.fragment != null) return false
        if (u.queryParameterNames.any { it.lowercase(Locale.ROOT) in setOf("api_key", "apikey", "api-key", "token", "access_token") }) return false
        return u.host in extendedHosts || u.host.endsWith(".mzstatic.com") || u.host.endsWith(".metron.cloud")
    }

    private fun upgradeArtworkUrl(raw: String): String {
        val trimmed = raw.trim().replace("http://", "https://")
        return when {
            trimmed.contains("cdn.jsdelivr.net/gh/akabab/superhero-api") && trimmed.contains("/images/md/") ->
                trimmed.replace("/images/md/", "/images/lg/")
            trimmed.contains("mzstatic.com/") && trimmed.contains("600x600bb") ->
                trimmed.replace("600x600bb", "1200x1200bb")
            trimmed.contains("mzstatic.com/") && trimmed.contains("100x100bb") ->
                trimmed.replace("100x100bb", "1200x1200bb")
            trimmed.contains("comicvine") && (trimmed.contains("/scale_small/") || trimmed.contains("/scale_medium/") || trimmed.contains("/scale_avatar/")) ->
                trimmed.replace("/scale_small/", "/scale_super/")
                    .replace("/scale_medium/", "/scale_super/")
                    .replace("/scale_avatar/", "/scale_super/")
            trimmed.contains("uploads.mangadex.org/covers/") && trimmed.endsWith(".256.jpg") ->
                trimmed.removeSuffix(".256.jpg") + ".512.jpg"
            else -> trimmed
        }
    }

    private fun canonicalUrlKey(raw: String): String {
        val u = raw.toHttpUrlOrNull() ?: return raw
        return u.host + u.encodedPath
            .replace(Regex("""/(?:scale_[^/]+|screen_[^/]+|original)/"""), "/")
            .replace("/images/lg/", "/images/md/")
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
                        response.header("Location")?.let { loc -> response.request.url.resolve(loc)?.toString() }?.takeIf(::allowedBannerSource)
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

    private suspend fun fallbackViaCatalogDetails(query: String, kind: String, context: android.content.Context? = null): List<BannerArtwork> = coroutineScope {
        val terms = CatalogSearchPolicy.searchTerms(query).ifEmpty { listOf(query) }
        val collected = mutableListOf<BannerArtwork>()

        if (kind == "character") {
            // 1. Local & cached CharacterDirectory (instant, high-res SuperHero API portraits)
            val dirMatches = runCatching {
                if (context != null) CharacterDirectory.search(context, query)
                else CharacterDirectory.searchLoaded(query)
            }.getOrDefault(emptyList())
            for (c in dirMatches.take(8)) {
                val url = upgradeArtworkUrl(c.image)
                if (allowedBannerSource(url)) {
                    collected += BannerArtwork(url, 1200, 675, c.name.take(300))
                }
            }

            // 2. Remote ComicVine / SuperHero character portraits & details
            val remoteChars = terms.take(2).flatMap { term ->
                val cv = runCatching { ComicVineCatalog.characters(term) }.getOrDefault(emptyList())
                val sh = runCatching { ComicVineCatalog.superheroCharacters(term) }.getOrDefault(emptyList())
                cv + sh
            }.distinctBy { it.id }.take(6)

            for (c in remoteChars) {
                val upgraded = upgradeArtworkUrl(c.image)
                if (allowedBannerSource(upgraded)) {
                    collected += BannerArtwork(upgraded, 1200, 675, c.name.take(300))
                }
                val rawId = c.id.removePrefix("cv-")
                if (rawId.all { it.isDigit() } && rawId.isNotEmpty()) {
                    val detail = runCatching { ComicVineCatalog.details("character", rawId) }.getOrNull()
                    if (detail != null) {
                        val title = detail.title.ifBlank { c.name }.ifBlank { query }
                        val urls = listOf(detail.banner, detail.image) + detail.covers
                        for (u in urls) {
                            val up = upgradeArtworkUrl(u)
                            if (allowedBannerSource(up)) {
                                collected += BannerArtwork(up, 1200, 675, title.take(300))
                            }
                        }
                    }
                }
            }

            val portraits = runCatching { ComicVineCatalog.portraits(query) }.getOrDefault(emptyList())
            for (u in portraits) {
                val up = upgradeArtworkUrl(u)
                if (allowedBannerSource(up)) {
                    collected += BannerArtwork(up, 1200, 675, query.take(300))
                }
            }
        } else {
            // kind == "volume" (HQs)
            // 1. Local ModernCatalog matches
            val localMatches = CatalogSearchPolicy.filterAndRankForQuery(ModernCatalog.current, query).take(6)
            for (c in localMatches) {
                val up = upgradeArtworkUrl(c.coverUrl)
                if (allowedBannerSource(up)) {
                    collected += BannerArtwork(up, 1200, 675, c.displayTitle.take(300))
                }
            }

            // 2. Parallel multi-source HQ artwork search (ComicVine, Apple Books BR, Google Books PT, MangaDex)
            val deferredBatches = listOf(
                async {
                    terms.take(2).flatMap { term ->
                        runCatching { ComicVineCatalog.search(term, 0).items }.getOrDefault(emptyList())
                    }
                },
                async {
                    runCatching { PortugueseBookSources.appleBrazil(query, 0) }.getOrDefault(emptyList())
                },
                async {
                    runCatching { PortugueseBookSources.googlePortuguese(query, 0) }.getOrDefault(emptyList())
                },
                async {
                    runCatching { AdditionalCatalogSources.load("mangadex", query, "ALL", 0) }.getOrDefault(emptyList())
                }
            )
            val allComics = CatalogSearchPolicy.filterAndRankForQuery(
                deferredBatches.awaitAll().flatten(),
                query
            ).take(10)

            for (c in allComics) {
                val up = upgradeArtworkUrl(c.coverUrl)
                if (allowedBannerSource(up)) {
                    collected += BannerArtwork(up, 1200, 675, c.displayTitle.take(300))
                }
                val ref = c.externalId.ifBlank { c.id }
                val (dType, rawId) = when {
                    ref.startsWith("mt-") -> "metron-issue" to ref.removePrefix("mt-")
                    ref.startsWith("cv-volume-") -> "volume" to ref.removePrefix("cv-volume-")
                    ref.startsWith("cvvol-") -> "volume" to ref.removePrefix("cvvol-")
                    ref.startsWith("cv-") -> "issue" to ref.removePrefix("cv-")
                    else -> "" to ""
                }
                if (dType.isNotEmpty() && rawId.all { it.isDigit() } && rawId.isNotEmpty() && collected.size < 8) {
                    val detail = runCatching { ComicVineCatalog.details(dType, rawId) }.getOrNull()
                    if (detail != null) {
                        val title = detail.title.ifBlank { c.displayTitle }.ifBlank { query }
                        for (u in listOf(detail.banner, detail.image) + detail.covers) {
                            val upDetail = upgradeArtworkUrl(u)
                            if (allowedBannerSource(upDetail)) {
                                collected += BannerArtwork(upDetail, 1200, 675, title.take(300))
                            }
                        }
                    }
                }
            }
        }

        collected.distinctBy { canonicalUrlKey(it.url) }.take(10)
    }

    suspend fun search(query: String, kind: String, context: android.content.Context? = null): List<BannerArtwork> = withContext(Dispatchers.IO) {
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

        val fallback = if (primaryItems.size < 6) {
            try {
                fallbackViaCatalogDetails(q, kind, context)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        val items = (primaryItems + fallback).distinctBy { canonicalUrlKey(it.url) }.take(10)
        if (items.isEmpty() && primaryError != null) {
            throw primaryError
        }
        synchronized(cache) { cache[key] = BannerCacheValue(items, System.currentTimeMillis()) }
        items
    }

    internal fun decode(bytes: ByteArray): Bitmap {
        require(bytes.isNotEmpty() && bytes.size <= 6 * 1024 * 1024)
        val bounds = SafeImageFormats.decodeBoundsSafely(bytes)
        require(bounds.outWidth in 480..32768 && bounds.outHeight in 200..32768 && bounds.outWidth.toDouble() / bounds.outHeight in 1.6..4.0) { "A imagem não é horizontal." }
        var sample = 1
        while (bounds.outWidth / sample > 1800 || bounds.outHeight / sample > 1800) sample *= 2
        return SafeImageFormats.decodeByteArraySafely(bytes, BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 }) ?: error("Imagem inválida")
    }

    private fun decodeForBannerCrop(bytes: ByteArray): Bitmap {
        require(bytes.isNotEmpty() && bytes.size <= 6 * 1024 * 1024) { "Imagem inválida." }
        val bounds = SafeImageFormats.decodeBoundsSafely(bytes)
        require(bounds.outWidth in 180..32768 && bounds.outHeight in 120..32768) { "Imagem inválida." }
        var sample = 1
        while (bounds.outWidth / sample > 1800 || bounds.outHeight / sample > 1800) sample *= 2
        return SafeImageFormats.decodeByteArraySafely(bytes, BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }) ?: error("Imagem inválida.")
    }

    suspend fun bitmap(url: String): Bitmap = withContext(Dispatchers.IO) {
        val upgraded = upgradeArtworkUrl(url)
        require(allowedBannerSource(upgraded) || allowedBannerSource(url)) { "Imagem não permitida." }
        val bytes = try {
            if (allowedBannerSource(upgraded)) read(upgraded, 6L * 1024 * 1024) else read(url, 6L * 1024 * 1024)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            read(url, 6L * 1024 * 1024)
        }
        decodeForBannerCrop(bytes)
    }

    suspend fun bitmapFromBannerValue(bannerValue: String): Bitmap = withContext(Dispatchers.IO) {
        val trimmed = bannerValue.trim()
        if (trimmed.startsWith("data:image/", ignoreCase = true) && trimmed.contains(";base64,")) {
            val base64Part = trimmed.substringAfter(";base64,")
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            decodeForBannerCrop(bytes)
        } else {
            bitmap(trimmed)
        }
    }
}

