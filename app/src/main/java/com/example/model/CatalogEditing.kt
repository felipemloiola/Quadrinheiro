package com.example.model

/** Catalog metadata never replaces the reader's evaluation, diary or progress. */
fun catalogEntryForEditing(candidate: ComicEntry, library: List<ComicEntry>): ComicEntry? {
    val owned = library.firstOrNull { it.id == candidate.id || (candidate.externalId.isNotBlank() && it.externalId == candidate.externalId) } ?: return null
    return owned.copy(coverUrl = candidate.coverUrl.ifBlank { owned.coverUrl }, externalId = candidate.externalId.ifBlank { owned.externalId },
        publisher = candidate.publisher.ifBlank { owned.publisher }, sourceLabel = candidate.sourceLabel.ifBlank { owned.sourceLabel })
}
