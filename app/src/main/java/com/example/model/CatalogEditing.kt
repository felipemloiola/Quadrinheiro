package com.example.model

/** Catalog metadata never replaces the reader's evaluation, diary or progress. */
fun catalogEntryForEditing(candidate: ComicEntry, library: List<ComicEntry>): ComicEntry? {
    val owned = library.firstOrNull { candidate.id.isNotBlank() && it.id == candidate.id }
        ?: library.firstOrNull { candidate.externalId.isNotBlank() && it.externalId == candidate.externalId }
        ?: candidate.displayTitle.takeIf { it.isNotBlank() }?.let { title ->
            val workKey = com.example.data.PortugueseComicTranslator.canonicalWorkKey(title)
            library.firstOrNull {
                com.example.data.PortugueseComicTranslator.canonicalWorkKey(it.displayTitle) == workKey &&
                    (candidate.issueNumber <= 0 || it.issueNumber == candidate.issueNumber)
            }
        }
        ?: return null
    return owned.copy(
        coverUrl = candidate.coverUrl.ifBlank { owned.coverUrl },
        externalId = candidate.externalId.ifBlank { owned.externalId },
        publisher = candidate.publisher.ifBlank { owned.publisher },
        sourceLabel = candidate.sourceLabel.ifBlank { owned.sourceLabel }
    )
}
