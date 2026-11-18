package com.example.data

import com.example.model.AppSettings
import com.example.model.ComicEntry
import com.example.model.ReadingStatus
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/** Metadata ranking only. No account history or favorite list is sent to a provider. */
object PersonalizedDiscovery {
    internal fun verifiedCandidate(c: ComicEntry): ComicEntry? {
        val language = c.editionLanguage.trim().lowercase().replace('_', '-').substringBefore('-')
        val original = c.title.trim()
        val specialized = PortugueseSearchPolicy.isSpecializedComicCatalog(c)
        if (specialized) {
            if (language.isNotBlank() && language !in setOf("pt", "por", "portuguese", "en", "eng", "english")) return null
        } else {
            val neutral = normalized(original) in setOf("batman", "superman", "watchmen", "maus", "marvels", "sandman", "hellboy", "flashpoint")
            if (language.isNotBlank() && language !in setOf("pt", "por")) return null
            if ((c.isOnlineResult || c.id.startsWith("ab-") || c.id.startsWith("gb-") || c.id.startsWith("ol-")) && language.isBlank() && !neutral) return null
        }
        if (PortugueseComicTranslator.isUnwantedForeignOrBundleResult(c.title, c.synopsis)) return null
        // A cached translation must not invent a character absent from the original title.
        val rawTitle = " " + normalized(c.title) + " "
        val inventedCharacter = CharacterDirectory.portuguese.any { (english, portuguese) ->
            val cached = " " + normalized(c.titlePtBr) + " "
            cached.contains(" ${normalized(portuguese)} ") &&
                !rawTitle.contains(" ${normalized(english)} ") && !rawTitle.contains(" ${normalized(portuguese)} ")
        }
        val title = c.titlePtBr.takeIf { it.isNotBlank() && !inventedCharacter }
            ?: PortugueseComicTranslator.translateTitleToPtBr(c.title)
        if (!normalized(title).any { it in 'a'..'z' }) return null
        val series = c.seriesName.takeIf { it.isNotBlank() && normalized(title).contains(normalized(it)) }.orEmpty()
        return c.copy(titlePtBr = title, seriesName = series)
    }
    private val stopWords = setOf("the", "and", "uma", "com", "para", "das", "dos", "vol", "volume", "edicao", "edition", "deluxe", "encadernado")
    private val genericGenres = setOf("graphic novel", "quadrinhos", "comics", "hq", "edicao", "encadernado", "colecao", "collection")
    private data class Features(val comic: ComicEntry, val title: String, val titleWords: Set<String>, val series: String,
        val authors: Set<String>, val genres: Set<String>, val publisher: String, val text: String)
    private fun normalized(raw: String) = CatalogSearchPolicy.normalize(raw)
    fun recommendationWorkKey(c: ComicEntry): String = workKey(c)
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
        favoriteUniverse: String,
        feedback: RecommendationFeedback = RecommendationFeedback()
    ): UserTasteProfile {
        val reading = library.filter { it.status == ReadingStatus.READING }
            .map { SeriesSequenceEngine.inferBaseSeriesTitle(it) to SeriesSequenceEngine.inferIssueNumber(it) }
            .filter { it.first.isNotBlank() }

        val read = library.filter { it.status == ReadingStatus.READ && (it.rating == 0f || it.rating > 2.0f || it.favorite) }
            .map { SeriesSequenceEngine.inferBaseSeriesTitle(it) to SeriesSequenceEngine.inferIssueNumber(it) }
            .filter { it.first.isNotBlank() }

        val topAuthors = library.filter {
            (it.rating == 0f || it.rating >= 3.5f || it.rereadCount > 0 || it.favorite || it.status == ReadingStatus.READING) &&
                it.authors.isNotBlank()
        }
            .map { SeriesSequenceEngine.extractPrimaryAuthor(it.authors) }
            .filter { it.isNotBlank() && it !in setOf("autor", "unknown", "varios") }
            .distinct()

        val likedSeriesSet = library.filter {
            it.rating >= 3.5f || it.favorite || it.rereadCount > 0 || it.status == ReadingStatus.READING
        }.map { normalized(SeriesSequenceEngine.inferBaseSeriesTitle(it)) }.filter { it.isNotBlank() }.toSet()

        val topAuthorsNorm = topAuthors.map(::normalized).filter { it.isNotBlank() }.toSet()

        val lowRatedEntries = library.filter { it.rating in 0.5f..2.0f && !it.favorite }
        val dislikedSeries = lowRatedEntries
            .map { normalized(SeriesSequenceEngine.inferBaseSeriesTitle(it)) }
            .filter { it.isNotBlank() && it !in likedSeriesSet }
            .toSet()
        val dislikedAuthors = lowRatedEntries
            .map { normalized(SeriesSequenceEngine.extractPrimaryAuthor(it.authors)) }
            .filter { it.length >= 4 && it !in setOf("autor", "unknown", "varios") && it !in topAuthorsNorm }
            .toSet()

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

        val hash = computeCanonicalTasteFingerprint(library, settings, favoriteUniverse, feedback)

        return UserTasteProfile(
            readingSeries = reading,
            readSeries = read,
            topAuthors = topAuthors,
            favoriteCharacters = settings.profileFavoriteCharacters,
            favoriteComics = allFavorites,
            wantToReadSeries = wantToRead,
            favoriteUniverse = favoriteUniverse,
            tasteHash = hash,
            moreLike = feedback.boosted.map { it.comic() },
            dislikedSeries = dislikedSeries,
            dislikedAuthors = dislikedAuthors
        )
    }

    /**
     * Calcula o fingerprint determinístico canônico v2 da conta, imune a mudanças de ordem da lista,
     * atualizações de currentPage, updatedAt, coverUrl ou flags de sync transitórias.
     */
    fun computeCanonicalTasteFingerprint(
        library: List<ComicEntry>,
        settings: AppSettings,
        favoriteUniverse: String,
        feedback: RecommendationFeedback = RecommendationFeedback()
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

        val canonicalPayload = "v16|u:${cleanUniverse}|chars:${sortedFavChars}|comics:${sortedFavComicIds}|lib:${comicsPart}|feedback:${feedback.canonical()}"
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(canonicalPayload.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun recommendWithReasons(
        catalog: List<ComicEntry>,
        library: List<ComicEntry>,
        favoriteUniverse: String = "",
        settings: AppSettings = AppSettings(),
        feedback: RecommendationFeedback = RecommendationFeedback()
    ): List<RecommendedComic> {
        val taste = extractTasteProfile(library, settings, favoriteUniverse, feedback)
        val pool = catalog.ifEmpty { CuratedCatalog.curatedComicsCatalog }.takeLast(396)

        val known = library + taste.favoriteComics
        val ownedIds = (known.flatMap { listOf(it.id, it.externalId) } + settings.profileFavoriteComicIds).filter { it.isNotBlank() }.toSet()
        val ownedIsbns = known.mapNotNull { it.isbn.filter { ch -> ch.isDigit() || ch.equals('X', true) }.takeIf { s -> s.length >= 10 } }.toSet()
        val ownedWorks = known.mapTo(HashSet(), ::workKey)

        val prefersManga = normalized(favoriteUniverse).contains("manga") || library.any { normalized(it.genre).contains("manga") }
        val candidates = CatalogSearchPolicy.deduplicateByCanonicalWork(pool.mapNotNull(::verifiedCandidate), "PT")
            .sortedByDescending { CatalogSearchPolicy.isPortugueseEdition(it) }.filter { c ->
            if (feedback.hides(c)) return@filter false
            if (!prefersManga && (c.sourceLabel.contains("MangaDex", true) || c.sourceLabel.contains("Kitsu", true) || c.id.startsWith("md-") || c.id.startsWith("kt-"))) return@filter false
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
            var reference = ""

            // 1. Continuação de série em leitura (READING)
            val readingMatch = taste.readingSeries.firstOrNull { (s, lastIssue) ->
                val base = normalized(s)
                val candBase = normalized(SeriesSequenceEngine.inferBaseSeriesTitle(candidate).ifBlank { candidate.seriesName.ifBlank { candidate.title } })
                val candIssue = SeriesSequenceEngine.inferIssueNumber(candidate)
                base.isNotBlank() && candBase.isNotBlank() && base == candBase &&
                    lastIssue > 0 && candIssue == lastIssue + 1
            }
            if (readingMatch != null) {
                score += 140
                reason = "Continuação da sua leitura"
                signal = "READING_CONTINUATION"
                reference = readingMatch.first
            }

            // 2. Continuação de série lida (READ)
            if (score < 140) {
                val readMatch = taste.readSeries.firstOrNull { (s, lastIssue) ->
                    val base = normalized(s)
                    val candBase = normalized(SeriesSequenceEngine.inferBaseSeriesTitle(candidate).ifBlank { candidate.seriesName.ifBlank { candidate.title } })
                    val candIssue = SeriesSequenceEngine.inferIssueNumber(candidate)
                    base.isNotBlank() && candBase.isNotBlank() && base == candBase &&
                        (lastIssue > 0 && candIssue == lastIssue + 1)
                }
                if (readMatch != null) {
                    score += 125
                    reason = "Próxima edição de ${readMatch.first.take(24)}"
                    signal = "SERIES_NEXT"
                    reference = readMatch.first
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
                        reference = charDisplay
                        break
                    }
                }
            }

            // 4. Autores aclamados na estante
            if (score < 90) {
                val topAuthorMatch = taste.topAuthors.firstOrNull { author ->
                    val normAuth = normalized(author)
                    normAuth.isNotBlank() && feat.authors.any { it.contains(normAuth) || normAuth.contains(it) }
                }
                if (topAuthorMatch != null) {
                    score += 90
                    reason = "De $topAuthorMatch • Na sua estante"
                    signal = "FAVORITE_AUTHOR"
                    reference = topAuthorMatch
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
                    reference = favMatch.displayTitle
                }
            }

            // 6. Fila Quero Ler
            if (score < 85) {
                val more = taste.moreLike.firstOrNull { affinity(feat, features(it)) >= 5f }
                if (more != null) {
                    score += 85
                    reason = "Mais como ${more.displayTitle.take(26)}"
                    signal = "EXPLICIT_MORE"
                    reference = more.displayTitle
                }
            }
            if (score < 65) {
                val wtrMatch = taste.wantToReadSeries.firstOrNull { s ->
                    val base = normalized(s)
                    base.isNotBlank() && (feat.series.contains(base) || feat.title.contains(base))
                }
                if (wtrMatch != null) {
                    score += 65
                    reason = "Relacionado à sua fila Quero Ler"
                    signal = "WANT_TO_READ"
                    reference = wtrMatch
                }
            }

            // 7. Universo favorito / Editora
            if (score < 45 && preferred(feat, favoriteUniverse)) {
                score += 45
                reason = "Destaque do universo $favoriteUniverse"
                signal = "FAVORITE_UNIVERSE"
                reference = favoriteUniverse
            }

            // 8. Penalidade para séries/autores avaliados negativamente (<= 2.0★) sem sinal explícito superior
            if (score > 0 && signal !in setOf("EXPLICIT_MORE", "FAVORITE_CHARACTER", "FAVORITE_COMIC", "READING_CONTINUATION")) {
                val candSeriesNorm = normalized(SeriesSequenceEngine.inferBaseSeriesTitle(candidate).ifBlank { candidate.seriesName })
                val candAuthorNorm = normalized(SeriesSequenceEngine.extractPrimaryAuthor(candidate.authors))
                if (candSeriesNorm.isNotBlank() && candSeriesNorm in taste.dislikedSeries) {
                    score -= 55
                } else if (candAuthorNorm.isNotBlank() && candAuthorNorm in taste.dislikedAuthors) {
                    score -= 40
                }
            }

            if (score > 0) {
                // Desempate por metadados reais e prioridade forte para edições em português
                if (CatalogSearchPolicy.isPortugueseEdition(candidate) || candidate.editionLanguage.startsWith("pt", true)) score += 60
                if (candidate.coverUrl.isNotBlank()) score += 10
                val publisher = normalized(candidate.publisher)
                if (publisher.contains("dc comics") || publisher.contains("marvel") || publisher.contains("vertigo")) score += 25
                if (candidate.authors.isNotBlank() && !candidate.authors.contains("Equipe criativa", true)) score += 8
                if (candidate.releaseYear > 0) score += 2
                val detail = when (signal) {
                    "READING_CONTINUATION" -> "É a próxima edição identificada de $reference, série que você está lendo."
                    "SERIES_NEXT" -> "É a próxima edição identificada de $reference, presente nas suas leituras concluídas."
                    "FAVORITE_CHARACTER" -> "Os metadados desta HQ mencionam $reference, um dos seus personagens favoritos."
                    "FAVORITE_AUTHOR" -> "Tem autoria em comum com suas leituras: $reference."
                    "FAVORITE_COMIC" -> "Compartilha sinais do catálogo com $reference, uma das suas HQs favoritas."
                    "EXPLICIT_MORE" -> "Você pediu mais sugestões como $reference. Esta HQ compartilha sinais do catálogo com ela."
                    "WANT_TO_READ" -> "Relacionada a $reference, que está na sua fila Quero Ler."
                    "FAVORITE_UNIVERSE" -> "Os metadados correspondem à sua preferência por $reference."
                    else -> "Seleção de catálogo. Não há um motivo pessoal detalhado registrado."
                }
                RecommendedComic(candidate, reason.ifBlank { "Sugerido para você" }.take(42), score, signal,
                    RecommendationReason(signal,reference.take(300),detail.take(1000)))
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
                RecommendedComic(candidate, reason, score, "CATALOG_SHOWCASE",
                    RecommendationReason("CATALOG_SHOWCASE","","Seleção editorial do catálogo, sem um sinal pessoal suficiente registrado."))
            }.sortedByDescending { it.score }
        } else {
            emptyList()
        }

        // Diversidade escalonada MMR:
        // - Posições 1..8 (carrossel inicial): diversidade estrita (máx. 2 série / 3 autor / 2 motivo)
        // - Posições 9..30 (grade expandida "Para você"): diversidade relaxada (máx. 4 série / 6 autor / 8 motivo)
        val seriesCount = mutableMapOf<String, Int>()
        val authorCount = mutableMapOf<String, Int>()
        val reasonCount = mutableMapOf<String, Int>()
        val carouselList = mutableListOf<RecommendedComic>()
        val expandedList = mutableListOf<RecommendedComic>()
        val deferredList = mutableListOf<RecommendedComic>()
        val monthlySeries = HashSet<String>()

        for (item in effectiveScored) {
            val seriesKey = normalized(SeriesSequenceEngine.inferBaseSeriesTitle(item.comic).ifBlank { item.comic.title })
            val format = normalized(item.comic.format)
            val monthly = item.comic.issueNumber > 0 &&
                listOf("encadernado", "graphic", "capa dura", "omnibus", "colecao", "manga").none { format.contains(it) }
            if (monthly && !monthlySeries.add(seriesKey)) continue
            val authorKey = SeriesSequenceEngine.extractPrimaryAuthor(item.comic.authors)

            val currentSeriesCount = seriesCount.getOrDefault(seriesKey, 0)
            val currentAuthorCount = authorCount.getOrDefault(authorKey, 0)
            val currentReasonCount = reasonCount.getOrDefault(item.reasonLabel, 0)

            if (carouselList.size < 8 && currentSeriesCount < 2 && currentAuthorCount < 3 && currentReasonCount < 2) {
                carouselList.add(item)
                seriesCount[seriesKey] = currentSeriesCount + 1
                authorCount[authorKey] = currentAuthorCount + 1
                reasonCount[item.reasonLabel] = currentReasonCount + 1
            } else if ((carouselList.size + expandedList.size) < MAX_DURABLE_RECOMMENDATIONS &&
                currentSeriesCount < 4 && currentAuthorCount < 6 && currentReasonCount < 8
            ) {
                expandedList.add(item)
                seriesCount[seriesKey] = currentSeriesCount + 1
                authorCount[authorKey] = currentAuthorCount + 1
                reasonCount[item.reasonLabel] = currentReasonCount + 1
            } else {
                deferredList.add(item)
            }
        }

        return (carouselList + expandedList + deferredList).take(48)
    }

    fun recommend(
        catalog: List<ComicEntry>,
        library: List<ComicEntry>,
        favoriteUniverse: String = "",
        settings: AppSettings = AppSettings()
    ): List<ComicEntry> {
        return recommendWithReasons(catalog, library, favoriteUniverse, settings).map { it.comic }
    }

    const val MAX_DURABLE_RECOMMENDATIONS = 30
    const val MAX_DURABLE_ENVELOPE_BYTES = 96 * 1024 // 96 KB
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
                    put("editionLanguage", c.editionLanguage)
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
                    rec.explanation?.let { e -> put("explanation",org.json.JSONObject().put("kind",e.kind.take(32))
                        .put("reference",e.reference.take(300)).put("detail",e.detail.take(1000))) }
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
     * Serializa até 30 recomendações completas para o snapshot durável da conta em filesDir (<= 96 KB com envelope).
     */
    fun encodeDurableAccountRecommendations(items: List<RecommendedComic>): String {
        return encodeRecommendationsWithByteBudget(items, MAX_DURABLE_RECOMMENDATIONS, MAX_DURABLE_ENVELOPE_BYTES - 2048)
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
                    editionLanguage = obj.optString("editionLanguage"),
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
                        signalSource = obj.optString("signalSource", "CACHE"),
                        explanation = obj.optJSONObject("explanation")?.let { e -> RecommendationReason(
                            e.optString("kind").take(32),e.optString("reference").take(300),e.optString("detail").take(1000)) }
                    )
                )
            }
            result
        } catch (_: Throwable) {
            emptyList()
        }
    }
}
