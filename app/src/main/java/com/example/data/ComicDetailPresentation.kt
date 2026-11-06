package com.example.data

import com.example.model.ComicEntry
import java.util.Locale

internal object ComicDetailPresentation {
    /** Returns related editions: first from the provider resource, falling back to same series/creators/publisher from catalog pool. */
    fun relatedEditions(comic: ComicEntry, detail: CatalogDetails?, catalogPool: List<ComicEntry> = emptyList()): List<ComicEntry> {
        val current = setOf(comic.id, comic.externalId).filter { it.isNotBlank() }.toSet()
        val direct = detail?.issues.orEmpty().filter { candidate ->
            candidate.id.isNotBlank() && candidate.id !in current &&
                (candidate.externalId.isBlank() || candidate.externalId !in current)
        }
        if (direct.isNotEmpty()) {
            return direct.distinctBy { it.externalId.ifBlank { it.id } }.take(8)
        }

        if (catalogPool.isEmpty()) return emptyList()

        val authors = comic.authors.ifBlank { detail?.creators.orEmpty().filter { it.role.contains("Roteiro", true) || it.role.contains("writer", true) }.take(2).joinToString(", ") { it.name } }
        val series = comic.seriesName.ifBlank { comic.displayTitle.substringBefore(':').trim() }
        val normSeries = CatalogSearchPolicy.normalize(series)
        val normAuthors = CatalogSearchPolicy.normalize(authors)
        val normPub = CatalogSearchPolicy.normalize(comic.publisher.ifBlank { detail?.publisher.orEmpty() })

        val candidates = catalogPool.filter { c ->
            c.id !in current && (c.externalId.isBlank() || c.externalId !in current) &&
                !c.title.equals(comic.title, ignoreCase = true)
        }

        // Tier 1: Same series
        val sameSeries = if (normSeries.length >= 3) {
            candidates.filter {
                val s = CatalogSearchPolicy.normalize(it.seriesName.ifBlank { it.displayTitle.substringBefore(':').trim() })
                s.isNotBlank() && (s == normSeries || s.contains(normSeries) || normSeries.contains(s))
            }
        } else emptyList()

        // Tier 2: Same author/creators
        val sameAuthor = if (normAuthors.length >= 4) {
            candidates.filter {
                val a = CatalogSearchPolicy.normalize(it.authors)
                a.isNotBlank() && (a.contains(normAuthors) || normAuthors.contains(a))
            }
        } else emptyList()

        val primaryMatches = (sameSeries + sameAuthor).distinctBy { it.externalId.ifBlank { it.id } }
        if (primaryMatches.isNotEmpty()) {
            return primaryMatches.take(8)
        }

        // Tier 3: Same publisher / universe only if no series or author matches exist
        val samePublisher = if (normPub.length >= 3) {
            candidates.filter {
                val p = CatalogSearchPolicy.normalize(it.publisher)
                p.isNotBlank() && (p == normPub || p.contains(normPub) || normPub.contains(p))
            }
        } else emptyList()

        return samePublisher.distinctBy { it.externalId.ifBlank { it.id } }.take(8)
    }

    fun creditRole(raw: String): String {
        val labels = mapOf("writer" to "Roteiro", "artist" to "Arte", "penciler" to "Desenho",
            "inker" to "Arte-final", "colorist" to "Cores", "letterer" to "Letras",
            "editor" to "Edição", "cover" to "Capa")
        return raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            .map { labels[it.lowercase(Locale.ROOT)] ?: it }.distinct().joinToString(", ")
    }
}
