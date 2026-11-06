package com.example.data

import com.example.model.AppSettings
import com.example.model.ComicEntry
import com.example.model.ReadingStatus
import java.security.MessageDigest

/** Metadata ranking only. No account history or favorite list is sent to a provider. */
object PersonalizedDiscovery {
    private val stopWords = setOf("the", "and", "uma", "com", "para", "das", "dos", "vol", "volume", "edicao", "edition", "deluxe", "encadernado")
    private val genericGenres = setOf("graphic novel", "quadrinhos", "comics", "hq", "edicao", "encadernado", "colecao", "collection")
    private data class Features(val comic: ComicEntry, val title: String, val titleWords: Set<String>, val series: String,
        val authors: Set<String>, val genres: Set<String>, val publisher: String, val text: String)
    private fun normalized(raw: String) = CatalogSearchPolicy.normalize(raw)
    private fun workKey(c: ComicEntry): String {
        val title = PortugueseComicTranslator.canonicalWorkKey(c.displayTitle).ifBlank { normalized(c.displayTitle).ifBlank { c.externalId.ifBlank { c.id } } }
        return title + if (c.issueNumber > 0) ":#${c.issueNumber}" else ""
    }
    private fun features(c: ComicEntry): Features {
        val title = normalized(c.displayTitle)
        val authors = c.authors.split(',', ';', '&').map(::normalized)
            .filter { it.length >= 4 && it !in setOf("autor", "autor desconhecido", "unknown", "various", "autores variados") }.toSet()
        val genres = c.genre.split(',', ';', '/', '|').map(::normalized).filter { it.isNotBlank() && it !in genericGenres }.toSet()
        return Features(c, title, title.split(' ').filter { it.length >= 3 && it.any(Char::isLetter) && it !in stopWords }.toSet(),
            normalized(c.seriesName), authors, genres, normalized(c.publisher),
            " " + normalized(c.displayTitle + " " + c.seriesName + " " + c.synopsisPtBr.ifBlank { c.synopsis }.take(6000)) + " ")
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

        val topAuthors = library.filter { (it.rating >= 4.0f || it.rereadCount > 0 || it.favorite) && it.authors.isNotBlank() }
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

        val rawSeed = (library.map { it.id + it.status.name + it.rating } + settings.profileFavoriteCharacters + settings.profileFavoriteComicIds + favoriteUniverse).joinToString("|")
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(rawSeed.toByteArray())
        val hash = digest.take(8).joinToString("") { "%02x".format(it) }

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

    fun recommendWithReasons(
        catalog: List<ComicEntry>,
        library: List<ComicEntry>,
        favoriteUniverse: String = "",
        settings: AppSettings = AppSettings()
    ): List<RecommendedComic> {
        val taste = extractTasteProfile(library, settings, favoriteUniverse)
        val pool = (CuratedCatalog.curatedComicsCatalog + RecommendationCandidateRetriever.getCachedPool() + catalog.takeLast(396)).distinctBy(::workKey)

        val known = library + taste.favoriteComics
        val ownedIds = (known.flatMap { listOf(it.id, it.externalId) } + settings.profileFavoriteComicIds).filter { it.isNotBlank() }.toSet()
        val ownedWorks = known.mapTo(HashSet(), ::workKey)

        val candidates = CatalogSearchPolicy.unique(pool).filter { c ->
            c.id !in ownedIds &&
                (c.externalId.isBlank() || c.externalId !in ownedIds) &&
                workKey(c) !in ownedWorks &&
                known.none { lib -> CatalogSearchPolicy.shouldMergeEntries(c, lib) }
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
                    val isCanonical = CatalogSearchPolicy.verifiedCanonicalWorks.entries.any { (k, works) ->
                        aliases.any { it.contains(k) || k.contains(it) } && works.any { feat.title.contains(it) }
                    }
                    if (aliases.any { " ${feat.title} ${feat.series} ".contains(" $it ") }) {
                        score += if (isCanonical) 155 else 110
                        val charDisplay = char.substringBefore('(').trim()
                        reason = if (isCanonical) "Clássico de $charDisplay" else "Com seu favorito: $charDisplay"
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

            // Metadados reais
            if (candidate.coverUrl.isNotBlank()) score += 10
            if (candidate.authors.isNotBlank() && !candidate.authors.contains("Equipe criativa", true)) score += 8
            if (candidate.releaseYear > 0) score += 2

            if (score > 0) {
                RecommendedComic(candidate, reason.ifBlank { "Sugerido para você" }.take(42), score, signal)
            } else null
        }.sortedByDescending { it.score }

        // Aplica diversidade MMR: no máximo 2 edições da mesma série e 3 do mesmo autor no topo
        val seriesCount = mutableMapOf<String, Int>()
        val authorCount = mutableMapOf<String, Int>()
        val diverseList = mutableListOf<RecommendedComic>()
        val deferredList = mutableListOf<RecommendedComic>()

        for (item in scored) {
            val seriesKey = SeriesSequenceEngine.inferBaseSeriesTitle(item.comic).ifBlank { item.comic.title }
            val authorKey = SeriesSequenceEngine.extractPrimaryAuthor(item.comic.authors)

            val currentSeriesCount = seriesCount.getOrDefault(seriesKey, 0)
            val currentAuthorCount = authorCount.getOrDefault(authorKey, 0)

            if (currentSeriesCount < 2 && currentAuthorCount < 3) {
                diverseList.add(item)
                seriesCount[seriesKey] = currentSeriesCount + 1
                authorCount[authorKey] = currentAuthorCount + 1
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
        val withReasons = recommendWithReasons(catalog, library, favoriteUniverse, settings)
        if (withReasons.isNotEmpty()) return withReasons.map { it.comic }
        return catalog.take(24)
    }

    /**
     * Serializa até 16 recomendações de forma compacta (<= 12 KB) para SafeStorage e Firestore.
     */
    fun encodeCachedRecommendations(items: List<RecommendedComic>): String {
        if (items.isEmpty()) return ""
        return try {
            val arr = org.json.JSONArray()
            for (rec in items.take(16)) {
                val c = rec.comic
                val obj = org.json.JSONObject().apply {
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
                    put("coverUrl", c.coverUrl.take(450))
                    put("seriesName", c.seriesName.take(120))
                    put("issueNumber", c.issueNumber)
                    put("reasonLabel", rec.reasonLabel.take(42))
                    put("score", rec.score)
                    put("signalSource", rec.signalSource.take(32))
                }
                arr.put(obj)
            }
            val raw = arr.toString()
            if (raw.toByteArray(Charsets.UTF_8).size <= 12_288) raw else ""
        } catch (_: Throwable) {
            ""
        }
    }

    /**
     * Restaura a lista de recomendações salvas no cache da conta (SafeStorage / Firestore).
     */
    fun decodeCachedRecommendations(rawJson: String): List<RecommendedComic> {
        if (rawJson.isBlank()) return emptyList()
        return try {
            val arr = org.json.JSONArray(rawJson)
            val result = mutableListOf<RecommendedComic>()
            for (i in 0 until arr.length().coerceAtMost(16)) {
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
