package com.example.data

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.example.model.ComicEntry
import com.example.model.ReadingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer

@Immutable
data class SequenceIssueItem(
    val comic: ComicEntry,
    val issueNumber: Int,
    val isSavedInLibrary: Boolean,
    val savedEntry: ComicEntry?,
    val fractionalSubIssue: Int = 0
)

@Stable
@Immutable
data class SeriesSequenceGroup(
    val sequenceKey: String,
    val baseSeriesTitle: String,
    val primaryAuthor: String,
    val displayTitle: String,
    val issues: List<SequenceIssueItem>,
    val nextIssueNumber: Int,
    val missingIssueNumbers: List<Int> = emptyList(),
    val isRecommendationMode: Boolean = false,
    val recommendationSubtitle: String = "",
    val recommendations: List<SequenceIssueItem> = emptyList()
) {
    val savedCount: Int get() = if (isRecommendationMode) recommendations.count { it.isSavedInLibrary } else issues.count { it.isSavedInLibrary }
    val readCount: Int get() = if (isRecommendationMode) recommendations.count { it.savedEntry?.status == ReadingStatus.READ } else issues.count { it.savedEntry?.status == ReadingStatus.READ }
    val totalCount: Int get() = if (isRecommendationMode) recommendations.size else issues.size
}

object SeriesSequenceEngine {

    private val wordNumbersMap = mapOf(
        "um" to 1, "uma" to 1, "one" to 1, "i" to 1,
        "dois" to 2, "duas" to 2, "two" to 2, "ii" to 2,
        "tres" to 3, "três" to 3, "three" to 3, "iii" to 3,
        "quatro" to 4, "four" to 4, "iv" to 4,
        "cinco" to 5, "five" to 5, "v" to 5,
        "seis" to 6, "six" to 6, "vi" to 6,
        "sete" to 7, "seven" to 7, "vii" to 7,
        "oito" to 8, "eight" to 8, "viii" to 8,
        "nove" to 9, "nine" to 9, "ix" to 9,
        "dez" to 10, "ten" to 10, "x" to 10
    )

    // Todas as expressões regulares pré-compiladas como constantes private val no topo do objeto
    // (evitando recriar instâncias de Regex dentro de .map {} ou .sortedWith {} a cada recomposição)
    private val hashIssueRegex = Regex("""#\s*0*(\d{1,4})(?:\.(\d{1,2}))?\b""")
    private val annualIssueRegex = Regex("""(?i)\b(?:anual|annual)\s*#?\s*0*(\d{1,3})\b""")
    private val volIssueRegex = Regex(
        """(?i)\b(?:vol\.?|volume|livro|book|ed\.?|edi[cç][aã]o|issue|parte|part|tomo|cap[ií]tulo|bd\.?|band|n[º°.]?)\s*#?\s*0*(\d{1,4})(?:\.(\d{1,2}))?\b"""
    )
    private val wordVolRegex = Regex(
        """(?i)\b(?:vol\.?|volume|livro|book|parte|part)\s+(um|uma|dois|duas|tr[eê]s|quatro|cinco|seis|sete|oito|nove|dez|one|two|three|four|five|six|seven|eight|nine|ten|i|ii|iii|iv|v|vi|vii|viii|ix|x)\b"""
    )
    private val trailingNumRegex = Regex("""(?:^|[\s—–-])0*(\d{1,3})\s*$""")
    private val diacriticsRegex = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val parentheticalMetaRegex = Regex(
        """(?i)\(\s*(?:vol\.?|volume|livro|book|ed\.?|edi[cç][aã]o|vizbig|tpb|capa dura|#|19\d\d|20\d\d).*?\)"""
    )
    private val editionSuffixesRegex = Regex(
        """(?i)\s*(?::|-|—|–)?\s*(?:edi[cç][aã]o\s+definitiva|edi[cç][aã]o\s+de\s+luxo|the\s+perfect\s+edition|comp[eê]ndio|omnibus|deluxe\s+edition|edi[cç][aã]o\s+absoluta|absoluta)\s*$"""
    )
    private val nonAlphaNumSpaceRegex = Regex("""[^a-z0-9\s]""")
    private val whitespaceRegex = Regex("""\s+""")
    private val authorDelimiterRegex = Regex("""[,;&/]|(?i)\s+e\s+|\s+and\s+""")

    private val ignoredAuthorTokens = setOf(
        "autor desconhecido",
        "unknown author",
        "unknown",
        "various",
        "varios",
        "varios autores",
        "vários autores",
        "dc comics",
        "marvel comics",
        "image comics",
        "panini",
        "editora",
        "shueisha",
        "kodansha"
    )

