package com.example.model

internal object CatalogPagePolicy {
    const val MAX_PAGES = 20000
    fun count(raw: String): Int? = raw.toIntOrNull()?.takeIf { it in 1..MAX_PAGES }
    fun remoteCount(current: Int, saved: Boolean, manuallyEdited: Boolean, remote: Int): Int? =
        remote.takeIf { it in 1..MAX_PAGES && !manuallyEdited && (!saved || current <= 1) }
    fun withCount(comic: ComicEntry, count: Int): ComicEntry {
        require(count in 0..MAX_PAGES)
        val current = when (comic.status) {
            ReadingStatus.READ -> count
            ReadingStatus.WANT_TO_READ -> 0
            ReadingStatus.READING -> comic.currentPage.coerceIn(0, count)
        }
        return comic.copy(pageCount = count, currentPage = current)
    }
}
