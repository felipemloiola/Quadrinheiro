package com.example.data

import com.example.model.ApiDiagnosticsReport
import com.example.model.ApiEndpointStatus
import com.example.model.ComicEntry
import com.example.model.ReadingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Dispatcher
import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class MultiApiSearchEngine {

    companion object {
        private const val API_TIMEOUT_MS = 3500L

        val ipv4FirstDns = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                return try {
                    Dns.SYSTEM.lookup(hostname).sortedBy { it is Inet6Address }
                } catch (_: Throwable) {
                    try {
                        Dns.SYSTEM.lookup(hostname)
                    } catch (_: Throwable) {
                        emptyList()
                    }
                }
            }
        }

        val fastImageAndApiDispatcher = Dispatcher().apply {
            maxRequests = 64
            maxRequestsPerHost = 24
        }

        private val defaultHttpClient: OkHttpClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            OkHttpClient.Builder()
                .dns(ipv4FirstDns)
                .dispatcher(fastImageAndApiDispatcher)
                .addInterceptor { chain ->
                    val orig = chain.request()
                    val reqBuilder = orig.newBuilder()
                    if (orig.header("User-Agent") == null) {
                        reqBuilder.header(
                            "User-Agent",
                            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_4) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15"
                        )
                    }
                    chain.proceed(reqBuilder.build())
                }
                .connectTimeout(6000, TimeUnit.MILLISECONDS)
                .readTimeout(8000, TimeUnit.MILLISECONDS)
                .build()
        }

        @Volatile
        private var customHttpClient: OkHttpClient? = null

        var sharedHttpClient: OkHttpClient
            get() = customHttpClient ?: defaultHttpClient
            set(value) {
                customHttpClient = value
            }

        @Volatile
        private var isCacheInitialized = false

        @Synchronized
        fun initHttpCache(context: android.content.Context) {
            if (isCacheInitialized) return
            isCacheInitialized = true
            try {
                val httpCacheDir = java.io.File(context.cacheDir, "quadrinheiro_http_cache")
                val httpCache = okhttp3.Cache(httpCacheDir, 16L * 1024L * 1024L)
                sharedHttpClient = sharedHttpClient.newBuilder()
                    .cache(httpCache)
                    .build()
            } catch (_: Throwable) {}
        }
    }


    private val cache=object:LinkedHashMap<String,List<ComicEntry>>(48,.75f,true){override fun removeEldestEntry(eldest:MutableMap.MutableEntry<String,List<ComicEntry>>?)=size>48}

    /**
     * Chave de cache que distingue idioma, provedor e capas HD. Antes o prefixo era descartado
     * na leitura, então trocar de idioma/provedor reaproveitava resultados de outra configuração.
     */
    fun searchCacheKey(query:String,searchLanguage:String,provider:String,hdCovers:Boolean):String{
        val countryCode=if(searchLanguage=="PT")"BR" else "US"
        return "$provider:$countryCode:hd$hdCovers:${query.trim()}".lowercase()
    }

    fun getCached(key:String):List<ComicEntry>?=synchronized(cache){cache[key.lowercase()]}
    fun updateCachedComicTranslation(comic:ComicEntry){ synchronized(cache){cache.keys.toList().forEach { key -> cache[key]=cache[key].orEmpty().map { if(it.id==comic.id) comic else it } }} }
    suspend fun resolveBestCoverUrl(title:String,authors:String=""):String {
        val key=PortugueseComicTranslator.canonicalWorkKey(title)
        return runCatching { ComicVineCatalog.search(title).items.firstOrNull { PortugueseComicTranslator.canonicalWorkKey(it.displayTitle)==key }?.coverUrl.orEmpty() }.getOrDefault("")
    }
    suspend fun fetchSequenceIssuesOnline(comic:ComicEntry,hdCovers:Boolean=true):List<ComicEntry> = runCatching { ComicVineCatalog.resolve(comic)?.issues.orEmpty() }.getOrDefault(emptyList())
    suspend fun prewarmPopularSearches(){try{ModernCatalog.load()}catch(cancel:kotlinx.coroutines.CancellationException){throw cancel}catch(_:Exception){}}
    fun searchOnlineProgressive(query:String,searchLanguage:String="ALL",provider:String="MULTI",hdCovers:Boolean=true):Flow<List<ComicEntry>> = kotlinx.coroutines.flow.flow {
        val q=query.trim();if(q.length<2){emit(emptyList());return@flow}
        val key=searchCacheKey(q,searchLanguage,provider,hdCovers)
        val cached=synchronized(cache){cache[key]};if(cached!=null){emit(cached);return@flow}
        val items=ComicVineCatalog.search(q).items.map { PortugueseComicTranslator.localizeComicMetadataOnly(it) }
        synchronized(cache){cache[key]=items};ModernCatalog.accept(items);emit(items)
    }.flowOn(Dispatchers.IO)
    data class CatalogPage(val items:List<ComicEntry>,val hasMore:Boolean)
    suspend fun fetchCatalogPage(page:Int,favoriteUniverse:String):CatalogPage=ComicVineCatalog.catalog(page).also { ModernCatalog.accept(it.items) }
    suspend fun runOnlineApiDiagnostics():ApiDiagnosticsReport {
        val start=System.currentTimeMillis();val ok=runCatching{ComicVineCatalog.catalog(0)}.isSuccess
        return ApiDiagnosticsReport(testedAt=SimpleDateFormat("HH:mm",Locale.getDefault()).format(Date()),totalMs=System.currentTimeMillis()-start,endpoints=listOf(ApiEndpointStatus("Catálogo",ok,System.currentTimeMillis()-start)))
    }
}