    private val knownStandaloneGraphicNovels = setOf(
        "watchmen",
        "maus",
        "daytripper",
        "v de vinganca",
        "v de vingança",
        "piada mortal",
        "killing joke",
        "ano um",
        "year one",
        "persepolis",
        "persépolis",
        "retalhos",
        "blankets",
        "reino do amanha",
        "reino do amanhã",
        "kingdom come",
        "grandes astros",
        "all-star superman",
        "guerra civil",
        "civil war",
        "marvels",
        "queda de murdock",
        "born again",
        "ultima cacada de kraven",
        "última caçada de kraven",
        "kraven",
        "homem-aranha: azul",
        "spider-man: blue",
        "uzumaki",
        "contrato com deus",
        "contract with god",
        "do inferno",
        "from hell",
        "monstro do pantano",
        "monstro do pântano",
        "asilo arkham",
        "arkham asylum",
        "longo dia das bruxas",
        "long halloween",
        "fenix negra",
        "fênix negra",
        "dias de um futuro esquecido",
        "days of future past",
        "origens",
        "ghost world",
        "castelo de areia",
        "dois irmaos",
        "dois irmãos"
    )

    private val curatedAffinities: Map<String, List<String>> = mapOf(
        "watchmen" to listOf("v de vinganca", "monstro do pantano", "cavaleiro das trevas", "piada mortal", "sandman", "reino do amanha"),
        "v de vinganca" to listOf("watchmen", "monstro do pantano", "cavaleiro das trevas", "sandman", "piada mortal"),
        "monstro do pantano" to listOf("sandman", "watchmen", "v de vinganca", "hellblazer"),
        "maus" to listOf("persepolis", "daytripper", "retalhos", "watchmen", "sandman", "hellboy"),
        "persepolis" to listOf("maus", "daytripper", "retalhos", "watchmen", "sandman"),
        "daytripper" to listOf("retalhos", "persepolis", "maus", "sandman", "watchmen"),
        "retalhos" to listOf("daytripper", "persepolis", "maus", "sandman"),
        "piada mortal" to listOf("cavaleiro das trevas", "ano um", "watchmen", "v de vinganca", "queda de murdock"),
        "ano um" to listOf("cavaleiro das trevas", "piada mortal", "queda de murdock", "homem-aranha: azul", "watchmen"),
        "reino do amanha" to listOf("marvels", "grandes astros", "watchmen", "guerra civil"),
        "grandes astros" to listOf("reino do amanha", "marvels", "watchmen", "cavaleiro das trevas"),
        "queda de murdock" to listOf("ano um", "cavaleiro das trevas", "homem-aranha: azul", "watchmen"),
        "homem-aranha: azul" to listOf("queda de murdock", "ultima cacada de kraven", "marvels", "cavaleiro das trevas"),
        "ultima cacada de kraven" to listOf("homem-aranha: azul", "queda de murdock", "cavaleiro das trevas", "watchmen"),
        "marvels" to listOf("reino do amanha", "grandes astros", "guerra civil", "watchmen"),
        "guerra civil" to listOf("marvels", "reino do amanha", "invencivel", "dias de um futuro"),
        "uzumaki" to listOf("berserk", "akira", "monster", "vagabond", "vinland saga"),
        "akira" to listOf("berserk", "uzumaki", "monster", "vagabond", "vinland saga"),
        "berserk" to listOf("vagabond", "vinland saga", "monster", "uzumaki", "akira"),
        "monster" to listOf("vagabond", "berserk", "akira", "uzumaki", "vinland saga"),
        "vagabond" to listOf("berserk", "vinland saga", "monster", "akira", "uzumaki"),
        "sandman" to listOf("watchmen", "monstro do pantano", "daytripper", "hellboy", "saga"),
        "hellboy" to listOf("sandman", "watchmen", "monstro do pantano", "saga"),
        "saga" to listOf("sandman", "invencivel", "hellboy", "daytripper")
    )

    private val primaryAuthorCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val issueNumberCache = java.util.concurrent.ConcurrentHashMap<String, Int>()
    private val baseSeriesCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val normalizeKeyCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    private val catalogByNormalizedSeries: Map<String, List<ComicEntry>> by lazy {
        CuratedCatalog.curatedComicsCatalog.groupBy { normalizeKey(inferBaseSeriesTitle(it)) }
    }

    fun stripAccents(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        return normalized.replace(diacriticsRegex, "")
    }

    fun extractPrimaryAuthor(authors: String): String {
        val trimmed = authors.trim()
        if (trimmed.isEmpty()) return ""
        primaryAuthorCache[trimmed]?.let { return it }
        val first = trimmed.split(authorDelimiterRegex)
            .map { it.trim() }
            .firstOrNull { candidate ->
                candidate.isNotBlank() && !ignoredAuthorTokens.contains(candidate.lowercase())
            }
        val result = first ?: trimmed
        if (primaryAuthorCache.size < 4096) {
            primaryAuthorCache[trimmed] = result
        }
        return result
    }

    fun inferIssueNumber(comic: ComicEntry): Int {
        if (comic.issueNumber > 0) return comic.issueNumber
        return inferIssueNumber(comic.title)
    }

    fun inferFractionalSubIssue(rawTitle: String): Int {
        val title = PortugueseComicTranslator.translateTitleToPtBr(rawTitle).trim()
        hashIssueRegex.find(title)?.groupValues?.getOrNull(2)?.toIntOrNull()?.let { return it }
        volIssueRegex.find(title)?.groupValues?.getOrNull(2)?.toIntOrNull()?.let { return it }
        return 0
    }

