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
    val missingIssueNumbers: List<Int> = emptyList()
) {
    val savedCount: Int get() = issues.count { it.isSavedInLibrary }
    val readCount: Int get() = issues.count { it.savedEntry?.status == ReadingStatus.READ }
    val totalCount: Int get() = issues.size
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

    fun stripAccents(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        return normalized.replace(diacriticsRegex, "")
    }

    fun extractPrimaryAuthor(authors: String): String {
        val first = authors.split(authorDelimiterRegex)
            .map { it.trim() }
            .firstOrNull { candidate ->
                candidate.isNotBlank() && !ignoredAuthorTokens.contains(candidate.lowercase())
            }
        return first ?: authors.trim()
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
        return stripAccents(text.lowercase())
            .replace(nonAlphaNumSpaceRegex, " ")
            .replace(whitespaceRegex, " ")
            .trim()
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
        val primaryAuthor = extractPrimaryAuthor(localizedTarget.authors)
        val seqKey = buildSequenceKey(localizedTarget)
        val targetIssue = inferIssueNumber(localizedTarget)

        // Gather all matching candidates
        val matchingUser = userComics.filter { isSameSequence(localizedTarget, it) }
        val matchingCatalog = catalogComics.filter { isSameSequence(localizedTarget, it) }
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
                val isUserSaved = userComics.any { it.id == rawCandidate.id }
                if (!isUserSaved) continue
            }

            val savedMatch = userComics.firstOrNull { u ->
                (rawCandidate.id.isNotBlank() && u.id == rawCandidate.id) ||
                    (rawCandidate.externalId.isNotBlank() && u.externalId == rawCandidate.externalId) ||
                    (issueNum > 0 && isSameSequence(u, candidate) && inferIssueNumber(u) == issueNum) ||
                    PortugueseComicTranslator.canonicalWorkKey(u.title) == workKey
            }

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

        return SeriesSequenceGroup(
            sequenceKey = seqKey,
            baseSeriesTitle = baseTitle,
            primaryAuthor = primaryAuthor,
            displayTitle = displayTitle,
            issues = sortedIssues,
            nextIssueNumber = nextIssueNumber,
            missingIssueNumbers = missingIssues
        )
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
        val groupsByKey = LinkedHashMap<String, MutableList<ComicEntry>>()
        for (comic in userComics) {
            val existingKey = groupsByKey.keys.firstOrNull { key ->
                val sample = groupsByKey[key]?.firstOrNull()
                sample != null && isSameSequence(sample, comic)
            }
            val key = existingKey ?: buildSequenceKey(comic)
            groupsByKey.getOrPut(key) { mutableListOf() }.add(comic)
        }

        return groupsByKey.values.map { list ->
            val representative = list.first()
            findSequenceForComic(
                target = representative,
                userComics = userComics,
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
