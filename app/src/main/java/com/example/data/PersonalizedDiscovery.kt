package com.example.data

import com.example.model.AppSettings
import com.example.model.ComicEntry
import com.example.model.ReadingStatus
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/** Metadata ranking only. No account history or favorite list is sent to a provider. */
object PersonalizedDiscovery {
    internal fun verifiedCandidate(c: ComicEntry): ComicEntry? {
        if (PortugueseComicTranslator.isUnwantedForeignOrBundleResult(c.title, c.synopsis)) return null
        // A cached translation must not invent a character absent from the original title.
        val title = PortugueseComicTranslator.translateTitleToPtBr(c.title)
        val series = c.seriesName.takeIf { it.isNotBlank() && normalized(title).contains(normalized(it)) }.orEmpty()
        return c.copy(titlePtBr = title, seriesName = series)
    }
    private val stopWords = setOf("the", "and", "uma", "com", "para", "das", "dos", "vol", "volume", "edicao", "edition", "deluxe", "encadernado")
    private val genericGenres = setOf("graphic novel", "quadrinhos", "comics", "hq", "edicao", "encadernado", "colecao", "collection")
    private data class Features(val comic: ComicEntry, val title: String, val titleWords: Set<String>, val series: String,
        val authors: Set<String>, val genres: Set<String>, val publisher: String, val text: String)
    private fun normalized(raw: String) = CatalogSearchPolicy.normalize(raw)
    private fun workKey(c: ComicEntry): String {
        val title = PortugueseComicTranslator.canonicalWorkKey(c.displayTitle).ifBlank { normalized(c.displayTitle).ifBlank { c.externalId.ifBlank { c.id } } }
        val collected = c.format.contains("Graphic", true) || c.format.contains("Encadernado", true) || c.format.contains("Coleção", true)
        return title + if (c.issueNumber > 0 && !collected) ":#${c.issueNumber}" else ""
    }
    private fun features(c: ComicEntry): Features {
        val title = normalized(c.displayTitle)
        val authors = c.authors.split(',', ';', '&').map(::normalized)
            .filter { it.length >= 4 && it !in setOf("autor", "autor desconhecido", "unknown", "various", "autores variados") }.toSet()
        val genres = c.genre.split(',', ';', '/', '|').map(::normalized).filter { it.isNotBlank() && it !in genericGenres }.toSet()
        return Features(c, title, title.split(' ').filter { it.length >= 3 && it.any(Char::isLetter) && it !in stopWords }.toSet(),
            normalized(c.seriesName), authors, genres, normalized(c.publisher),
            " " + normalized(c.displayTitle + " " + c.seriesName + " " + c.synopsis.ifBlank { c.synopsisPtBr }.take(6000)) + " ")
    }
    private fun affinity(candidate: Features, seed: Features): Float {
        var score = 0f
        if (candidate.authors.any { it in seed.authors }) score += 5f
        if (seed.series.isNotBlank() && candidate.series == seed.series) score += 7f
        val overlap = candidate.titleWords.count { it in seed.titleWords }
        if (overlap >= 2) score += 4f
        else if (overlap == 1 && (seed.titleWords.size == 1 ||
                (seed.titleWords.firstOrNull()?.length ?: 0) >= 4 && seed.titleWords.firstOrNull() == candidate.titleWords.firstOrNull())) score += 2f
        score += candidate.genres.count { it in seed.genres }.coerceAtMost(2) * 1.2f
        if (score > 0f && seed.publisher.isNotBlank() && candidate.publisher == seed.publisher) score += 1f
        return score
    }
    private fun aliases(character: String): List<String> {
        val name = character.substringBefore('(').trim()
        val civil = character.substringAfter('(', "").substringBefore(')').trim()
        val result = mutableListOf(name, civil)
        CharacterDirectory.portuguese.forEach { (english, portuguese) ->
            if (normalized(english) == normalized(name) || normalized(portuguese) == normalized(name)) { result.add(english); result.add(portuguese) }
        }
        return result.map(::normalized).filter { it.length >= 3 }.distinct()
    }
    private fun preferred(candidate: Features, preference: String): Boolean {
        val p = normalized(preference)
        if (p.isBlank()) return false
        if (p == "mangas" || p == "manga") return candidate.genres.any { it.contains("manga") }
        if (p == "quadrinhos nacionais") return candidate.genres.any { it.contains("nacion") || it.contains("brasileir") }
        if (p == "independentes") return candidate.genres.any { it.contains("independen") }
        return p.split(' ').filter { it.length >= 2 }.any { word ->
            " ${candidate.publisher} ".contains(" $word ") || candidate.genres.any { " $it ".contains(" $word ") }
        }
    }

