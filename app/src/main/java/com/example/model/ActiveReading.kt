package com.example.model

/** The home card, ambient light and widget must resolve the same active reading. */
fun activeReadingComic(comics: List<ComicEntry>, activeId: String?): ComicEntry? {
    val reading = comics.filter { it.status == ReadingStatus.READING }
    return reading.firstOrNull { it.id == activeId || (it.externalId.isNotBlank() && it.externalId == activeId) }
        ?: reading.firstOrNull()
}
