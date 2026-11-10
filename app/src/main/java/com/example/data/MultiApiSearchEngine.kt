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
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
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

open class MultiApiSearchEngine {
    companion object {
        val ipv4FirstDns = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> = try { Dns.SYSTEM.lookup(hostname).sortedBy { it is Inet6Address } }
                catch (_: Throwable) { try { Dns.SYSTEM.lookup(hostname) } catch (_: Throwable) { emptyList() } }
        }
        val fastImageAndApiDispatcher = Dispatcher().apply { maxRequests = 64; maxRequestsPerHost = 24 }
        private val defaultHttpClient: OkHttpClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            OkHttpClient.Builder().dns(ipv4FirstDns).dispatcher(fastImageAndApiDispatcher).addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()
                if (original.header("User-Agent") == null) {
                    builder.header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                }
                val host = original.url.host
                if (host.contains("comicvine") || host.contains("gamespot.com")) {
                    builder.header("Referer", "https://comicvine.gamespot.com/")
                }
                val accept = original.header("Accept")
                if (accept == null || accept == "*/*" || accept == "image/*") {
                    builder.header("Accept", SafeImageFormats.ACCEPT_RASTER_HEADER)
                }
                chain.proceed(builder.build())
            }.connectTimeout(6000, TimeUnit.MILLISECONDS).readTimeout(8000, TimeUnit.MILLISECONDS).build()
        }
        @Volatile private var customHttpClient: OkHttpClient? = null
        var sharedHttpClient: OkHttpClient
            get() = customHttpClient ?: defaultHttpClient
            set(value) { customHttpClient = value }
        const val HTTP_CACHE_BUDGET_BYTES = 8L * 1024 * 1024
        @Volatile private var isCacheInitialized = false
        @Synchronized fun initHttpCache(context: android.content.Context) {
            if (isCacheInitialized) return
            try {
                // Legacy file removal belongs to CacheMaintenance's IO migration.
                // MultiApiSearchEngine já possui cache LRU em memória (48 requisições completas).
                // Não adicionamos cache OkHttp em disco para buscas e navegação para evitar inchaço progressivo.
                isCacheInitialized = true
            } catch (_: Throwable) { }
        }
    }

    private val cache = object : LinkedHashMap<String, List<ComicEntry>>(48, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<ComicEntry>>?) = size > 48
    }
    /**
     * Chave de cache que distingue idioma, provedor e capas HD. Antes o prefixo era descartado
     * na leitura, então trocar de idioma/provedor reaproveitava resultados de outra configuração.
     */
    fun searchCacheKey(query: String, searchLanguage: String, provider: String, hdCovers: Boolean): String {
        val countryCode = if (searchLanguage == "PT") "BR" else "US"
        return "$provider:$countryCode:hd$hdCovers:${query.trim()}".lowercase()
    }
    fun getCached(key: String): List<ComicEntry>? {
        val match = Regex("^(multi|books|comics|manga):(br|us):hd(true|false):(.*)$", RegexOption.IGNORE_CASE).matchEntire(key.trim())
        val resolved = if (match == null) searchCacheKey(key, "ALL", "MULTI", true) else "${match.groupValues[1].lowercase()}:${match.groupValues[2].lowercase()}:hd${match.groupValues[3].lowercase()}:${match.groupValues[4].trim().lowercase()}"
        return synchronized(cache) { cache[resolved] }
    }
    fun updateCachedComicTranslation(comic: ComicEntry) { synchronized(cache) { cache.keys.toList().forEach { key -> cache[key] = cache[key].orEmpty().map { if (it.id == comic.id) comic else it } } } }
    suspend fun resolveBestCoverUrl(title: String, authors: String = ""): String {
        val key = PortugueseComicTranslator.canonicalWorkKey(title)
        return try { ComicVineCatalog.search(title).items.firstOrNull { PortugueseComicTranslator.canonicalWorkKey(it.displayTitle) == key }?.coverUrl.orEmpty() }
        catch (cancel: CancellationException) { throw cancel } catch (_: Exception) { "" }
    }
    suspend fun fetchSequenceIssuesOnline(comic: ComicEntry, hdCovers: Boolean = true): List<ComicEntry> = try { ComicVineCatalog.resolve(comic)?.issues.orEmpty() }
        catch (cancel: CancellationException) { throw cancel } catch (_: Exception) { emptyList() }
    suspend fun prewarmPopularSearches(context: android.content.Context? = null) {
        try {
            ModernCatalog.load(context = context)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
        }
    }
    open fun searchOnlineProgressive(query: String, searchLanguage: String = "ALL", provider: String = "MULTI", hdCovers: Boolean = true): Flow<List<ComicEntry>> = channelFlow {
        val literal = query.trim().take(100)
        if (literal.length < 2) { send(emptyList()); return@channelFlow }
        val key = searchCacheKey(literal, searchLanguage, provider, hdCovers)
        val saved = synchronized(cache) { cache[key] }
        if (saved != null) { send(saved); return@channelFlow }
        val variants = CatalogSearchPolicy.searchTerms(literal, ModernCatalog.current.map { it.displayTitle }).take(3)
        val mutex = Mutex(); val providers = Semaphore(4)
        var pool = emptyList<ComicEntry>()
        var emitted = emptyList<ComicEntry>()
        suspend fun accept(batch: List<ComicEntry>) {
            if (batch.isEmpty()) return
            mutex.withLock {
                pool = CatalogSearchPolicy.unique(pool + batch.map(AdditionalCatalogSources::localize)).take(120)
                emitted = CatalogSearchPolicy.ranked(pool, variants, searchLanguage).take(60)
                ModernCatalog.accept(emitted); send(emitted)
            }
        }
        supervisorScope {
            AdditionalCatalogSources.enabledSources(provider, searchLanguage).forEach { source -> launch(Dispatchers.IO) {
                val sourceTerms = if (source in setOf("apple-br", "google-pt")) variants.take(2) else variants
                for (term in sourceTerms) providers.withPermit {
                    try { withTimeoutOrNull(5500L) { AdditionalCatalogSources.load(source, term, searchLanguage, page = 0) }?.let { accept(it) } }
                    catch (cancel: CancellationException) { throw cancel } catch (_: Exception) { }
                }
            } }
        }
        if (emitted.isEmpty()) send(emptyList()) else synchronized(cache) { cache[key] = emitted }
    }
    data class CatalogPage(val items: List<ComicEntry>, val hasMore: Boolean)
    suspend fun fetchCatalogPage(page: Int, favoriteUniverse: String): CatalogPage {
        val primary = runCatching { ComicVineCatalog.catalog(page) }.getOrNull()
        val extraTopic = when {
            favoriteUniverse.contains("Marvel", ignoreCase = true) -> "Spider-Man"
            favoriteUniverse.contains("Mangá", ignoreCase = true) -> "Manga"
            favoriteUniverse.contains("Image", ignoreCase = true) -> "Invincible"
            else -> "Batman"
        }
        val extra = if (page <= 2) {
            runCatching { withTimeoutOrNull(3500L) { PortugueseBookSources.googlePortuguese(extraTopic, page = page).map(AdditionalCatalogSources::localize) } }
                .getOrNull().orEmpty()
        } else emptyList()
        val combined = CatalogSearchPolicy.unique((primary?.items.orEmpty().map(AdditionalCatalogSources::localize)) + extra)
        val hasMore = (primary?.hasMore == true) || extra.isNotEmpty()
        return CatalogPage(combined, hasMore).also { ModernCatalog.accept(it.items) }
    }
    suspend fun runOnlineApiDiagnostics(): ApiDiagnosticsReport {
        val start = System.currentTimeMillis()
        val ok = try { ComicVineCatalog.catalog(0); true } catch (cancel: CancellationException) { throw cancel } catch (_: Exception) { false }
        val elapsed = System.currentTimeMillis() - start
        return ApiDiagnosticsReport(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()), elapsed, listOf(ApiEndpointStatus("Catálogo", ok, elapsed)))
    }
}
