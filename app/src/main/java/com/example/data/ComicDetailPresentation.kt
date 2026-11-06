package com.example.data

import com.example.model.ComicEntry
import java.util.Locale

internal object ComicDetailPresentation {
    /** Returns related editions: first from the provider resource, falling back to multi-signal related works from catalog pool. */
    fun relatedEditions(comic: ComicEntry, detail: CatalogDetails?, catalogPool: List<ComicEntry> = emptyList()): List<ComicEntry> {
        return relatedEditionsWithReasons(comic, detail, catalogPool).map { it.comic }
    }

    /**
     * Multi-signal related recommendations for the HQ detail page:
     * 1. Next issues in the same series (#N+1, #N+2)
     * 2. Same series / story arc
     * 3. Same primary author
     * 4. Same credited character (strictly from real detail.characters credits — never invented)
     * 5. Verified canonical related works
     * Bounded to at most 8 deduplicated items, each with a truthful PT-BR relationship label.
     */
    fun relatedEditionsWithReasons(
        comic: ComicEntry,
        detail: CatalogDetails?,
        catalogPool: List<ComicEntry> = emptyList()
    ): List<RecommendedComic> {
        val current = setOf(comic.id, comic.externalId).filter { it.isNotBlank() }.toSet()
        val direct = detail?.issues.orEmpty().filter { candidate ->
            candidate.id.isNotBlank() && candidate.id !in current &&
                (candidate.externalId.isBlank() || candidate.externalId !in current)
        }
        if (direct.isNotEmpty()) {
            val baseIssue = SeriesSequenceEngine.inferIssueNumber(comic)
            return direct.distinctBy { it.externalId.ifBlank { it.id } }.take(8).map { issue ->
                val candIssue = SeriesSequenceEngine.inferIssueNumber(issue)
                val reason = when {
                    baseIssue > 0 && candIssue == baseIssue + 1 -> "Próxima edição • #$candIssue"
                    baseIssue > 0 && candIssue > baseIssue -> "Continuação • #$candIssue"
                    else -> "Edição da mesma série"
                }
                RecommendedComic(
                    comic = issue,
                    reasonLabel = reason,
                    score = if (baseIssue > 0 && candIssue == baseIssue + 1) 180 else 140,
                    signalSource = "DETAIL_ISSUES"
                )
            }
        }

        if (catalogPool.isEmpty()) return emptyList()

        val primaryAuthor = comic.authors.split(",")
            .map { it.trim() }
            .firstOrNull { it.length >= 4 && !it.equals("Autor desconhecido", ignoreCase = true) }
            ?: detail?.creators.orEmpty()
                .firstOrNull { it.role.contains("Roteiro", true) || it.role.contains("writer", true) }
                ?.name?.trim().orEmpty()

        val rawSeries = comic.seriesName.ifBlank { comic.displayTitle.substringBefore(':').trim() }
        val baseSeries = SeriesSequenceEngine.inferBaseSeriesTitle(comic).ifBlank { rawSeries }
        val normRawSeries = CatalogSearchPolicy.normalize(rawSeries)
        val normBaseSeries = CatalogSearchPolicy.normalize(baseSeries)
        val normAuthor = CatalogSearchPolicy.normalize(primaryAuthor)
        val normPub = CatalogSearchPolicy.normalize(comic.publisher.ifBlank { detail?.publisher.orEmpty() })
        val baseIssue = SeriesSequenceEngine.inferIssueNumber(comic)

        // Strictly real credited characters from detail.characters — never invented
        val creditedCharacters = detail?.characters.orEmpty()
            .map { it.name.trim() }
            .filter { it.length >= 3 }
            .distinct()

        val candidates = catalogPool.filter { c ->
            c.id !in current && (c.externalId.isBlank() || c.externalId !in current) &&
                !c.title.equals(comic.title, ignoreCase = true) &&
                !CatalogSearchPolicy.shouldMergeEntries(c, comic)
        }

        fun matchesSeries(c: ComicEntry): Boolean {
            val candRaw = CatalogSearchPolicy.normalize(c.seriesName.ifBlank { c.displayTitle.substringBefore(':').trim() })
            val candBase = CatalogSearchPolicy.normalize(SeriesSequenceEngine.inferBaseSeriesTitle(c).ifBlank { candRaw })
            val rawMatch = normRawSeries.length >= 3 && candRaw.isNotBlank() &&
                (candRaw == normRawSeries || candRaw.contains(normRawSeries) || normRawSeries.contains(candRaw))
            val baseMatch = normBaseSeries.length >= 3 && candBase.isNotBlank() &&
                (candBase == normBaseSeries || candBase.contains(normBaseSeries) || normBaseSeries.contains(candBase))
            return rawMatch || baseMatch
        }

        val allSameSeries = candidates.filter { matchesSeries(it) }

        // Signal 1: Next issues in the same series (#N+1, #N+2)
        val nextInSeries = if (baseIssue > 0) {
            allSameSeries.mapNotNull { c ->
                val candIssue = SeriesSequenceEngine.inferIssueNumber(c)
                if (candIssue == baseIssue + 1 || candIssue == baseIssue + 2) {
                    val distanceBoost = if (candIssue == baseIssue + 1) 200 else 185
                    RecommendedComic(
                        comic = c,
                        reasonLabel = "Próxima edição • #$candIssue",
                        score = distanceBoost,
                        signalSource = "NEXT_IN_SERIES"
                    )
                } else null
            }.sortedByDescending { it.score }
        } else emptyList()

        // Signal 2: Same series / story arc
        val nextIds = nextInSeries.map { it.comic.externalId.ifBlank { it.comic.id } }.toSet()
        val sameSeriesArc = allSameSeries
            .filter { it.externalId.ifBlank { it.id } !in nextIds }
            .map { c ->
                RecommendedComic(
                    comic = c,
                    reasonLabel = "Mesma série",
                    score = 160,
                    signalSource = "SAME_SERIES"
                )
            }

        // Signal 3: Same primary author
        val sameAuthor = if (normAuthor.length >= 4) {
            candidates.filter { c ->
                val a = CatalogSearchPolicy.normalize(c.authors)
                a.isNotBlank() && (a.contains(normAuthor) || normAuthor.contains(a))
            }.map { c ->
                RecommendedComic(
                    comic = c,
                    reasonLabel = "De ${primaryAuthor.take(24)}",
                    score = 135,
                    signalSource = "SAME_AUTHOR"
                )
            }
        } else emptyList()

        // Signal 4: Same credited character (only when detail.characters has real credits)
        val sameCreditedCharacter = if (creditedCharacters.isNotEmpty()) {
            candidates.mapNotNull { c ->
                val candNorm = CatalogSearchPolicy.normalize("${c.title} ${c.titlePtBr} ${c.seriesName}")
                val matchedChar = creditedCharacters.firstOrNull { charName ->
                    val normChar = CatalogSearchPolicy.normalize(charName.substringBefore('(').substringBefore('—').trim())
                    normChar.length >= 3 && candNorm.contains(normChar)
                }
                if (matchedChar != null) {
                    val cleanChar = matchedChar.substringBefore('(').trim().take(24)
                    RecommendedComic(
                        comic = c,
                        reasonLabel = "Com $cleanChar",
                        score = 125,
                        signalSource = "CREDITED_CHARACTER"
                    )
                } else null
            }
        } else emptyList()

        // Signal 5: Canonical related works (verified classics for the series or credited character)
        val canonicalKeys = buildList {
            if (normBaseSeries.length >= 3) add(normBaseSeries)
            if (normRawSeries.length >= 3) add(normRawSeries)
            creditedCharacters.forEach { ch ->
                val n = CatalogSearchPolicy.normalize(ch.substringBefore('(').substringBefore('—').trim())
                if (n.length >= 3) add(n)
            }
        }.distinct()

        val canonicalTargets = canonicalKeys.flatMap { key ->
            CatalogSearchPolicy.verifiedCanonicalWorks.entries
                .filter { (k, _) -> key.contains(k) || k.contains(key) }
                .flatMap { it.value }
        }.map { CatalogSearchPolicy.normalize(it) }.distinct()

        val canonicalRelated = if (canonicalTargets.isNotEmpty()) {
            candidates.mapNotNull { c ->
                val candTitleNorm = CatalogSearchPolicy.normalize("${c.title} ${c.titlePtBr}")
                val isCanonical = canonicalTargets.any { target -> target.length >= 4 && candTitleNorm.contains(target) }
                if (isCanonical) {
                    RecommendedComic(
                        comic = c,
                        reasonLabel = "Clássico relacionado",
                        score = 115,
                        signalSource = "CANONICAL_WORK"
                    )
                } else null
            }
        } else emptyList()

        val prioritized = nextInSeries.take(2) +
            sameSeriesArc.take(2) +
            sameAuthor.take(2) +
            sameCreditedCharacter.take(2) +
            canonicalRelated.take(2) +
            nextInSeries.drop(2) +
            sameSeriesArc.drop(2) +
            sameAuthor.drop(2) +
            sameCreditedCharacter.drop(2) +
            canonicalRelated.drop(2)

        val combinedPrimary = deduplicateRelated(prioritized)
        if (combinedPrimary.isNotEmpty()) {
            return combinedPrimary.take(8)
        }

        // Fallback Tier 3: Same publisher / universe only if no primary matches exist
        val samePublisher = if (normPub.length >= 3) {
            candidates.filter {
                val p = CatalogSearchPolicy.normalize(it.publisher)
                p.isNotBlank() && (p == normPub || p.contains(normPub) || normPub.contains(p))
            }.map { c ->
                RecommendedComic(
                    comic = c,
                    reasonLabel = "Mesmo universo",
                    score = 80,
                    signalSource = "SAME_PUBLISHER"
                )
            }
        } else emptyList()

        return deduplicateRelated(samePublisher).take(8)
    }

    private fun deduplicateRelated(items: List<RecommendedComic>): List<RecommendedComic> {
        val result = mutableListOf<RecommendedComic>()
        val seenKeys = HashSet<String>()
        for (item in items) {
            val key = item.comic.externalId.ifBlank { item.comic.id }
            if (key.isNotBlank() && !seenKeys.add(key)) continue
            val duplicateIndex = result.indexOfFirst { existing ->
                CatalogSearchPolicy.shouldMergeEntries(existing.comic, item.comic)
            }
            if (duplicateIndex >= 0) {
                val existing = result[duplicateIndex]
                val mergedComic = CatalogSearchPolicy.mergeComplementaryEditionData(existing.comic, item.comic)
                result[duplicateIndex] = existing.copy(comic = mergedComic)
            } else {
                result.add(item)
            }
        }
        return result
    }

    fun creditRole(raw: String): String {
        val labels = mapOf("writer" to "Roteiro", "artist" to "Arte", "penciler" to "Desenho",
            "inker" to "Arte-final", "colorist" to "Cores", "letterer" to "Letras",
            "editor" to "Edição", "cover" to "Capa")
        return raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            .map { labels[it.lowercase(Locale.ROOT)] ?: it }.distinct().joinToString(", ")
    }
}
