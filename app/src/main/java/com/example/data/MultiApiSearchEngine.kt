package com.example.data

import com.example.model.ApiDiagnosticsReport
import com.example.model.ApiEndpointStatus
import com.example.model.ComicEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Dispatcher
import okhttp3.Dns
import okhttp3.OkHttpClient
import java.net.Inet6Address
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MultiApiSearchEngine {
    companion object {
        val ipv4FirstDns = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> = try {
                Dns.SYSTEM.lookup(hostname).sortedBy { it is Inet6Address }
            } catch (_: Throwable) { try { Dns.SYSTEM.lookup(hostname) } catch (_: Throwable) { emptyList() } }
        }
        val fastImageAndApiDispatcher = Dispatcher().apply { maxRequests = 64; maxRequestsPerHost = 24 }
        private val defaultHttpClient: OkHttpClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            OkHttpClient.Builder().dns(ipv4FirstDns).dispatcher(fastImageAndApiDispatcher)
                .addInterceptor { chain ->
                    val original = chain.request()
                    val request = if (original.header("User-Agent") == null) original.newBuilder()
                        .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_4) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15").build() else original
                    chain.proceed(request)
                }.connectTimeout(6000, TimeUnit.MILLISECONDS).readTimeout(8000, TimeUnit.MILLISECONDS).build()
        }
        @Volatile private var customHttpClient: OkHttpClient? = null
        var sharedHttpClient: OkHttpClient
            get() = customHttpClient ?: defaultHttpClient
            set(value) { customHttpClient = value }
        @Volatile private var isCacheInitialized = false
        @Synchronized fun initHttpCache(context: android.content.Context) {
            if (isCacheInitialized) return
            try {
                val cache = okhttp3.Cache(java.io.File(context.cacheDir, "quadrinheiro_http_cache"), 16L * 1024 * 1024)
                sharedHttpClient = sharedHttpClient.newBuilder().cache(cache).build()
                isCacheInitialized = true
            } catch (_: Throwable) { }
        }
    }

    private val cache = object : LinkedHashMap<String, List<ComicEntry>>(48, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<ComicEntry>>?) = size > 48
    }
    private fun cacheKey(query: String, language: String, provider: String, hd: Boolean) =
        "$provider:${if (language == "PT") "BR" else "US"}:hd$hd:${CatalogSearchPolicy.normalize(query)}"
    fun getCached(key: String): List<ComicEntry>? {
        val match = Regex("^(MULTI|BOOKS|MANGA):(BR|US):hd(true|false):(.*)$").matchEntire(key)
        val resolved = if (match == null) cacheKey(key, "ALL", "MULTI", true) else
            "${match.groupValues[1]}:${match.groupValues[2]}:hd${match.groupValues[3]}:${CatalogSearchPolicy.normalize(match.groupValues[4])}"
        return synchronized(cache) { cache[resolved] }
    }
    fun updateCachedComicTranslation(comic: ComicEntry) {
        synchronized(cache) { cache.keys.toList().forEach { key -> cache[key] = cache[key].orEmpty().map { if (it.id == comic.id) comic else it } } }
    }
    suspend fun resolveBestCoverUrl(title: String, authors: String = ""): String {
        val key = PortugueseComicTranslator.canonicalWorkKey(title)
        return try { ComicVineCatalog.search(title).items.firstOrNull { PortugueseComicTranslator.canonicalWorkKey(it.displayTitle) == key }?.coverUrl.orEmpty() }
        catch (cancel: CancellationException) { throw cancel } catch (_: Exception) { "" }
    }
    suspend fun fetchSequenceIssuesOnline(comic: ComicEntry, hdCovers: Boolean = true): List<ComicEntry> =
        try { ComicVineCatalog.resolve(comic)?.issues.orEmpty() } catch (cancel: CancellationException) { throw cancel } catch (_: Exception) { emptyList() }
    suspend fun prewarmPopularSearches() { try { ModernCatalog.load() } catch (cancel: CancellationException) { throw cancel } catch (_: Exception) { } }

    fun searchOnlineProgressive(query: String, searchLanguage: String = "ALL", provider: String = "MULTI", hdCovers: Boolean = true): Flow<List<ComicEntry>> = channelFlow {
        val literal = query.trim().take(100)
        if (literal.length < 2) { send(emptyList()); return@channelFlow }
        val key = cacheKey(literal, searchLanguage, provider, hdCovers)
        val cached = synchronized(cache) { cache[key] }
        if (cached != null) { send(cached); return@channelFlow }
        val variants = CatalogSearchPolicy.variants(literal, ModernCatalog.current.map { it.displayTitle })
        val mutex = Mutex()
        var merged = emptyList<ComicEntry>()
        suspend fun accept(batch: List<ComicEntry>) {
            if (batch.isEmpty()) return
            mutex.withLock {
                merged = CatalogSearchPolicy.unique(merged + batch.map { PortugueseComicTranslator.localizeComicMetadataOnly(it) }).take(48)
                ModernCatalog.accept(merged)
                send(merged)
            }
        }
        val loaders = mutableListOf<suspend (String) -> List<ComicEntry>>({ term -> ComicVineCatalog.search(term).items })
        if (provider != "MANGA") {
            loaders.add { term -> PortugueseBookSources.appleBrazil(term) }
            loaders.add { term -> PortugueseBookSources.googlePortuguese(term) }
        }
        supervisorScope {
            loaders.forEach { load -> launch(Dispatchers.IO) {
                for (term in variants) {
                    try { withTimeoutOrNull(5000L) { load(term) }?.let { accept(it) } }
                    catch (cancel: CancellationException) { throw cancel } catch (_: Exception) { }
                }
            } }
        }
        if (merged.isEmpty()) send(emptyList())
        else synchronized(cache) { cache[key] = merged }
    }
    data class CatalogPage(val items: List<ComicEntry>, val hasMore: Boolean)
    suspend fun fetchCatalogPage(page: Int, favoriteUniverse: String): CatalogPage = ComicVineCatalog.catalog(page).also { ModernCatalog.accept(it.items) }
    suspend fun runOnlineApiDiagnostics(): ApiDiagnosticsReport {
        val start = System.currentTimeMillis()
        val ok = try { ComicVineCatalog.catalog(0); true } catch (cancel: CancellationException) { throw cancel } catch (_: Exception) { false }
        val elapsed = System.currentTimeMillis() - start
        return ApiDiagnosticsReport(testedAt = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()), totalMs = elapsed,
            endpoints = listOf(ApiEndpointStatus("Catálogo", ok, elapsed)))
    }
}