    fun extractTasteProfile(
        library: List<ComicEntry>,
        settings: AppSettings,
        favoriteUniverse: String
    ): UserTasteProfile {
        val reading = library.filter { it.status == ReadingStatus.READING }
            .map { SeriesSequenceEngine.inferBaseSeriesTitle(it) to SeriesSequenceEngine.inferIssueNumber(it) }
            .filter { it.first.isNotBlank() }

        val read = library.filter { it.status == ReadingStatus.READ }
            .map { SeriesSequenceEngine.inferBaseSeriesTitle(it) to SeriesSequenceEngine.inferIssueNumber(it) }
            .filter { it.first.isNotBlank() }

        val topAuthors = library.filter {
            (it.rating == 0f || it.rating >= 3.5f || it.rereadCount > 0 || it.favorite || it.status == ReadingStatus.READING) &&
                it.authors.isNotBlank()
        }
            .map { SeriesSequenceEngine.extractPrimaryAuthor(it.authors) }
            .filter { it.isNotBlank() && it !in setOf("autor", "unknown", "varios") }
            .distinct()

        val snapshots = settings.profileFavoriteComicIds.mapNotNull { id ->
            settings.profileShowcaseDetails["comic:$id"]?.let(ProfileShowcase::restoreComic)
        }
        val explicitFavorites = settings.profileFavoriteComicIds.mapNotNull { id ->
            library.firstOrNull { it.id == id || it.externalId == id } ?: snapshots.firstOrNull { it.id == id }
        }
        val allFavorites = (explicitFavorites + library.filter { it.favorite }).distinctBy(::workKey)

        val wantToRead = library.filter { it.status == ReadingStatus.WANT_TO_READ }
            .map { SeriesSequenceEngine.inferBaseSeriesTitle(it) }
            .filter { it.isNotBlank() }
            .distinct()

        val hash = computeCanonicalTasteFingerprint(library, settings, favoriteUniverse)

        return UserTasteProfile(
            readingSeries = reading,
            readSeries = read,
            topAuthors = topAuthors,
            favoriteCharacters = settings.profileFavoriteCharacters,
            favoriteComics = allFavorites,
            wantToReadSeries = wantToRead,
            favoriteUniverse = favoriteUniverse,
            tasteHash = hash
        )
    }

