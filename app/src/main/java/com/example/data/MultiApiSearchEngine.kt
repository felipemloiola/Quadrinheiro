package com.example.data

import com.example.model.ApiDiagnosticsReport
import com.example.model.ApiEndpointStatus
import com.example.model.ComicEntry
import com.example.model.ReadingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

        var sharedHttpClient: OkHttpClient = OkHttpClient.Builder()
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

        @Volatile
        private var isCacheInitialized = false

        @Synchronized
        fun initHttpCache(context: android.content.Context) {
            if (isCacheInitialized) return
            isCacheInitialized = true
            try {
                val httpCacheDir = java.io.File(context.cacheDir, "quadrinheiro_http_cache")
                val httpCache = okhttp3.Cache(httpCacheDir, 120L * 1024L * 1024L)
                sharedHttpClient = sharedHttpClient.newBuilder()
                    .cache(httpCache)
                    .build()
            } catch (_: Throwable) {}
        }
    }

    private val httpClient: OkHttpClient get() = sharedHttpClient

    private val onlineSearchCache = ConcurrentHashMap<String, List<ComicEntry>>()

    private fun Request.Builder.withBrowserHeaders(): Request.Builder = this
        .header(
            "User-Agent",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_4) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15"
        )
        .header("Accept", "application/json")

    fun getCached(cacheKey: String): List<ComicEntry>? = onlineSearchCache[cacheKey]

    fun updateCachedComicTranslation(translated: ComicEntry) {
        if (translated.synopsisPtBr.isBlank() && translated.titlePtBr.isBlank()) return
        onlineSearchCache.keys.forEach { k ->
            val current = onlineSearchCache[k] ?: return@forEach
            var changed = false
            val updated = current.map { item ->
                if ((translated.id.isNotBlank() && item.id == translated.id) ||
                    (translated.externalId.isNotBlank() && item.externalId == translated.externalId)
                ) {
                    changed = true
                    item.copy(
                        title = translated.titlePtBr.ifBlank { item.title },
                        titlePtBr = translated.titlePtBr.ifBlank { item.titlePtBr },
                        synopsis = translated.synopsisPtBr.ifBlank { item.synopsis },
                        synopsisPtBr = translated.synopsisPtBr.ifBlank { item.synopsisPtBr }
                    )
                } else {
                    item
                }
            }
            if (changed) {
                onlineSearchCache[k] = updated
            }
        }
    }

    suspend fun resolveBestCoverUrl(title: String, authors: String = ""): String = withContext(Dispatchers.IO) {
        val cleanTitle = title.substringBefore("—").substringBefore("(").trim()
        val query = if (authors.isNotBlank()) "$cleanTitle ${authors.substringBefore(",")}" else cleanTitle
        val enc = URLEncoder.encode(query, "UTF-8")
        val appleBr = fetchAppleBooks(enc, "BR", true).firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl
        if (!appleBr.isNullOrBlank()) return@withContext appleBr
        val appleUs = fetchAppleBooks(enc, "US", true).firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl
        if (!appleUs.isNullOrBlank()) return@withContext appleUs
        val kitsu = fetchKitsuManga(URLEncoder.encode(cleanTitle, "UTF-8")).firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl
        if (!kitsu.isNullOrBlank()) return@withContext kitsu
        val google = fetchGoogleBooks(enc, "pt").firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl
        google ?: ""
    }

    private val sequenceCache = ConcurrentHashMap<String, List<ComicEntry>>()

    suspend fun fetchSequenceIssuesOnline(comic: ComicEntry, hdCovers: Boolean = true): List<ComicEntry> = withContext(Dispatchers.IO) {
        val baseSeries = SeriesSequenceEngine.inferBaseSeriesTitle(comic)
        val primaryAuthor = SeriesSequenceEngine.extractPrimaryAuthor(comic.authors)
        if (baseSeries.length < 2) return@withContext emptyList()
        val seqKey = SeriesSequenceEngine.buildSequenceKey(comic)
        sequenceCache[seqKey]?.let { return@withContext it }

        val query = if (primaryAuthor.isNotBlank() && !primaryAuthor.equals("Autor Desconhecido", ignoreCase = true)) {
            "$baseSeries $primaryAuthor"
        } else {
            baseSeries
        }
        val enc = URLEncoder.encode(query, "UTF-8")
        val combined = coroutineScope {
            val brJob = async { fetchAppleBooks(enc, "BR", hdCovers) }
            val usJob = async { fetchAppleBooks(enc, "US", hdCovers) }
            val gbJob = async { fetchGoogleBooks(enc, "pt") }
            (brJob.await() + usJob.await() + gbJob.await())
        }
        val uniqueMap = LinkedHashMap<String, ComicEntry>()
        for (candidate in combined) {
            if (!SeriesSequenceEngine.isSameSequence(comic, candidate)) continue
            val dedupKey = SeriesSequenceEngine.canonicalDeduplicationKey(candidate)
            if (!uniqueMap.containsKey(dedupKey)) {
                uniqueMap[dedupKey] = candidate
            }
        }
        val filtered = uniqueMap.values.toList()
        sequenceCache[seqKey] = filtered
        filtered
    }

    suspend fun prewarmPopularSearches() = withContext(Dispatchers.IO) {
        val popular = CuratedCatalog.quickSearchSuggestions.take(6)
        coroutineScope {
            popular.forEach { term ->
                launch {
                    val tLower = term.lowercase()
                    val keyUsHd = "MULTI:US:hdtrue:$tLower"
                    if (!onlineSearchCache.containsKey(keyUsHd)) {
                        val enc = URLEncoder.encode(term, "UTF-8")
                        val appleBrJob = async { fetchAppleBooks(enc, "BR", true) }
                        val appleUsJob = async { fetchAppleBooks(enc, "US", true) }
                        val kitsuJob = async { fetchKitsuManga(enc) }
                        val rawList = appleBrJob.await() + appleUsJob.await() + kitsuJob.await()
                        val dedupMap = LinkedHashMap<String, ComicEntry>()
                        for (item in rawList) {
                            val key = SeriesSequenceEngine.canonicalDeduplicationKey(item)
                            if (!dedupMap.containsKey(key)) {
                                dedupMap[key] = item
                            }
                        }
                        val combined = dedupMap.values.toList()
                        if (combined.isNotEmpty()) {
                            onlineSearchCache[keyUsHd] = combined
                            onlineSearchCache["MULTI:BR:hdtrue:$tLower"] = combined
                            onlineSearchCache["MULTI:US:hdfalse:$tLower"] = combined
                            onlineSearchCache["MULTI:BR:hdfalse:$tLower"] = combined
                        }
                    }
                }
            }
        }
    }

    fun searchOnlineProgressive(
        query: String,
        provider: String, // "MULTI" | "BOOKS" | "MANGA"
        searchLanguage: String, // "ALL" | "PT"
        hdCovers: Boolean = true
    ): Flow<List<ComicEntry>> = channelFlow {
        val q = query.trim()
        if (q.isEmpty()) {
            send(emptyList())
            return@channelFlow
        }

        val countryCode = if (searchLanguage == "PT") "BR" else "US"
        val cacheKey = "$provider:$countryCode:hd$hdCovers:${q.lowercase()}"

        onlineSearchCache[cacheKey]?.let { cached ->
            send(cached)
            return@channelFlow
        }

        val mapByCanonicalKey = LinkedHashMap<String, ComicEntry>()

        @Synchronized
        fun pushProgressiveBatch(newBatch: List<ComicEntry>): List<ComicEntry> {
            for (rawItem in newBatch) {
                val it = PortugueseComicTranslator.localizeComicMetadataOnly(rawItem)
                val dedupKey = SeriesSequenceEngine.canonicalDeduplicationKey(it)
                val existing = mapByCanonicalKey[dedupKey]
                if (existing == null) {
                    mapByCanonicalKey[dedupKey] = it
                } else {
                    // Prefere a edição com capa melhor ou que veio do catálogo BR
                    val existingHasGoodCover = existing.coverUrl.isNotBlank() && !existing.coverUrl.contains("covers.openlibrary.org")
                    val candidateHasGoodCover = it.coverUrl.isNotBlank() && !it.coverUrl.contains("covers.openlibrary.org")
                    if (!existingHasGoodCover && candidateHasGoodCover) {
                        mapByCanonicalKey[dedupKey] = it
                    }
                }
            }
            val sorted = mapByCanonicalKey.values.sortedByDescending {
                val coverScore = when {
                    it.coverUrl.isBlank() -> 0
                    it.coverUrl.contains("covers.openlibrary.org") -> 1
                    else -> 3
                }
                val brBonus = if (it.externalId.startsWith("ab-BR-")) 1 else 0
                coverScore + brBonus + (if (it.synopsis.isNotBlank()) 1 else 0)
            }
            if (sorted.isNotEmpty()) {
                onlineSearchCache[cacheKey] = sorted
            }
            return sorted
        }

        val encoded = withContext(Dispatchers.IO) {
            URLEncoder.encode(q, "UTF-8")
        }

        coroutineScope {
            // 1. Apple Books BR (Catálogo brasileiro com títulos e capas em português)
            launch(Dispatchers.IO) {
                val batch = fetchAppleBooks(encoded, "BR", hdCovers)
                if (batch.isNotEmpty()) {
                    send(pushProgressiveBatch(batch))
                }
            }

            // 2. Apple Books US (Catálogo mundial amplo: Marvel, DC, Image, Mangás - traduzido para PT-BR)
            launch(Dispatchers.IO) {
                val batch = fetchAppleBooks(encoded, "US", hdCovers)
                if (batch.isNotEmpty()) {
                    send(pushProgressiveBatch(batch))
                }
            }

            // 3. Kitsu Manga & Comics API (Ultra-rápido, sem autenticação, capas em CDN)
            launch(Dispatchers.IO) {
                val batch = fetchKitsuManga(encoded)
                if (batch.isNotEmpty()) {
                    send(pushProgressiveBatch(batch))
                }
            }

            // 4. MangaDex API (Banco de dados rico de mangás com títulos em múltiplos idiomas)
            launch(Dispatchers.IO) {
                val batch = fetchMangaDex(encoded)
                if (batch.isNotEmpty()) {
                    send(pushProgressiveBatch(batch))
                }
            }

            // 5. Open Library API (Edições ocidentais, graphic novels e encadernados)
            launch(Dispatchers.IO) {
                val batch = fetchOpenLibrary(encoded, hdCovers)
                if (batch.isNotEmpty()) {
                    send(pushProgressiveBatch(batch))
                }
            }

            // 6. Google Books API (Priorizando idioma português)
            launch(Dispatchers.IO) {
                val batch = fetchGoogleBooks(encoded, "pt")
                if (batch.isNotEmpty()) {
                    send(pushProgressiveBatch(batch))
                }
            }
        }

        // Se nenhuma API remota tiver retornado resultados (ou offline),
        // busca instantaneamente no catálogo curado para nunca deixar a busca sem retorno
        if (mapByCanonicalKey.isEmpty()) {
            val qLower = q.lowercase()
            val localFallback = CuratedCatalog.curatedComicsCatalog.filter {
                it.title.lowercase().contains(qLower) ||
                    it.authors.lowercase().contains(qLower) ||
                    it.publisher.lowercase().contains(qLower) ||
                    it.genre.lowercase().contains(qLower)
            }
            if (localFallback.isNotEmpty()) {
                send(pushProgressiveBatch(localFallback))
            }
        }
    }

    private fun fetchAppleBooks(encoded: String, countryCode: String, hdCovers: Boolean = true): List<ComicEntry> {
        return try {
            val url = "https://itunes.apple.com/search?term=$encoded&media=ebook&entity=ebook&country=$countryCode&limit=12"
            val req = Request.Builder().url(url).withBrowserHeaders().get().build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val body = resp.body?.string() ?: return emptyList()
                val results = JSONObject(body).optJSONArray("results") ?: return emptyList()
                val out = mutableListOf<ComicEntry>()
                for (i in 0 until results.length()) {
                    val item = results.optJSONObject(i) ?: continue
                    val rawTitle = item.optString("trackName", item.optString("collectionName", "Sem Título")).trim()
                    val desc = item.optString("description", "")
                        .replace(Regex("<[^>]*>?"), " ")
                        .replace("&nbsp;", " ")
                        .replace("&amp;", "&")
                        .replace("&quot;", "\"")
                        .replace(Regex("\\s+"), " ")
                        .trim()

                    // Filtra edições em alemão/francês/boxes repetidos que poluem a busca
                    if (PortugueseComicTranslator.isUnwantedForeignOrBundleResult(rawTitle, desc)) {
                        continue
                    }

                    val authors = item.optString("artistName", "Autor Desconhecido")
                    val rawArt = item.optString("artworkUrl100", item.optString("artworkUrl60", ""))
                    val targetRes = if (hdCovers) "600x600bb" else "300x300bb"
                    val coverUrl = if (rawArt.isNotBlank()) {
                        rawArt.replace("http://", "https://")
                            .replace("100x100bb", targetRes)
                            .replace("60x60bb", targetRes)
                    } else ""
                    val year = Regex("\\d{4}").find(item.optString("releaseDate", ""))?.value?.toIntOrNull() ?: 2026
                    val genresArr = item.optJSONArray("genres")
                    val genreStr = buildString {
                        if (genresArr != null) {
                            for (g in 0 until genresArr.length()) append(genresArr.optString(g)).append(" ")
                        }
                    }.lowercase()

                    val rawSeller = item.optString("sellerName", "").trim()
                    val ptTitle = PortugueseComicTranslator.translateTitleToPtBr(rawTitle)
                    val genre = PortugueseComicTranslator.normalizeGenre(genreStr, ptTitle, rawSeller, authors)
                    val publisher = PortugueseComicTranslator.normalizePublisher(rawSeller, ptTitle, authors, genre)
                    val (format, pageCount) = PortugueseComicTranslator.normalizeFormatAndPages("", 0, ptTitle, genre)
                    val issueNum = SeriesSequenceEngine.inferIssueNumber(ptTitle)
                    val trackId = item.optLong("trackId", item.optLong("collectionId", i.toLong()))
                    val uniqueExternalId = "ab-$countryCode-$trackId"
                    val rawEntry = ComicEntry(
                        id = uniqueExternalId,
                        externalId = uniqueExternalId,
                        sourceLabel = "Catálogo Online",
                        isOnlineResult = true,
                        title = ptTitle,
                        titlePtBr = ptTitle,
                        authors = authors,
                        publisher = publisher,
                        genre = genre,
                        format = format,
                        releaseYear = year,
                        pageCount = pageCount,
                        currentPage = pageCount,
                        coverUrl = coverUrl,
                        synopsis = desc,
                        status = ReadingStatus.READ,
                        seriesName = SeriesSequenceEngine.inferBaseSeriesTitle(ptTitle),
                        issueNumber = issueNum
                    )
                    out.add(PortugueseComicTranslator.localizeComicMetadataOnly(rawEntry))
                }
                out
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun fetchGoogleBooks(encoded: String, langRestrict: String): List<ComicEntry> {
        return try {
            val url = "https://www.googleapis.com/books/v1/volumes?q=$encoded&maxResults=10&printType=books&langRestrict=$langRestrict"
            val req = Request.Builder().url(url).withBrowserHeaders().get().build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val body = resp.body?.string() ?: return emptyList()
                val items = JSONObject(body).optJSONArray("items") ?: return emptyList()
                val out = mutableListOf<ComicEntry>()
                for (i in 0 until items.length()) {
                    val item = items.optJSONObject(i) ?: continue
                    val vol = item.optJSONObject("volumeInfo") ?: continue
                    val rawTitle = vol.optString("title", "Sem Título").trim()
                    val sub = vol.optString("subtitle", "").trim()
                    val fullTitle = if (sub.isNotBlank() && !rawTitle.lowercase().contains(sub.lowercase())) {
                        "$rawTitle: $sub"
                    } else rawTitle

                    val desc = vol.optString("description", "")
                        .replace(Regex("<[^>]*>?"), " ")
                        .replace(Regex("\\s+"), " ")
                        .trim()

                    if (PortugueseComicTranslator.isUnwantedForeignOrBundleResult(fullTitle, desc)) {
                        continue
                    }

                    val authorsArr = vol.optJSONArray("authors")
                    val authors = buildList {
                        if (authorsArr != null) {
                            for (a in 0 until authorsArr.length()) add(authorsArr.optString(a))
                        }
                    }.ifEmpty { listOf("Autor Desconhecido") }.joinToString(", ")

                    val rawPublisher = vol.optString("publisher", "").trim()
                    val year = Regex("\\d{4}").find(vol.optString("publishedDate", ""))?.value?.toIntOrNull() ?: 2026
                    val rawPages = vol.optInt("pageCount", 0)

                    val ptTitle = PortugueseComicTranslator.translateTitleToPtBr(fullTitle)
                    val genre = PortugueseComicTranslator.normalizeGenre("Graphic Novel", ptTitle, rawPublisher, authors)
                    val publisher = PortugueseComicTranslator.normalizePublisher(rawPublisher, ptTitle, authors, genre)
                    val (format, pages) = PortugueseComicTranslator.normalizeFormatAndPages("", rawPages, ptTitle, genre)
                    val issueNum = SeriesSequenceEngine.inferIssueNumber(ptTitle)
                    val imgLinks = vol.optJSONObject("imageLinks")
                    val coverUrl = (imgLinks?.optString("thumbnail", "")?.takeIf { it.isNotBlank() }
                        ?: imgLinks?.optString("smallThumbnail", "") ?: "")
                        .replace("http://", "https://")
                        .replace("&edge=curl", "")

                    val gId = item.optString("id", "$i")
                    val uniqueExternalId = "gb-$gId"
                    val rawEntry = ComicEntry(
                        id = uniqueExternalId,
                        externalId = uniqueExternalId,
                        sourceLabel = "Catálogo Online",
                        isOnlineResult = true,
                        title = ptTitle,
                        titlePtBr = ptTitle,
                        authors = authors,
                        publisher = publisher,
                        genre = genre,
                        format = format,
                        releaseYear = year,
                        pageCount = pages,
                        currentPage = pages,
                        coverUrl = coverUrl,
                        synopsis = desc,
                        status = ReadingStatus.READ,
                        seriesName = SeriesSequenceEngine.inferBaseSeriesTitle(ptTitle),
                        issueNumber = issueNum
                    )
                    out.add(PortugueseComicTranslator.localizeComicMetadataOnly(rawEntry))
                }
                out
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun fetchKitsuManga(encoded: String): List<ComicEntry> {
        return try {
            val url = "https://kitsu.io/api/edge/manga?filter[text]=$encoded&page[limit]=10"
            val req = Request.Builder().url(url).withBrowserHeaders().get().build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val body = resp.body?.string() ?: return emptyList()
                val data = JSONObject(body).optJSONArray("data") ?: return emptyList()
                val out = mutableListOf<ComicEntry>()
                for (idx in 0 until data.length()) {
                    val item = data.optJSONObject(idx) ?: continue
                    val attr = item.optJSONObject("attributes") ?: continue
                    val titlesObj = attr.optJSONObject("titles")
                    val rawTitle = titlesObj?.optString("pt_br", "")
                        ?.takeIf { it.isNotBlank() && it != "null" }
                        ?: titlesObj?.optString("en", "")
                            ?.takeIf { it.isNotBlank() && it != "null" }
                        ?: attr.optString("canonicalTitle", "Sem Título").trim()

                    val poster = attr.optJSONObject("posterImage")
                    val coverUrl = (poster?.optString("medium", "")?.takeIf { it.isNotBlank() && it != "null" }
                        ?: poster?.optString("small", "")?.takeIf { it.isNotBlank() && it != "null" }
                        ?: poster?.optString("large", "") ?: "").replace("http://", "https://")

                    val releaseYear = Regex("\\d{4}").find(attr.optString("startDate", ""))?.value?.toIntOrNull() ?: 2026
                    val chapters = attr.optInt("chapterCount", 0)
                    val pageCount = if (chapters > 0) (chapters * 20).coerceIn(140, 720) else 192
                    val rawSerialization = attr.optString("serialization", "").takeIf { it.isNotBlank() && it != "null" } ?: ""
                    val ptTitle = PortugueseComicTranslator.translateTitleToPtBr(rawTitle)
                    val publisher = PortugueseComicTranslator.normalizePublisher(rawSerialization, ptTitle, rawSerialization, "Mangá")
                    val authorLabel = rawSerialization.ifBlank { publisher }
                    val kitsuId = item.optString("id", "$idx")
                    val uniqueExternalId = "kitsu-$kitsuId"
                    val rawSynopsis = attr.optString("synopsis", "").trim()
                    val issueNum = SeriesSequenceEngine.inferIssueNumber(ptTitle)
                    val rawEntry = ComicEntry(
                        id = uniqueExternalId,
                        externalId = uniqueExternalId,
                        sourceLabel = "Catálogo de Mangás",
                        isOnlineResult = true,
                        title = ptTitle,
                        titlePtBr = ptTitle,
                        authors = authorLabel,
                        publisher = publisher,
                        genre = "Mangá",
                        format = "Mangá (Tankobon)",
                        releaseYear = releaseYear,
                        pageCount = pageCount,
                        currentPage = pageCount,
                        coverUrl = coverUrl,
                        synopsis = rawSynopsis,
                        status = ReadingStatus.READ,
                        seriesName = SeriesSequenceEngine.inferBaseSeriesTitle(ptTitle),
                        issueNumber = issueNum
                    )
                    out.add(PortugueseComicTranslator.localizeComicMetadataOnly(rawEntry))
                }
                out
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun fetchOpenLibrary(encoded: String, hdCovers: Boolean = true): List<ComicEntry> {
        return try {
            val url = "https://openlibrary.org/search.json?q=$encoded&limit=10&fields=key,title,subtitle,author_name,first_publish_year,number_of_pages_median,cover_i,publisher,isbn,first_sentence"
            val req = Request.Builder().url(url).withBrowserHeaders().get().build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val body = resp.body?.string() ?: return emptyList()
                val docs = JSONObject(body).optJSONArray("docs") ?: return emptyList()
                val out = mutableListOf<ComicEntry>()
                for (idx in 0 until docs.length()) {
                    val doc = docs.optJSONObject(idx) ?: continue
                    val rawTitle = doc.optString("title", "Sem Título").trim()
                    val rawSub = doc.optString("subtitle", "").trim()
                    val fullTitle = if (rawSub.isNotEmpty() && !rawTitle.lowercase().contains(rawSub.lowercase())) {
                        "$rawTitle: $rawSub"
                    } else rawTitle

                    val firstSentence = doc.optJSONArray("first_sentence")?.optString(0, "") ?: ""
                    if (PortugueseComicTranslator.isUnwantedForeignOrBundleResult(fullTitle, firstSentence)) {
                        continue
                    }

                    val authorArr = doc.optJSONArray("author_name")
                    val authors = buildList {
                        if (authorArr != null) {
                            for (a in 0 until minOf(3, authorArr.length())) add(authorArr.optString(a))
                        }
                    }.ifEmpty { listOf("Autor Desconhecido") }.joinToString(", ")

                    val rawPublisher = doc.optJSONArray("publisher")?.optString(0, "") ?: ""
                    val releaseYear = doc.optInt("first_publish_year", 2026)
                    val rawPages = doc.optInt("number_of_pages_median", 0)
                    val isbn = doc.optJSONArray("isbn")?.optString(0, "") ?: ""
                    val coverI = doc.optLong("cover_i", 0L)
                    val sizeSuffix = if (hdCovers) "-L.jpg" else "-M.jpg"
                    val coverUrl = when {
                        coverI > 0L -> "https://covers.openlibrary.org/b/id/$coverI$sizeSuffix"
                        isbn.isNotBlank() -> "https://covers.openlibrary.org/b/isbn/$isbn$sizeSuffix"
                        else -> ""
                    }

                    val ptTitle = PortugueseComicTranslator.translateTitleToPtBr(fullTitle)
                    val genre = PortugueseComicTranslator.normalizeGenre("Graphic Novel", ptTitle, rawPublisher, authors)
                    val publisher = PortugueseComicTranslator.normalizePublisher(rawPublisher, ptTitle, authors, genre)
                    val (format, pageCount) = PortugueseComicTranslator.normalizeFormatAndPages("", rawPages, ptTitle, genre)
                    val issueNum = SeriesSequenceEngine.inferIssueNumber(ptTitle)
                    val cleanKey = doc.optString("key", "$idx").replace("/", "-")
                    val uniqueExternalId = "ol-$cleanKey"
                    val rawEntry = ComicEntry(
                        id = uniqueExternalId,
                        externalId = uniqueExternalId,
                        sourceLabel = "Catálogo Online",
                        isOnlineResult = true,
                        title = ptTitle,
                        titlePtBr = ptTitle,
                        authors = authors,
                        publisher = publisher,
                        genre = genre,
                        format = format,
                        releaseYear = releaseYear,
                        pageCount = pageCount,
                        currentPage = pageCount,
                        isbn = isbn,
                        coverUrl = coverUrl,
                        synopsis = firstSentence,
                        status = ReadingStatus.READ,
                        seriesName = SeriesSequenceEngine.inferBaseSeriesTitle(ptTitle),
                        issueNumber = issueNum
                    )
                    out.add(PortugueseComicTranslator.localizeComicMetadataOnly(rawEntry))
                }
                out
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun fetchMangaDex(encoded: String): List<ComicEntry> {
        return try {
            val url = "https://api.mangadex.org/manga?title=$encoded&limit=8&includes[]=cover_art&includes[]=author&contentRating[]=safe&contentRating[]=suggestive"
            val req = Request.Builder().url(url).withBrowserHeaders().get().build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val body = resp.body?.string() ?: return emptyList()
                val data = JSONObject(body).optJSONArray("data") ?: return emptyList()
                val out = mutableListOf<ComicEntry>()
                for (idx in 0 until data.length()) {
                    val item = data.optJSONObject(idx) ?: continue
                    val mangaId = item.optString("id", "")
                    if (mangaId.isBlank()) continue
                    val attr = item.optJSONObject("attributes") ?: continue
                    val titleObj = attr.optJSONObject("title")
                    val rawTitle = (titleObj?.optString("pt-br", "")?.takeIf { it.isNotBlank() }
                        ?: titleObj?.optString("pt", "")?.takeIf { it.isNotBlank() }
                        ?: titleObj?.optString("en", "")?.takeIf { it.isNotBlank() }
                        ?: titleObj?.optString("ja-ro", "")?.takeIf { it.isNotBlank() }
                        ?: titleObj?.keys()?.asSequence()?.firstOrNull()?.let { titleObj.optString(it, "") }
                        ?: "Sem Título").trim()

                    val descObj = attr.optJSONObject("description")
                    val rawSynopsis = (descObj?.optString("pt-br", "")?.takeIf { it.isNotBlank() }
                        ?: descObj?.optString("pt", "")?.takeIf { it.isNotBlank() }
                        ?: descObj?.optString("en", "")?.takeIf { it.isNotBlank() }
                        ?: "").replace(Regex("\\[.*?\\]\\(.*?\\)"), "").trim()

                    val releaseYear = attr.optInt("year", 2026).let { if (it <= 1900) 2026 else it }

                    var coverFileName = ""
                    val authorNames = mutableListOf<String>()
                    val rels = item.optJSONArray("relationships")
                    if (rels != null) {
                        for (r in 0 until rels.length()) {
                            val rel = rels.optJSONObject(r) ?: continue
                            val rType = rel.optString("type", "")
                            val rAttr = rel.optJSONObject("attributes")
                            if (rType == "cover_art" && rAttr != null) {
                                coverFileName = rAttr.optString("fileName", "")
                            } else if (rType == "author" && rAttr != null) {
                                val aName = rAttr.optString("name", "")
                                if (aName.isNotBlank()) authorNames.add(aName)
                            }
                        }
                    }

                    val coverUrl = if (coverFileName.isNotBlank()) {
                        "https://uploads.mangadex.org/covers/$mangaId/$coverFileName.256.jpg"
                    } else ""

                    val authors = if (authorNames.isNotEmpty()) authorNames.joinToString(", ") else "Autor de Mangá"
                    val ptTitle = PortugueseComicTranslator.translateTitleToPtBr(rawTitle)
                    val publisher = PortugueseComicTranslator.normalizePublisher("", ptTitle, authors, "Mangá")
                    val issueNum = SeriesSequenceEngine.inferIssueNumber(ptTitle)
                    val uniqueExternalId = "md-$mangaId"
                    val rawEntry = ComicEntry(
                        id = uniqueExternalId,
                        externalId = uniqueExternalId,
                        sourceLabel = "Catálogo de Mangás",
                        isOnlineResult = true,
                        title = ptTitle,
                        titlePtBr = ptTitle,
                        authors = authors,
                        publisher = publisher,
                        genre = "Mangá",
                        format = "Mangá (Tankobon)",
                        releaseYear = releaseYear,
                        pageCount = 192,
                        currentPage = 192,
                        coverUrl = coverUrl,
                        synopsis = rawSynopsis,
                        status = ReadingStatus.READ,
                        seriesName = SeriesSequenceEngine.inferBaseSeriesTitle(ptTitle),
                        issueNumber = issueNum
                    )
                    out.add(PortugueseComicTranslator.localizeComicMetadataOnly(rawEntry))
                }
                out
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    suspend fun runOnlineApiDiagnostics(): ApiDiagnosticsReport = coroutineScope {
        val t0 = System.currentTimeMillis()
        suspend fun checkGetEndpoint(name: String, url: String) = withContext(Dispatchers.IO) {
            val start = System.currentTimeMillis()
            try {
                val req = Request.Builder().url(url).withBrowserHeaders().get().build()
                httpClient.newCall(req).execute().use { res ->
                    val ms = (System.currentTimeMillis() - start).coerceAtLeast(10L)
                    ApiEndpointStatus(name = name, ok = res.isSuccessful, ms = ms)
                }
            } catch (_: Throwable) {
                val ms = (System.currentTimeMillis() - start).coerceAtLeast(25L)
                ApiEndpointStatus(name = name, ok = false, ms = ms)
            }
        }

        val jobs = listOf(
            async { checkGetEndpoint("Apple Books API", "https://itunes.apple.com/search?term=batman&media=ebook&limit=2") },
            async { checkGetEndpoint("MangaDex API", "https://api.mangadex.org/manga?title=berserk&limit=2") },
            async { checkGetEndpoint("Kitsu Manga DB", "https://kitsu.io/api/edge/manga?filter[text]=berserk&page[limit]=2") },
            async { checkGetEndpoint("Open Library API", "https://openlibrary.org/search.json?q=watchmen&limit=2") }
        )

        val results = jobs.map { it.await() }
        val testedAt = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        ApiDiagnosticsReport(
            testedAt = testedAt,
            totalMs = (System.currentTimeMillis() - t0).coerceAtLeast(10L),
            endpoints = results
        )
    }
}
