package com.example.data

import com.example.model.ComicEntry

/** Stable, unique rows; retain a bounded visible window without discarding the provider cursor. */
class CatalogWindow(initial: List<ComicEntry> = emptyList()) {
    private val seen = HashSet<String>()
    var items: List<ComicEntry> = emptyList(); private set
    var nextPage: Int = 0; private set
    var hasMore = true; private set
    var autoLoad = true; private set
    init { append(initial) }
    private fun keys(c: ComicEntry): List<String> = buildList {
        if (c.id.isNotBlank()) add("id:" + c.id)
        add("work:" + SeriesSequenceEngine.canonicalDeduplicationKey(c))
        if (c.externalId.isNotBlank()) add("external:" + c.externalId)
        if (c.isbn.isNotBlank()) add("isbn:" + c.isbn.filter(Char::isDigit))
    }
    private fun append(page: List<ComicEntry>): Int {
        val fresh = page.filter { comic -> val keys = keys(comic); if (keys.any { it in seen }) false else { seen.addAll(keys); true } }
        val combined = items + fresh
        val drop = if (combined.size > 396) ((combined.size - 396 + 2) / 3) * 3 else 0
        items = combined.drop(drop)
        return fresh.size
    }
    fun accept(page: MultiApiSearchEngine.CatalogPage) {
        autoLoad = append(page.items) > 0
        nextPage++
        hasMore = page.hasMore && seen.size < 30000
    }
}