    /**
     * Calcula o fingerprint determinístico canônico v2 da conta, imune a mudanças de ordem da lista,
     * atualizações de currentPage, updatedAt, coverUrl ou flags de sync transitórias.
     */
    fun computeCanonicalTasteFingerprint(
        library: List<ComicEntry>,
        settings: AppSettings,
        favoriteUniverse: String
    ): String {
        val sortedComics = library.sortedWith(
            compareBy<ComicEntry> { it.id.ifBlank { it.externalId } }
                .thenBy { it.externalId }
        )
        val comicsPart = sortedComics.joinToString(";") { c ->
            val sortedAuthors = c.authors.split(',', ';', '/')
                .map { normalized(it) }
                .filter { it.isNotBlank() }
                .sorted()
                .joinToString(",")
            val key = c.id.ifBlank { c.externalId }
            "${key}|${c.status.name}|${c.rating}|${c.favorite}|${c.rereadCount}|${normalized(c.seriesName)}|${c.issueNumber}|${sortedAuthors}|${normalized(c.publisher)}"
        }

        val sortedFavComicIds = settings.profileFavoriteComicIds.map { it.trim() }.filter { it.isNotBlank() }.sorted().joinToString(",")
        val sortedFavChars = settings.profileFavoriteCharacters.map { normalized(it) }.filter { it.isNotBlank() }.sorted().joinToString(",")
        val cleanUniverse = normalized(favoriteUniverse)

        val canonicalPayload = "v4|u:${cleanUniverse}|chars:${sortedFavChars}|comics:${sortedFavComicIds}|lib:${comicsPart}"
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(canonicalPayload.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun recommendWithReasons(
        catalog: List<ComicEntry>,
        library: List<ComicEntry>,
        favoriteUniverse: String = "",
        settings: AppSettings = AppSettings()
    ): List<RecommendedComic> {
        val taste = extractTasteProfile(library, settings, favoriteUniverse)
        val pool = catalog.ifEmpty { CuratedCatalog.curatedComicsCatalog }.takeLast(396)

        val known = library + taste.favoriteComics
        val ownedIds = (known.flatMap { listOf(it.id, it.externalId) } + settings.profileFavoriteComicIds).filter { it.isNotBlank() }.toSet()
        val ownedIsbns = known.mapNotNull { it.isbn.filter { ch -> ch.isDigit() || ch.equals('X', true) }.takeIf { s -> s.length >= 10 } }.toSet()
        val ownedWorks = known.mapTo(HashSet(), ::workKey)

        val candidates = CatalogSearchPolicy.deduplicateByCanonicalWork(pool.mapNotNull(::verifiedCandidate), "PT")
            .sortedByDescending { CatalogSearchPolicy.isPortugueseEdition(it) }.filter { c ->
            val isbn = c.isbn.filter { ch -> ch.isDigit() || ch.equals('X', true) }
            c.id !in ownedIds &&
                (c.externalId.isBlank() || c.externalId !in ownedIds) &&
                (isbn.length < 10 || isbn !in ownedIsbns) &&
                workKey(c) !in ownedWorks
        }.distinctBy(::workKey)

        val scored = candidates.mapNotNull { candidate ->
            val feat = features(candidate)
            var score = 0
            var reason = ""
            var signal = ""

            // 1. Continuação de série em leitura (READING)
            val readingMatch = taste.readingSeries.firstOrNull { (s, lastIssue) ->
                val base = normalized(s)
                val candBase = normalized(SeriesSequenceEngine.inferBaseSeriesTitle(candidate).ifBlank { candidate.seriesName.ifBlank { candidate.title } })
                val candIssue = SeriesSequenceEngine.inferIssueNumber(candidate)
                (candBase.contains(base) || base.contains(candBase)) &&
                    (lastIssue == 0 || candIssue == lastIssue + 1)
            }
            if (readingMatch != null) {
                score += 140
                reason = "Continuação da sua leitura"
                signal = "READING_CONTINUATION"
            }

            // 2. Continuação de série lida (READ)
            if (score < 140) {
                val readMatch = taste.readSeries.firstOrNull { (s, lastIssue) ->
                    val base = normalized(s)
                    val candBase = normalized(SeriesSequenceEngine.inferBaseSeriesTitle(candidate).ifBlank { candidate.seriesName.ifBlank { candidate.title } })
                    val candIssue = SeriesSequenceEngine.inferIssueNumber(candidate)
                    (candBase.contains(base) || base.contains(candBase)) &&
                        (lastIssue > 0 && candIssue == lastIssue + 1)
                }
                if (readMatch != null) {
                    score += 125
                    reason = "Próxima edição de ${readMatch.first.take(24)}"
                    signal = "SERIES_NEXT"
                }
            }

            // 3. Personagens favoritos e obras canônicas
            if (score < 110) {
                for (char in taste.favoriteCharacters) {
                    val aliases = aliases(char)
                    if (aliases.any { feat.text.contains(" $it ") }) {
                        score += 110
                        val charDisplay = char.substringBefore('(').trim()
                        reason = "Com seu favorito: $charDisplay"
                        signal = "FAVORITE_CHARACTER"
                        break
                    }
                }
            }

            // 4. Autores aclamados na estante
            if (score < 90) {
                val topAuthorMatch = taste.topAuthors.firstOrNull { author ->
                    val normAuth = normalized(author)
                    feat.authors.any { it.contains(normAuth) || normAuth.contains(it) }
                }
                if (topAuthorMatch != null) {
                    score += 90
                    reason = "De $topAuthorMatch • Na sua estante"
                    signal = "FAVORITE_AUTHOR"
                }
            }

            // 5. Obras favoritas
            if (score < 80) {
                val favMatch = taste.favoriteComics.firstOrNull { fav ->
                    val favFeat = features(fav)
                    affinity(feat, favFeat) >= 5f
                }
                if (favMatch != null) {
                    score += 80
                    reason = "Porque você curte ${favMatch.displayTitle.take(20)}"
                    signal = "FAVORITE_COMIC"
                }
            }

            // 6. Fila Quero Ler
            if (score < 65) {
                val wtrMatch = taste.wantToReadSeries.firstOrNull { s ->
                    val base = normalized(s)
                    feat.series.contains(base) || feat.title.contains(base)
                }
                if (wtrMatch != null) {
                    score += 65
                    reason = "Relacionado à sua fila Quero Ler"
                    signal = "WANT_TO_READ"
                }
            }

            // 7. Universo favorito / Editora
            if (score < 45 && preferred(feat, favoriteUniverse)) {
                score += 45
                reason = "Destaque do universo $favoriteUniverse"
                signal = "FAVORITE_UNIVERSE"
            }

            if (score > 0) {
                // Desempate por metadados reais apenas quando há sinal real de afinidade
                if (candidate.coverUrl.isNotBlank()) score += 10
                if (candidate.authors.isNotBlank() && !candidate.authors.contains("Equipe criativa", true)) score += 8
                if (candidate.releaseYear > 0) score += 2
                RecommendedComic(candidate, reason.ifBlank { "Sugerido para você" }.take(42), score, signal)
            } else null
        }.sortedByDescending { it.score }

        val hasPersonalSignals = library.isNotEmpty() ||
            settings.profileFavoriteComicIds.isNotEmpty() ||
            settings.profileFavoriteCharacters.isNotEmpty()

        val effectiveScored = if (scored.isNotEmpty()) {
            scored
        } else if (!hasPersonalSignals) {
            candidates.map { candidate ->
                val feat = features(candidate)
                val pub = candidate.publisher.lowercase()
                val reason = when {
                    favoriteUniverse.isNotBlank() && preferred(feat, favoriteUniverse) -> "Destaque de $favoriteUniverse"
                    pub.contains("dc") || pub.contains("vertigo") -> "Destaque do Universo DC"
                    pub.contains("marvel") -> "Destaque do Universo Marvel"
                    candidate.genre.contains("Mangá", true) -> "Destaque em Mangás"
                    else -> "Recomendado para sua estante"
                }
                var score = 30
                if (candidate.coverUrl.isNotBlank()) score += 10
                if (candidate.authors.isNotBlank() && !candidate.authors.contains("Equipe criativa", true)) score += 8
                RecommendedComic(candidate, reason, score, "CATALOG_SHOWCASE")
            }.sortedByDescending { it.score }
        } else {
            emptyList()
        }

        // Aplica diversidade MMR: no máximo 2 edições da mesma série e 3 do mesmo autor no topo
        val seriesCount = mutableMapOf<String, Int>()
        val authorCount = mutableMapOf<String, Int>()
        val reasonCount = mutableMapOf<String, Int>()
        val diverseList = mutableListOf<RecommendedComic>()
        val deferredList = mutableListOf<RecommendedComic>()

        for (item in effectiveScored) {
            val seriesKey = SeriesSequenceEngine.inferBaseSeriesTitle(item.comic).ifBlank { item.comic.title }
            val authorKey = SeriesSequenceEngine.extractPrimaryAuthor(item.comic.authors)

            val currentSeriesCount = seriesCount.getOrDefault(seriesKey, 0)
            val currentAuthorCount = authorCount.getOrDefault(authorKey, 0)

            if (currentSeriesCount < 2 && currentAuthorCount < 3 && reasonCount.getOrDefault(item.reasonLabel, 0) < 2) {
                diverseList.add(item)
                seriesCount[seriesKey] = currentSeriesCount + 1
                authorCount[authorKey] = currentAuthorCount + 1
                reasonCount[item.reasonLabel] = reasonCount.getOrDefault(item.reasonLabel, 0) + 1
            } else {
                deferredList.add(item)
            }
        }

        return (diverseList + deferredList).take(48)
    }

    fun recommend(
        catalog: List<ComicEntry>,
        library: List<ComicEntry>,
        favoriteUniverse: String = "",
        settings: AppSettings = AppSettings()
    ): List<ComicEntry> {
        return recommendWithReasons(catalog, library, favoriteUniverse, settings).map { it.comic }
    }

    const val MAX_DURABLE_RECOMMENDATIONS = 24
    const val MAX_DURABLE_ENVELOPE_BYTES = 64 * 1024 // 64 KB
    const val MAX_CLOUD_RECOMMENDATIONS = 16
    const val MAX_CLOUD_PREFS_BYTES = 12 * 1024 // 12 KB

    /**
     * Serializa recomendações respeitando um teto rígido de bytes UTF-8 para o JSONArray completo.
     * Nunca trunca JSON no meio de uma string; empacota itens completos um a um até o limite.
     */
    fun encodeRecommendationsWithByteBudget(
        items: List<RecommendedComic>,
        maxItems: Int,
        maxBytesUtf8: Int
    ): String {
        if (items.isEmpty()) return ""
        return try {
            val candidateItems = items.take(maxItems)
            val jsonObjects = candidateItems.map { rec ->
                val c = rec.comic
                val safeCover = if (c.coverUrl.startsWith("data:") && c.coverUrl.length > 512) "" else c.coverUrl.take(450)
                org.json.JSONObject().apply {
                    put("id", c.id.take(90))
                    put("externalId", c.externalId.take(90))
                    put("title", c.title.take(160))
                    put("titlePtBr", c.titlePtBr.take(160))
                    put("authors", c.authors.take(120))
                    put("publisher", c.publisher.take(80))
                    put("genre", c.genre.take(60))
                    put("format", c.format.take(60))
                    put("releaseYear", c.releaseYear)
                    put("pageCount", c.pageCount)
                    put("coverUrl", safeCover)
                    put("seriesName", c.seriesName.take(120))
                    put("issueNumber", c.issueNumber)
                    put("reasonLabel", rec.reasonLabel.take(42))
                    put("score", rec.score)
                    put("signalSource", rec.signalSource.take(32))
                }
            }

            val finalArray = org.json.JSONArray()
            for (obj in jsonObjects) {
                finalArray.put(obj)
                val currentBytes = finalArray.toString().toByteArray(StandardCharsets.UTF_8).size
                if (currentBytes > maxBytesUtf8) {
                    finalArray.remove(finalArray.length() - 1)
                    break
                }
            }
            finalArray.toString()
        } catch (_: Throwable) {
            ""
        }
    }

    /**
     * Serializa até 16 recomendações de forma compacta (<= 12 KB) para AppSettings e Firestore.
     */
    fun encodeCachedRecommendations(items: List<RecommendedComic>): String {
        return encodeRecommendationsWithByteBudget(items, MAX_CLOUD_RECOMMENDATIONS, MAX_CLOUD_PREFS_BYTES)
    }

    /**
     * Serializa até 24 recomendações completas para o snapshot durável da conta em filesDir (<= 64 KB).
     */
    fun encodeDurableAccountRecommendations(items: List<RecommendedComic>): String {
        return encodeRecommendationsWithByteBudget(items, MAX_DURABLE_RECOMMENDATIONS, MAX_DURABLE_ENVELOPE_BYTES)
    }

    /**
     * Restaura a lista de recomendações salvas no cache da conta (SafeStorage / filesDir / Firestore).
     */
    fun decodeCachedRecommendations(rawJson: String): List<RecommendedComic> {
        if (rawJson.isBlank()) return emptyList()
        return try {
            val arr = org.json.JSONArray(rawJson)
            val result = mutableListOf<RecommendedComic>()
            for (i in 0 until arr.length().coerceAtMost(MAX_DURABLE_RECOMMENDATIONS)) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id", "").trim()
                val title = obj.optString("title", "").trim()
                if (id.isBlank() || title.isBlank()) continue
                val comic = ComicEntry(
                    id = id,
                    externalId = obj.optString("externalId", ""),
                    title = title,
                    titlePtBr = obj.optString("titlePtBr", ""),
                    authors = obj.optString("authors", ""),
                    publisher = obj.optString("publisher", ""),
                    genre = obj.optString("genre", "Graphic Novel"),
                    format = obj.optString("format", "Encadernado (TPB)"),
                    releaseYear = obj.optInt("releaseYear", 2026),
                    pageCount = obj.optInt("pageCount", 160),
                    coverUrl = obj.optString("coverUrl", ""),
                    seriesName = obj.optString("seriesName", ""),
                    issueNumber = obj.optInt("issueNumber", 0)
                )
                result.add(
                    RecommendedComic(
                        comic = comic,
                        reasonLabel = obj.optString("reasonLabel", "Para você").take(42),
                        score = obj.optInt("score", 100),
                        signalSource = obj.optString("signalSource", "CACHE")
                    )
                )
            }
            result
        } catch (_: Throwable) {
            emptyList()
        }
    }
}
