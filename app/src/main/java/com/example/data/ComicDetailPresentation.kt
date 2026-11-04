package com.example.data

import com.example.model.ComicEntry
import java.util.Locale

internal object ComicDetailPresentation {
    /** Only relationships explicitly returned for this resource; no generic catalog fallback. */
    fun relatedEditions(comic: ComicEntry, detail: CatalogDetails?): List<ComicEntry> {
        val current = setOf(comic.id, comic.externalId).filter { it.isNotBlank() }.toSet()
        return detail?.issues.orEmpty().filter { candidate ->
            candidate.id.isNotBlank() && candidate.id !in current &&
                (candidate.externalId.isBlank() || candidate.externalId !in current)
        }.distinctBy { it.externalId.ifBlank { it.id } }.take(8)
    }

    fun creditRole(raw: String): String {
        val labels = mapOf("writer" to "Roteiro", "artist" to "Arte", "penciler" to "Desenho",
            "inker" to "Arte-final", "colorist" to "Cores", "letterer" to "Letras",
            "editor" to "Edição", "cover" to "Capa")
        return raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            .map { labels[it.lowercase(Locale.ROOT)] ?: it }.distinct().joinToString(", ")
    }
}
