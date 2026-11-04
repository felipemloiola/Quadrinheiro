package com.example.data

import com.example.model.AppSettings
import com.example.model.ComicEntry
import com.example.model.ReadingStatus

/** Metadata ranking only. No account history or favorite list is sent to a provider. */
object PersonalizedDiscovery {
    private val stopWords = setOf("the", "and", "uma", "com", "para", "das", "dos", "vol", "volume", "edicao", "edition", "deluxe", "encadernado")
    private val genericGenres = setOf("graphic novel", "quadrinhos", "comics", "hq", "edicao", "encadernado", "colecao", "collection")
    private data class Features(val comic: ComicEntry, val title: String, val titleWords: Set<String>, val series: String,
        val authors: Set<String>, val genres: Set<String>, val publisher: String, val text: String)
    private fun normalized(raw: String) = CatalogSearchPolicy.normalize(raw)
    private fun workKey(c: ComicEntry): String {
        val title = PortugueseComicTranslator.canonicalWorkKey(c.displayTitle).ifBlank { normalized(c.displayTitle).ifBlank { c.externalId.ifBlank { c.id } } }
        return title + if (c.issueNumber > 0) ":#${c.issueNumber}" else ""
    }
    private fun features(c: ComicEntry): Features {
        val title = normalized(c.displayTitle)
        val authors = c.authors.split(',', ';', '&').map(::normalized)
            .filter { it.length >= 4 && it !in setOf("autor", "autor desconhecido", "unknown", "various", "autores variados") }.toSet()
        val genres = c.genre.split(',', ';', '/', '|').map(::normalized).filter { it.isNotBlank() && it !in genericGenres }.toSet()
        return Features(c, title, title.split(' ').filter { it.length >= 3 && it.any(Char::isLetter) && it !in stopWords }.toSet(),
            normalized(c.seriesName), authors, genres, normalized(c.publisher),
            " " + normalized(c.displayTitle + " " + c.seriesName + " " + c.synopsisPtBr.ifBlank { c.synopsis }.take(6000)) + " ")
    }
    private fun affinity(candidate: Features, seed: Features): Float {
        var score = 0f
        if (candidate.authors.any { it in seed.authors }) score += 5f
        if (seed.series.isNotBlank() && candidate.series == seed.series) score += 7f
        val overlap = candidate.titleWords.count { it in seed.titleWords }
        if (overlap >= 2) score += 4f
        else if (overlap == 1 && (seed.titleWords.size == 1 ||
                (seed.titleWords.firstOrNull()?.length ?: 0) >= 4 && seed.titleWords.firstOrNull() == candidate.titleWords.firstOrNull())) score += 2f
        score += candidate.genres.count { it in seed.genres }.coerceAtMost(2) * 1.2f
        if (score > 0f && seed.publisher.isNotBlank() && candidate.publisher == seed.publisher) score += 1f
        return score
    }
    private fun aliases(character: String): List<String> {
        val name = character.substringBefore('(').trim()
        val civil = character.substringAfter('(', "").substringBefore(')').trim()
        val result = mutableListOf(name, civil)
        CharacterDirectory.portuguese.forEach { (english, portuguese) ->
            if (normalized(english) == normalized(name) || normalized(portuguese) == normalized(name)) { result.add(english); result.add(portuguese) }
        }
        return result.map(::normalized).filter { it.length >= 3 }.distinct()
    }
    private fun preferred(candidate: Features, preference: String): Boolean {
        val p = normalized(preference)
        if (p.isBlank()) return false
        if (p == "mangas" || p == "manga") return candidate.genres.any { it.contains("manga") }
        if (p == "quadrinhos nacionais") return candidate.genres.any { it.contains("nacion") || it.contains("brasileir") }
        if (p == "independentes") return candidate.genres.any { it.contains("independen") }
        return p.split(' ').filter { it.length >= 2 }.any { word ->
            " ${candidate.publisher} ".contains(" $word ") || candidate.genres.any { " $it ".contains(" $word ") }
        }
    }

    fun recommend(catalog: List<ComicEntry>, library: List<ComicEntry>, favoriteUniverse: String = "", settings: AppSettings = AppSettings()): List<ComicEntry> {
        val pool = catalog.takeLast(396)
        val snapshots = settings.profileFavoriteComicIds.mapNotNull { id -> settings.profileShowcaseDetails["comic:$id"]?.let(ProfileShowcase::restoreComic) }
        val favorites = settings.profileFavoriteComicIds.take(4).mapNotNull { id ->
            library.firstOrNull { it.id == id || it.externalId == id } ?: snapshots.firstOrNull { it.id == id } ?: pool.firstOrNull { it.id == id || it.externalId == id }
        }
        val known = library + favorites
        val ownedIds = (known.flatMap { listOf(it.id, it.externalId) } + settings.profileFavoriteComicIds).filter { it.isNotBlank() }.toSet()
        val ownedWorks = known.mapTo(HashSet(), ::workKey)
        val history = library.filter { it.status == ReadingStatus.READ }
            .sortedWith(compareByDescending<ComicEntry> { it.readDate.ifBlank { it.addedAt } }.thenByDescending { it.updatedAt }).take(12)
        val seeds = favorites.map { features(it) to 3f }.toMutableList()
        history.forEachIndexed { index, read ->
            val weight = when {
                read.favorite -> 1.8f
                read.rating >= 4f -> 1.4f
                read.rating > 0f && read.rating <= 2f -> -.7f
                read.rating > 0f -> .7f
                else -> .6f
            } * (1f - index * .04f)
            seeds.add(features(read) to weight)
        }
        library.filter { it.status == ReadingStatus.READING }.take(4).forEach { seeds.add(features(it) to .5f) }
        val characters = settings.profileFavoriteCharacters.take(4).map(::aliases)
        val candidates = CatalogSearchPolicy.unique(pool).filter { c ->
            c.id !in ownedIds && (c.externalId.isBlank() || c.externalId !in ownedIds) && workKey(c) !in ownedWorks
        }.distinctBy(::workKey).map(::features)
        val ranked = candidates.mapIndexed { index, c ->
            var score = if (preferred(c, favoriteUniverse)) 4f else 0f
            for ((seed, weight) in seeds) score += affinity(c, seed) * weight
            for (names in characters) {
                if (names.any { " ${c.title} ${c.series} ".contains(" $it ") }) score += 24f
                else if (names.any { c.text.contains(" $it ") }) score += 12f
            }
            Triple(c.comic, score, index)
        }.sortedWith(compareByDescending<Triple<ComicEntry, Float, Int>> { it.second }
            .thenByDescending { it.first.coverUrl.isNotBlank() }.thenBy { it.third })
        val relevant = ranked.filter { it.second > 0f }
        val hasSignals = favoriteUniverse.isNotBlank() || settings.profileFavoriteComicIds.isNotEmpty() || characters.isNotEmpty() || seeds.isNotEmpty()
        return if (relevant.isNotEmpty()) relevant.take(48).map { it.first }
            else if (hasSignals) emptyList() else ranked.take(24).map { it.first }
    }
}
