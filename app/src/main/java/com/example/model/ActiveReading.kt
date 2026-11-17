package com.example.model

/** The home card, ambient light and widget must resolve the same active reading. */
fun activeReadingComic(comics: List<ComicEntry>, activeId: String?): ComicEntry? {
    return activeId?.takeIf { it.isNotBlank() }?.let { id ->
        comics.firstOrNull {
            it.status == ReadingStatus.READING && (it.id == id || it.externalId == id)
        }
    } ?: comics.firstOrNull { it.status == ReadingStatus.READING }
}