    fun inferIssueNumber(rawTitle: String): Int {
        val trimmedRaw = rawTitle.trim()
        if (trimmedRaw.isEmpty()) return 0
        issueNumberCache[trimmedRaw]?.let { return it }

        val computed = computeIssueNumberUncached(trimmedRaw)
        if (issueNumberCache.size < 4096) {
            issueNumberCache[trimmedRaw] = computed
        }
        return computed
    }

    private fun computeIssueNumberUncached(rawTitle: String): Int {
        val title = PortugueseComicTranslator.translateTitleToPtBr(rawTitle).trim()

        hashIssueRegex.find(title)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
            if (it > 0) return it
        }
        volIssueRegex.find(title)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
            if (it > 0) return it
        }
        annualIssueRegex.find(title)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
            if (it > 0) return it
        }
        wordVolRegex.find(title)?.groupValues?.getOrNull(1)?.lowercase()?.let { word ->
            val mapped = wordNumbersMap[word] ?: wordNumbersMap[stripAccents(word)]
            if (mapped != null) return mapped
        }
        // Check before subtitle separator as well (e.g. "Batman #1: ...", "O Cavaleiro das Trevas 2")
        val beforeColon = title.substringBefore(":").substringBefore("—").trim()
        trailingNumRegex.find(beforeColon)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
            if (it in 1..500 && it !in 1930..2035) return it
        }
        return 0
    }

    fun inferBaseSeriesTitle(comic: ComicEntry): String {
        if (comic.seriesName.isNotBlank()) {
            return PortugueseComicTranslator.translateTitleToPtBr(comic.seriesName).trim()
        }
        return inferBaseSeriesTitle(comic.title)
    }

    fun inferBaseSeriesTitle(rawTitle: String): String {
        val trimmedRaw = rawTitle.trim()
        if (trimmedRaw.isEmpty()) return ""
        baseSeriesCache[trimmedRaw]?.let { return it }

        val computed = computeBaseSeriesTitleUncached(trimmedRaw)
        if (baseSeriesCache.size < 4096) {
            baseSeriesCache[trimmedRaw] = computed
        }
        return computed
    }

    private fun computeBaseSeriesTitleUncached(rawTitle: String): String {
        var working = PortugueseComicTranslator.translateTitleToPtBr(rawTitle).trim()

        // Strip parenthetical metadata like "(Vol. 1)", "(Edição Definitiva)", "(2016)"
        working = working.replace(parentheticalMetaRegex, "").trim()

        // If title has "#N", take what comes before "#N" if non-empty
        val hashMatch = hashIssueRegex.find(working)
        if (hashMatch != null) {
            val prefix = working.substring(0, hashMatch.range.first)
                .trim()
                .trimEnd(':', '-', '—', '–', ',', '•')
                .trim()
            if (prefix.length >= 2) {
                return cleanEditionSuffixes(prefix)
            }
        }

        // If title has "Vol. N" / "Volume N" / "Livro N"
        val volMatch = volIssueRegex.find(working)
        if (volMatch != null) {
            val prefix = working.substring(0, volMatch.range.first)
                .trim()
                .trimEnd(':', '-', '—', '–', ',', '•')
                .trim()
            if (prefix.length >= 2) {
                return cleanEditionSuffixes(prefix)
            }
        }

        val wordMatch = wordVolRegex.find(working)
        if (wordMatch != null) {
            val prefix = working.substring(0, wordMatch.range.first)
                .trim()
                .trimEnd(':', '-', '—', '–', ',', '•')
                .trim()
            if (prefix.length >= 2) {
                return cleanEditionSuffixes(prefix)
            }
        }

        // Do not collapse standalone graphic novels with subtitles (e.g. "Batman: Ano Um", "Batman: Silêncio") into "Batman"
        return cleanEditionSuffixes(working)
    }

    private fun cleanEditionSuffixes(raw: String): String {
        return raw
            .replace(editionSuffixesRegex, "")
            .trim()
            .trimEnd(':', '-', '—', '–')
            .trim()
            .ifBlank { raw.trim() }
    }

    fun normalizeKey(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ""
        normalizeKeyCache[trimmed]?.let { return it }
        val computed = stripAccents(trimmed.lowercase())
            .replace(nonAlphaNumSpaceRegex, " ")
            .replace(whitespaceRegex, " ")
            .trim()
        if (normalizeKeyCache.size < 4096) {
            normalizeKeyCache[trimmed] = computed
        }
        return computed
    }

    /**
     * Chave canônica para deduplicar edições equivalentes (ex: "Batman: Ano Um" == "Batman: Year One" == "Batman: Ano Um (Edição Definitiva)").
     */
    fun canonicalDeduplicationKey(comic: ComicEntry): String {
        val workKey = PortugueseComicTranslator.canonicalWorkKey(comic.title)
        val issueNum = inferIssueNumber(comic)
        val baseSeries = normalizeKey(inferBaseSeriesTitle(comic))
        val authorKey = normalizeKey(extractPrimaryAuthor(comic.authors))
        return if (issueNum > 0) {
            "seq:$baseSeries:$authorKey:issue:$issueNum"
        } else {
            "work:$workKey:$authorKey"
        }
    }

    fun buildSequenceKey(comic: ComicEntry): String {
        val baseSeries = normalizeKey(inferBaseSeriesTitle(comic))
        val primaryAuthor = normalizeKey(extractPrimaryAuthor(comic.authors))
        return if (primaryAuthor.isNotBlank()) {
            "$baseSeries::$primaryAuthor"
        } else {
            baseSeries
        }
    }

    fun isSameSequence(a: ComicEntry, b: ComicEntry): Boolean {
        val seriesA = normalizeKey(inferBaseSeriesTitle(a))
        val seriesB = normalizeKey(inferBaseSeriesTitle(b))
        if (seriesA.isBlank() || seriesB.isBlank()) return false
        if (seriesA != seriesB) return false

        // Se ambos têm anos válidos de lançamento distantes por mais de 12 anos, são arcos/fases totalmente distintos
        if (a.releaseYear in 1930..2035 && b.releaseYear in 1930..2035) {
            if (kotlin.math.abs(a.releaseYear - b.releaseYear) > 12) {
                return false
            }
        }

        val authorA = normalizeKey(extractPrimaryAuthor(a.authors))
        val authorB = normalizeKey(extractPrimaryAuthor(b.authors))

        // Se ambos os autores forem desconhecidos ou vazios, apenas aceita se forem edições explicitamente numeradas
        if (authorA.isBlank() && authorB.isBlank()) {
            return inferIssueNumber(a) > 0 && inferIssueNumber(b) > 0
        }

        // Se um é conhecido e o outro é vazio, não assume que são da mesma fase
        if (authorA.isBlank() || authorB.isBlank()) {
            return false
        }

        if (authorA == authorB) return true

        val surnameA = authorA.substringAfterLast(" ").trim()
        val surnameB = authorB.substringAfterLast(" ").trim()
        if (surnameA.length >= 4 && surnameA == surnameB) return true

        return false
    }

    fun findSequenceForComic(
        target: ComicEntry,
        userComics: List<ComicEntry>,
        catalogComics: List<ComicEntry> = CuratedCatalog.curatedComicsCatalog,
        extraOnlineComics: List<ComicEntry> = emptyList()
    ): SeriesSequenceGroup {
        val localizedTarget = PortugueseComicTranslator.localizeComicMetadataOnly(target)
        val baseTitle = inferBaseSeriesTitle(localizedTarget)
        val normalizedBaseTitle = normalizeKey(baseTitle)
        val primaryAuthor = extractPrimaryAuthor(localizedTarget.authors)
        val seqKey = buildSequenceKey(localizedTarget)
        val targetIssue = inferIssueNumber(localizedTarget)

        // Gather matching candidates using normalized base title bucket when available
        val matchingUser = userComics.filter { isSameSequence(localizedTarget, it) }
        val catalogCandidates = if (catalogComics === CuratedCatalog.curatedComicsCatalog) {
            catalogByNormalizedSeries[normalizedBaseTitle].orEmpty()
        } else {
            catalogComics
        }
        val matchingCatalog = catalogCandidates.filter { isSameSequence(localizedTarget, it) }
        val matchingOnline = if (matchingCatalog.count { inferIssueNumber(it) > 0 } >= 3) {
            // Se o catálogo curado já possui a sequência completa verificada (ex: Matt Fraction #1-#5),
            // não polui com resultados desordenados da internet
            emptyList()
        } else {
            extraOnlineComics
                .filter { !PortugueseComicTranslator.isUnwantedForeignOrBundleResult(it.title, it.synopsis) }
                .map { PortugueseComicTranslator.localizeComicMetadataOnly(it) }
                .filter { isSameSequence(localizedTarget, it) }
        }

        val combinedPool = mutableListOf<ComicEntry>()
        combinedPool.addAll(matchingUser)
        if (matchingUser.none {
                it.id == localizedTarget.id ||
                    (localizedTarget.externalId.isNotBlank() && it.externalId == localizedTarget.externalId)
            }
        ) {
            combinedPool.add(localizedTarget)
        }
        combinedPool.addAll(matchingCatalog)
        combinedPool.addAll(matchingOnline)

        // Determina se esta é uma sequência numerada (#1, #2, #3...)
        val hasNumberedSequence = targetIssue > 0 ||
            combinedPool.count { inferIssueNumber(it) > 0 } >= 2

        // Pre-index userComics for O(1) lookup instead of scanning userComics per candidate
        val userById = HashMap<String, ComicEntry>(userComics.size * 2)
        val userByExtId = HashMap<String, ComicEntry>(userComics.size * 2)
        val userByWorkKey = HashMap<String, ComicEntry>(userComics.size * 2)
        val matchingUserByIssue = HashMap<Int, ComicEntry>()
        for (u in userComics) {
            if (u.id.isNotBlank() && !userById.containsKey(u.id)) userById[u.id] = u
            if (u.externalId.isNotBlank() && !userByExtId.containsKey(u.externalId)) userByExtId[u.externalId] = u
            val uWorkKey = PortugueseComicTranslator.canonicalWorkKey(u.title)
            if (uWorkKey.isNotBlank() && !userByWorkKey.containsKey(uWorkKey)) userByWorkKey[uWorkKey] = u
        }
        for (u in matchingUser) {
            val uIssue = inferIssueNumber(u)
            if (uIssue > 0 && !matchingUserByIssue.containsKey(uIssue)) {
                matchingUserByIssue[uIssue] = u
            }
        }

        // Deduplica rigorosamente por número da edição E por título canônico em português
        // (impedindo "Batman: Ano Um" + "Batman: Year One" ou múltiplas edições da mesma obra)
        val byIssueKey = LinkedHashMap<Int, SequenceIssueItem>()
        val byCanonicalWorkKey = LinkedHashMap<String, SequenceIssueItem>()

        for (rawCandidate in combinedPool) {
            val candidate = PortugueseComicTranslator.localizeComicMetadataOnly(rawCandidate)
            val issueNum = inferIssueNumber(candidate)
            val workKey = PortugueseComicTranslator.canonicalWorkKey(candidate.title)

            // Se a sequência principal é numerada (#1, #2, #3...), ignora edições avulsas sem número vindas da internet
            if (hasNumberedSequence && issueNum <= 0 && candidate.id != localizedTarget.id) {
                val isUserSaved = userById.containsKey(rawCandidate.id)
                if (!isUserSaved) continue
            }

            val savedMatch = (if (rawCandidate.id.isNotBlank()) userById[rawCandidate.id] else null)
                ?: (if (rawCandidate.externalId.isNotBlank()) userByExtId[rawCandidate.externalId] else null)
                ?: (if (issueNum > 0) matchingUserByIssue[issueNum] else null)
                ?: userByWorkKey[workKey]

            val effectiveComic = if (savedMatch != null) {
                PortugueseComicTranslator.localizeComicMetadataOnly(savedMatch)
            } else {
                candidate
            }

            val fracNum = inferFractionalSubIssue(candidate.title)
            val item = SequenceIssueItem(
                comic = effectiveComic,
                issueNumber = issueNum,
                isSavedInLibrary = savedMatch != null,
                savedEntry = savedMatch,
                fractionalSubIssue = fracNum
            )

            if (issueNum > 0) {
                val existingByIssue = byIssueKey[issueNum]
                val existingByWork = byCanonicalWorkKey[workKey]
                if (existingByWork != null && existingByWork.issueNumber != issueNum) {
                    // Mesma obra canônica já adicionada
                    if (!existingByWork.isSavedInLibrary && item.isSavedInLibrary) {
                        byCanonicalWorkKey[workKey] = item
                        byIssueKey[issueNum] = item
                    }
                    continue
                }
                if (existingByIssue == null || (!existingByIssue.isSavedInLibrary && item.isSavedInLibrary)) {
                    byIssueKey[issueNum] = item
                    byCanonicalWorkKey[workKey] = item
                }
            } else {
                val existingWork = byCanonicalWorkKey[workKey]
                if (existingWork == null || (!existingWork.isSavedInLibrary && item.isSavedInLibrary)) {
                    byCanonicalWorkKey[workKey] = item
                }
            }
        }

        val mergedUnique = if (hasNumberedSequence && byIssueKey.isNotEmpty()) {
            val list = byIssueKey.values.toMutableList()
            val existingWorkKeys = list.mapTo(HashSet()) { PortugueseComicTranslator.canonicalWorkKey(it.comic.title) }
            // Se o próprio item aberto não tinha número mas pertence ao grupo, mantém apenas ele sem duplicar
            byCanonicalWorkKey.values.forEach { unnumbered ->
                val unnumberedKey = PortugueseComicTranslator.canonicalWorkKey(unnumbered.comic.title)
                if (unnumbered.issueNumber <= 0 &&
                    (unnumbered.comic.id == localizedTarget.id || unnumbered.isSavedInLibrary) &&
                    existingWorkKeys.add(unnumberedKey)
                ) {
                    list.add(unnumbered)
                }
            }
            list
        } else {
            byCanonicalWorkKey.values.toList()
        }

        val sortedIssues = mergedUnique.sortedWith(
            compareBy<SequenceIssueItem> { if (it.issueNumber > 0) 0 else 1 }
                .thenBy { if (it.issueNumber > 0) it.issueNumber else Int.MAX_VALUE }
                .thenBy { it.fractionalSubIssue }
                .thenBy { it.comic.releaseYear }
                .thenBy { it.comic.title }
        ).take(12)

        val maxIssue = sortedIssues.maxOfOrNull { it.issueNumber } ?: targetIssue
        val savedIssuesSet = sortedIssues.filter { it.isSavedInLibrary && it.issueNumber > 0 }.mapTo(HashSet()) { it.issueNumber }
        val missingIssues = if (maxIssue > 1) {
            (1..maxIssue).filter { it !in savedIssuesSet }
        } else {
            emptyList()
        }
        // Sugere a primeira edição faltante ("buraco" na coleção) ou a edição seguinte à última
        val nextIssueNumber = missingIssues.firstOrNull()
            ?: if (maxIssue > 0) maxIssue + 1 else (sortedIssues.size + 1)

        val displayTitle = if (primaryAuthor.isNotBlank() && !primaryAuthor.equals("Autor Desconhecido", ignoreCase = true)) {
            "$baseTitle • $primaryAuthor"
        } else {
            baseTitle
        }

        val isStandalone = isStandaloneGraphicNovelOrSingle(localizedTarget, sortedIssues)
        val recommendations = if (isStandalone) {
            findRecommendationsForComic(
                target = localizedTarget,
                userComics = userComics,
                catalogComics = catalogComics
            )
        } else {
            emptyList()
        }

        val recommendationSubtitle = if (isStandalone) {
            val formatLower = localizedTarget.format.lowercase()
            val genreLower = localizedTarget.genre.lowercase()
            if (formatLower.contains("graphic novel") || genreLower.contains("graphic novel") || isKnownStandaloneWork(localizedTarget.title)) {
                "Quem leu esta Graphic Novel também curtiu"
            } else {
                "Títulos semelhantes que você pode gostar"
            }
        } else {
            ""
        }

        return SeriesSequenceGroup(
            sequenceKey = seqKey,
            baseSeriesTitle = baseTitle,
            primaryAuthor = primaryAuthor,
            displayTitle = displayTitle,
            issues = sortedIssues,
            nextIssueNumber = nextIssueNumber,
            missingIssueNumbers = missingIssues,
            isRecommendationMode = isStandalone,
            recommendationSubtitle = recommendationSubtitle,
            recommendations = recommendations
        )
    }

    fun isKnownStandaloneWork(title: String): Boolean {
        val norm = normalizeKey(title)
        return knownStandaloneGraphicNovels.any { norm.contains(it) }
    }

    fun isStandaloneGraphicNovelOrSingle(
        target: ComicEntry,
        sequenceIssues: List<SequenceIssueItem>
    ): Boolean {
        // Se houver 2 ou mais edições distintas numeradas (#1, #2, #3...), é sequência serializada
        val distinctNumbered = sequenceIssues.count { it.issueNumber > 0 }
        if (distinctNumbered >= 2) return false

        val formatLower = target.format.lowercase()
        val genreLower = target.genre.lowercase()
        val titleLower = target.title.lowercase()

        val isGraphicNovel = formatLower.contains("graphic novel") ||
            genreLower.contains("graphic novel") ||
            formatLower.contains("encadernado") ||
            formatLower.contains("capa dura") ||
            formatLower.contains("volume único") ||
            formatLower.contains("volume unico") ||
            formatLower.contains("edição definitiva") ||
            formatLower.contains("edicao definitiva") ||
            formatLower.contains("one-shot") ||
            formatLower.contains("oneshot") ||
            titleLower.contains("graphic novel")

        val isKnown = isKnownStandaloneWork(target.title)

        // Se tem apenas 1 edição e não possui nome de série serializado ou é graphic novel conhecida
        return isGraphicNovel || isKnown || (sequenceIssues.size <= 1 && target.seriesName.isBlank())
    }

    fun findRecommendationsForComic(
        target: ComicEntry,
        userComics: List<ComicEntry>,
        catalogComics: List<ComicEntry> = CuratedCatalog.curatedComicsCatalog
    ): List<SequenceIssueItem> {
        val targetNormTitle = normalizeKey(target.title)
        val targetWorkKey = PortugueseComicTranslator.canonicalWorkKey(target.title)
        val targetAuthor = extractPrimaryAuthor(target.authors).lowercase().trim()
        val targetGenre = target.genre.lowercase().trim()
        val targetPublisher = target.publisher.lowercase().trim()

        val userByWorkKey = HashMap<String, ComicEntry>(userComics.size * 2)
        val userById = HashMap<String, ComicEntry>(userComics.size * 2)
        val userByExtId = HashMap<String, ComicEntry>(userComics.size * 2)
        for (u in userComics) {
            if (u.id.isNotBlank()) userById[u.id] = u
            if (u.externalId.isNotBlank()) userByExtId[u.externalId] = u
            val wk = PortugueseComicTranslator.canonicalWorkKey(u.title)
            if (wk.isNotBlank() && !userByWorkKey.containsKey(wk)) userByWorkKey[wk] = u
        }

        val affinities = curatedAffinities.entries.firstOrNull { (key, _) ->
            targetWorkKey.contains(key) || targetNormTitle.contains(key)
        }?.value.orEmpty()

        val pool = LinkedHashMap<String, ComicEntry>()
        for (c in catalogComics) {
            val wk = PortugueseComicTranslator.canonicalWorkKey(c.title)
            if (wk.isNotBlank() && !pool.containsKey(wk)) pool[wk] = c
        }
        for (u in userComics) {
            val wk = PortugueseComicTranslator.canonicalWorkKey(u.title)
            if (wk.isNotBlank() && !pool.containsKey(wk)) pool[wk] = u
        }

        pool.remove(targetWorkKey)
        if (target.id.isNotBlank()) pool.remove(PortugueseComicTranslator.canonicalWorkKey(target.id))

        class ScoredCandidate(val comic: ComicEntry, val score: Int, val isCurated: Boolean)
        val scored = mutableListOf<ScoredCandidate>()

        for ((workKey, rawCandidate) in pool) {
            val candidate = PortugueseComicTranslator.localizeComicMetadataOnly(rawCandidate)
            if (candidate.id == target.id ||
                (target.externalId.isNotBlank() && candidate.externalId == target.externalId) ||
                workKey == targetWorkKey
            ) continue

            var score = 0
            var isCurated = false

            val affinityIndex = affinities.indexOfFirst { aff ->
                workKey.contains(aff) || normalizeKey(candidate.title).contains(aff)
            }
            if (affinityIndex >= 0) {
                score += 150 - (affinityIndex * 15)
                isCurated = true
            }

            val candAuthor = extractPrimaryAuthor(candidate.authors).lowercase().trim()
            if (candAuthor.isNotBlank() && targetAuthor.isNotBlank()) {
                if (candAuthor == targetAuthor) {
                    score += 80
                } else if (candidate.authors.contains(targetAuthor, ignoreCase = true) || target.authors.contains(candAuthor, ignoreCase = true)) {
                    score += 50
                }
            }

            val candGenre = candidate.genre.lowercase().trim()
            if (candGenre.isNotBlank() && targetGenre.isNotBlank()) {
                if (candGenre == targetGenre) {
                    score += 35
                } else if ((targetGenre.contains("vertigo") && candGenre.contains("vertigo")) ||
                    (targetGenre.contains("manga") && candGenre.contains("manga")) ||
                    (targetGenre.contains("graphic novel") && candGenre.contains("graphic novel"))
                ) {
                    score += 30
                }
            }

            val candFormat = candidate.format.lowercase()
            if (candFormat.contains("graphic novel") || candFormat.contains("capa dura") || candGenre.contains("graphic novel")) {
                score += 20
            }

            val candPub = candidate.publisher.lowercase().trim()
            if (candPub.isNotBlank() && targetPublisher.isNotBlank()) {
                if (candPub.contains(targetPublisher) || targetPublisher.contains(candPub)) {
                    score += 15
                }
            }

            if (isKnownStandaloneWork(candidate.title)) {
                score += 25
            }

            // Evita edições extras da mesma série exata
            if (isSameSequence(target, candidate)) {
                continue
            }

            scored.add(ScoredCandidate(candidate, score, isCurated))
        }

        var topCandidates = scored.sortedWith(
            compareByDescending<ScoredCandidate> { it.score }
                .thenByDescending { it.isCurated }
                .thenByDescending { it.comic.releaseYear }
        ).map { it.comic }.take(8)

        // Fallback para assegurar um carrossel rico se houver poucos candidatos
        if (topCandidates.size < 5) {
            val fallbackCatalog = catalogComics.filter { c ->
                val wk = PortugueseComicTranslator.canonicalWorkKey(c.title)
                wk != targetWorkKey && !isSameSequence(target, c) && topCandidates.none { tc ->
                    PortugueseComicTranslator.canonicalWorkKey(tc.title) == wk
                }
            }.take(6 - topCandidates.size)
            topCandidates = topCandidates + fallbackCatalog
        }

        return topCandidates.map { cand ->
            val candWorkKey = PortugueseComicTranslator.canonicalWorkKey(cand.title)
            val savedMatch = (if (cand.id.isNotBlank()) userById[cand.id] else null)
                ?: (if (cand.externalId.isNotBlank()) userByExtId[cand.externalId] else null)
                ?: userByWorkKey[candWorkKey]

            val effectiveComic = if (savedMatch != null) {
                PortugueseComicTranslator.localizeComicMetadataOnly(savedMatch)
            } else {
                cand
            }

            SequenceIssueItem(
                comic = effectiveComic,
                issueNumber = 0,
                isSavedInLibrary = savedMatch != null,
                savedEntry = savedMatch
            )
        }
    }

    private val sequenceCache = java.util.concurrent.ConcurrentHashMap<String, SeriesSequenceGroup>()

    fun buildCacheKey(
        target: ComicEntry,
        userComicsCount: Int,
        extraOnlineCount: Int
    ): String {
        val idPart = target.externalId.ifBlank { target.id }.trim().lowercase()
        val titlePart = target.title.trim().lowercase()
        return "$idPart|$titlePart|${target.seriesName.trim().lowercase()}|${target.issueNumber}|$userComicsCount|$extraOnlineCount"
    }

    fun getCachedSequenceForComic(
        target: ComicEntry,
        userComics: List<ComicEntry>,
        extraOnlineComics: List<ComicEntry> = emptyList()
    ): SeriesSequenceGroup? {
        return sequenceCache[buildCacheKey(target, userComics.size, extraOnlineComics.size)]
    }

    /**
     * Executa o cálculo pesado de sequência cronológica explicitamente fora da Main Thread (Dispatchers.Default).
     */
    suspend fun findSequenceForComicAsync(
        target: ComicEntry,
        userComics: List<ComicEntry>,
        catalogComics: List<ComicEntry> = CuratedCatalog.curatedComicsCatalog,
        extraOnlineComics: List<ComicEntry> = emptyList()
    ): SeriesSequenceGroup = withContext(Dispatchers.Default) {
        val key = buildCacheKey(target, userComics.size, extraOnlineComics.size)
        sequenceCache[key]?.let { return@withContext it }
        findSequenceForComic(
            target = target,
            userComics = userComics,
            catalogComics = catalogComics,
            extraOnlineComics = extraOnlineComics
        ).also { computed ->
            if (sequenceCache.size > 512) sequenceCache.clear()
            sequenceCache[key] = computed
        }
    }

    fun createNextIssueTemplate(
        fromComic: ComicEntry,
        nextIssueNumber: Int
    ): ComicEntry {
        val baseSeries = inferBaseSeriesTitle(fromComic)
        val currentTitle = PortugueseComicTranslator.translateTitleToPtBr(fromComic.title).trim()

        val nextTitle = when {
            hashIssueRegex.containsMatchIn(currentTitle) -> {
                "$baseSeries #$nextIssueNumber"
            }
            currentTitle.contains("Vol.", ignoreCase = true) -> {
                "$baseSeries — Vol. $nextIssueNumber"
            }
            currentTitle.contains("Volume", ignoreCase = true) -> {
                "$baseSeries — Volume $nextIssueNumber"
            }
            currentTitle.contains("Livro", ignoreCase = true) -> {
                "$baseSeries — Livro $nextIssueNumber"
            }
            else -> {
                "$baseSeries #$nextIssueNumber"
            }
        }

        val generatedId = "seq-${normalizeKey(baseSeries).replace(" ", "-")}-${normalizeKey(extractPrimaryAuthor(fromComic.authors)).replace(" ", "-")}-$nextIssueNumber"
        val cleanPub = PortugueseComicTranslator.normalizePublisher(
            fromComic.publisher,
            nextTitle,
            fromComic.authors,
            fromComic.genre
        )

        return ComicEntry(
            id = generatedId,
            externalId = generatedId,
            title = nextTitle,
            authors = fromComic.authors,
            publisher = cleanPub,
            genre = fromComic.genre,
            format = fromComic.format,
            releaseYear = fromComic.releaseYear,
            pageCount = fromComic.pageCount,
            currentPage = 0,
            status = ReadingStatus.WANT_TO_READ,
            rating = 0f,
            favorite = false,
            rereadCount = 0,
            tags = emptyList(),
            review = "",
            readDate = "",
            coverUrl = fromComic.coverUrl,
            synopsis = "Edição #$nextIssueNumber da sequência $baseSeries escrita e ilustrada por ${fromComic.authors} ($cleanPub).",
            sourceLabel = "Sequência da Série",
            seriesName = baseSeries,
            issueNumber = nextIssueNumber
        )
    }

    fun groupLibraryBySequences(
        userComics: List<ComicEntry>,
        catalogComics: List<ComicEntry> = CuratedCatalog.curatedComicsCatalog
    ): List<SeriesSequenceGroup> {
        if (userComics.isEmpty()) return emptyList()
        val groupsByKey = LinkedHashMap<String, MutableList<ComicEntry>>()
        // Bucket by normalized baseSeriesTitle first so we only compare comics within the same series title bucket (O(N) vs O(N^2))
        val keysBySeriesBucket = HashMap<String, MutableList<String>>()
        for (comic in userComics) {
            val seriesBucket = normalizeKey(inferBaseSeriesTitle(comic))
            val bucketKeys = keysBySeriesBucket[seriesBucket]
            val existingKey = bucketKeys?.firstOrNull { key ->
                val sample = groupsByKey[key]?.firstOrNull()
                sample != null && isSameSequence(sample, comic)
            }
            val key = existingKey ?: buildSequenceKey(comic)
            val groupList = groupsByKey.getOrPut(key) { mutableListOf() }
            if (groupList.isEmpty()) {
                keysBySeriesBucket.getOrPut(seriesBucket) { mutableListOf() }.add(key)
            }
            groupList.add(comic)
        }

        return groupsByKey.values.map { list ->
            val representative = list.first()
            findSequenceForComic(
                target = representative,
                userComics = list,
                catalogComics = catalogComics
            )
        }.sortedWith(
            compareByDescending<SeriesSequenceGroup> { it.issues.size > 1 }
                .thenByDescending { it.savedCount }
                .thenBy { it.displayTitle }
        )
    }

    /**
     * Agrupa o acervo do usuário por sequências cronológicas explicitamente em Dispatchers.Default.
     */
    suspend fun groupLibraryBySequencesAsync(
        userComics: List<ComicEntry>,
        catalogComics: List<ComicEntry> = CuratedCatalog.curatedComicsCatalog
    ): List<SeriesSequenceGroup> = withContext(Dispatchers.Default) {
        groupLibraryBySequences(
            userComics = userComics,
            catalogComics = catalogComics
        )
    }
}

