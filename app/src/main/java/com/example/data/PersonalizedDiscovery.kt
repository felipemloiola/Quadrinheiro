package com.example.data

import com.example.model.ComicEntry
import com.example.model.ReadingStatus

/** Local ranking: no user history leaves the device to generate recommendations. */
object PersonalizedDiscovery {
    fun recommend(catalog: List<ComicEntry>, library: List<ComicEntry>, favoriteUniverse: String = ""): List<ComicEntry> {
        val owned = library.mapTo(HashSet()) { PortugueseComicTranslator.canonicalWorkKey(it.displayTitle) }
        val candidates = catalog.filter { PortugueseComicTranslator.canonicalWorkKey(it.displayTitle) !in owned }
        val history = library.filter { it.status == ReadingStatus.READ }
            .sortedWith(compareByDescending<ComicEntry> { it.readDate.ifBlank { it.addedAt } }.thenByDescending { it.updatedAt })
            .take(8)
        val scores = HashMap<String, Float>()
        val publisherWords = favoriteUniverse.lowercase().split(Regex("[^\\p{L}\\p{N}]+" )).filter { it.length > 1 }
        candidates.forEach { candidate ->
            if (publisherWords.any { candidate.publisher.contains(it, true) || candidate.genre.contains(it, true) }) {
                scores[PortugueseComicTranslator.canonicalWorkKey(candidate.displayTitle)] = 3f
            }
        }
        history.forEachIndexed { index, read ->
            val weight = (if (read.rating > 0f) read.rating.coerceIn(.5f, 5f) / 5f else .65f) * (1f - index * .06f)
            SeriesSequenceEngine.findRecommendationsForComic(read, library, candidates).forEachIndexed { rank, suggestion ->
                val key = PortugueseComicTranslator.canonicalWorkKey(suggestion.comic.displayTitle)
                scores[key] = (scores[key] ?: 0f) + (10 - rank) * weight
            }
        }
        return candidates.sortedByDescending { scores[PortugueseComicTranslator.canonicalWorkKey(it.displayTitle)] ?: 0f }
    }
}
