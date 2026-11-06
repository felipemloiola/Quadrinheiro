package com.example.data

import com.example.model.ComicEntry
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

internal object CatalogSearchPolicy {
    fun normalize(raw: String): String = Normalizer.normalize(raw.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("""\p{M}"""), "").replace(Regex("[^a-z0-9]+"), " ").trim()
    fun canonicalIsbn(raw: String): String? {
        val value = raw.uppercase(Locale.ROOT).filter { it.isDigit() || it == 'X' }
        if (value.length == 13 && value.all(Char::isDigit)) return value.takeIf { it.mapIndexed { index, c -> (c - '0') * if (index % 2 == 0) 1 else 3 }.sum() % 10 == 0 }
        if (value.length != 10 || !value.take(9).all(Char::isDigit) || !(value.last().isDigit() || value.last() == 'X')) return null
        if (value.mapIndexed { index, c -> (if (c == 'X') 10 else c - '0') * (10 - index) }.sum() % 11 != 0) return null
        val prefix = "978" + value.take(9)
        val check = (10 - prefix.mapIndexed { index, c -> (c - '0') * if (index % 2 == 0) 1 else 3 }.sum() % 10) % 10
        return prefix + check
    }
    private fun distance(a: String, b: String, limit: Int): Int {
        if (abs(a.length - b.length) > limit) return limit + 1
        var previous = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            val next = IntArray(b.length + 1); next[0] = i + 1
            for (j in b.indices) next[j + 1] = minOf(next[j] + 1, previous[j + 1] + 1, previous[j] + if (a[i] == b[j]) 0 else 1)
            if (next.minOrNull()!! > limit) return limit + 1
            previous = next
        }
        return previous[b.length]
    }
    private val bilingualAliases: Map<String, List<String>> = mapOf(
        "homem aranha" to listOf("Homem-Aranha", "Spider-Man", "O Espetacular Homem-Aranha", "The Amazing Spider-Man"),
        "spider man" to listOf("Spider-Man", "Homem-Aranha", "The Amazing Spider-Man", "O Espetacular Homem-Aranha"),
        "spiderman" to listOf("Spider-Man", "Homem-Aranha", "The Amazing Spider-Man", "O Espetacular Homem-Aranha"),
        "o espetacular homem aranha" to listOf("O Espetacular Homem-Aranha", "The Amazing Spider-Man", "Homem-Aranha", "Spider-Man"),
        "amazing spider man" to listOf("The Amazing Spider-Man", "O Espetacular Homem-Aranha", "Spider-Man", "Homem-Aranha"),
        "batman ano um" to listOf("Batman: Ano Um", "Batman: Year One"),
        "batman year one" to listOf("Batman: Year One", "Batman: Ano Um"),
        "ano um" to listOf("Batman: Ano Um", "Batman: Year One"),
        "year one" to listOf("Batman: Year One", "Batman: Ano Um"),
        "piada mortal" to listOf("Batman: A Piada Mortal", "Batman: The Killing Joke"),
        "batman a piada mortal" to listOf("Batman: A Piada Mortal", "Batman: The Killing Joke"),
        "killing joke" to listOf("Batman: The Killing Joke", "Batman: A Piada Mortal"),
        "cavaleiro das trevas" to listOf("Batman: O Cavaleiro das Trevas", "Batman: The Dark Knight Returns"),
        "batman o cavaleiro das trevas" to listOf("Batman: O Cavaleiro das Trevas", "Batman: The Dark Knight Returns"),
        "dark knight" to listOf("Batman: The Dark Knight Returns", "Batman: O Cavaleiro das Trevas"),
        "dark knight returns" to listOf("Batman: The Dark Knight Returns", "Batman: O Cavaleiro das Trevas"),
        "longo dia das bruxas" to listOf("Batman: O Longo Dia das Bruxas", "Batman: The Long Halloween"),
        "long halloween" to listOf("Batman: The Long Halloween", "Batman: O Longo Dia das Bruxas"),
        "vitoria sombria" to listOf("Batman: Vitória Sombria", "Batman: Dark Victory"),
        "dark victory" to listOf("Batman: Dark Victory", "Batman: Vitória Sombria"),
        "asilo arkham" to listOf("Asilo Arkham", "Arkham Asylum"),
        "arkham asylum" to listOf("Arkham Asylum", "Asilo Arkham"),
        "monstro do pantano" to listOf("Monstro do Pântano", "Swamp Thing", "A Saga do Monstro do Pântano"),
        "swamp thing" to listOf("Swamp Thing", "Monstro do Pântano", "A Saga do Monstro do Pântano"),
        "mulher maravilha" to listOf("Mulher-Maravilha", "Wonder Woman"),
        "wonder woman" to listOf("Wonder Woman", "Mulher-Maravilha"),
        "lanterna verde" to listOf("Lanterna Verde", "Green Lantern"),
        "green lantern" to listOf("Green Lantern", "Lanterna Verde"),
        "demolidor" to listOf("Demolidor", "Daredevil", "Demolidor: A Queda de Murdock"),
        "daredevil" to listOf("Daredevil", "Demolidor", "Daredevil: Born Again"),
        "queda de murdock" to listOf("Demolidor: A Queda de Murdock", "Daredevil: Born Again"),
        "born again" to listOf("Daredevil: Born Again", "Demolidor: A Queda de Murdock"),
        "v de vinganca" to listOf("V de Vingança", "V for Vendetta"),
        "v for vendetta" to listOf("V for Vendetta", "V de Vingança"),
        "x men" to listOf("X-Men", "Surpreendentes X-Men", "Novos X-Men", "Astonishing X-Men"),
        "homem de ferro" to listOf("Homem de Ferro", "Iron Man"),
        "iron man" to listOf("Iron Man", "Homem de Ferro"),
        "capitao america" to listOf("Capitão América", "Captain America"),
        "captain america" to listOf("Captain America", "Capitão América"),
        "vingadores" to listOf("Vingadores", "Os Vingadores", "Avengers"),
        "avengers" to listOf("Avengers", "Os Vingadores", "Vingadores"),
        "liga da justica" to listOf("Liga da Justiça", "Justice League"),
        "justice league" to listOf("Justice League", "Liga da Justiça"),
        "reino do amanha" to listOf("Reino do Amanhã", "Kingdom Come"),
        "kingdom come" to listOf("Kingdom Come", "Reino do Amanhã"),
        "quarteto fantastico" to listOf("Quarteto Fantástico", "Fantastic Four"),
        "fantastic four" to listOf("Fantastic Four", "Quarteto Fantástico"),
        "gaviao arqueiro" to listOf("Gavião Arqueiro", "Hawkeye"),
        "hawkeye" to listOf("Hawkeye", "Gavião Arqueiro"),
        "velho logan" to listOf("Velho Logan", "Old Man Logan"),
        "old man logan" to listOf("Old Man Logan", "Velho Logan"),
        "guerra civil" to listOf("Guerra Civil", "Civil War"),
        "civil war" to listOf("Civil War", "Guerra Civil"),
        "invencivel" to listOf("Invencível", "Invincible"),
        "invincible" to listOf("Invincible", "Invencível"),
        "ultimo homem" to listOf("Y: O Último Homem", "Y: The Last Man"),
        "last man" to listOf("Y: The Last Man", "Y: O Último Homem")
    )

    /**
     * Índice verificado e restrito de obras e fases canônicas e aclamadas associadas a conceitos/personagens frequentes.
     * Serve exclusivamente para priorizar a ordem de ranqueamento, sem inventar dados nem suprimir os demais resultados reais.
     */
    internal val verifiedCanonicalWorks: Map<String, List<String>> = mapOf(
        "monstro do pantano" to listOf("a saga do monstro do pantano", "saga of the swamp thing", "monstro do pantano"),
        "swamp thing" to listOf("a saga do monstro do pantano", "saga of the swamp thing", "swamp thing"),
        "watchmen" to listOf("watchmen"),
        "sandman" to listOf("sandman preludios e noturnos", "sandman preludes and nocturnes", "sandman estacao das brumas", "sandman"),
        "demolidor" to listOf("demolidor a queda de murdock", "daredevil born again", "demolidor o homem sem medo", "daredevil the man without fear"),
        "daredevil" to listOf("demolidor a queda de murdock", "daredevil born again", "demolidor o homem sem medo", "daredevil the man without fear"),
        "batman" to listOf("batman ano um", "batman year one", "batman o cavaleiro das trevas", "batman the dark knight returns", "batman a piada mortal", "batman the killing joke", "batman o longo dia das bruxas", "batman the long halloween"),
        "ano um" to listOf("batman ano um", "batman year one"),
        "year one" to listOf("batman ano um", "batman year one"),
        "cavaleiro das trevas" to listOf("batman o cavaleiro das trevas", "batman the dark knight returns"),
        "piada mortal" to listOf("batman a piada mortal", "batman the killing joke"),
        "longo dia das bruxas" to listOf("batman o longo dia das bruxas", "batman the long halloween"),
        "v de vinganca" to listOf("v de vinganca", "v for vendetta"),
        "homem aranha" to listOf("a ultima cacada de kraven", "kravens last hunt", "homem aranha historia de vida", "spider man life story", "homem aranha azul"),
        "spider man" to listOf("a ultima cacada de kraven", "kravens last hunt", "spider man life story", "spider man blue"),
        "x men" to listOf("a saga da fenix negra", "dark phoenix saga", "dias de um futuro esquecido", "days of future past", "deus ama o homem mata", "surpreendentes x men", "astonishing x men"),
        "reino do amanha" to listOf("reino do amanha", "kingdom come"),
        "all star superman" to listOf("grandes astros superman", "all star superman"),
        "grandes astros superman" to listOf("grandes astros superman", "all star superman"),
        "crise nas infinitas terras" to listOf("crise nas infinitas terras", "crisis on infinite earths"),
        "ponto de ignicao" to listOf("ponto de ignicao", "flashpoint"),
        "flashpoint" to listOf("ponto de ignicao", "flashpoint")
    )

    fun variants(query: String, knownTitles: List<String> = emptyList()): List<String> {
        val literal = query.trim().take(100); val q = normalize(literal)
        if (q.length < 4) return listOf(literal)
        val titles = (listOf("Batman", "Batman: Ano Um", "Batman: A Piada Mortal", "Watchmen", "Maus", "Sandman", "V de Vingança", "Monstro do Pântano", "Homem-Aranha", "Demolidor") + knownTitles.take(160)).filter { it.length <= 100 }.distinctBy(::normalize)
        if (titles.any { normalize(it) == q }) return listOf(literal)
        val prefix = titles.filter { normalize(it).startsWith(q) }.sortedBy { normalize(it).length }
        if (prefix.isNotEmpty()) {
            val shortest = normalize(prefix.first()).length
            if (prefix.count { normalize(it).length == shortest } == 1) return listOf(literal, prefix.first())
        }
        val limit = if (q.length >= 6) 2 else 1
        val ranked = titles.map { it to distance(q, normalize(it), limit) }.filter { it.second <= limit }.sortedBy { it.second }
        val corrected = ranked.firstOrNull()?.takeIf { best -> ranked.count { it.second == best.second } == 1 }?.first
        return listOfNotNull(literal, corrected).distinctBy(::normalize)
    }

    fun searchTerms(query: String, knownTitles: List<String> = emptyList()): List<String> {
        val base = variants(query, knownTitles)
        val q = normalize(query)
        if (q.length < 2) return base
        val aliases = mutableListOf<String>()
        bilingualAliases[q]?.let { aliases.addAll(it) }
        if (aliases.isEmpty() && q.length >= 4) {
            bilingualAliases.entries.forEach { (key, values) ->
                if (key.contains(q) || q.contains(key) || distance(q, key, if (q.length >= 6) 2 else 1) <= (if (q.length >= 6) 2 else 1)) {
                    aliases.addAll(values)
                }
            }
        }
        return (base + aliases).filter { it.isNotBlank() }.distinctBy { it.lowercase(Locale.ROOT).trim() }.take(6)
    }

    fun isPortugueseEdition(c: ComicEntry): Boolean {
        val id = c.externalId.ifBlank { c.id }
        if (id.startsWith("ab-BR-") || id.startsWith("gb-") || id.startsWith("cat-") || id.startsWith("init-")) return true
        val pub = c.publisher.lowercase(Locale.ROOT)
        if (listOf("panini", "jbc", "pipoca", "nanquim", "companhia das letras", "quadrinhos na cia", "devir", "mythos", "newpop", "conrad", "abril", "globo", "darkside", "veneta", "nemo", "minotauro").any { pub.contains(it) }) return true
        if (c.titlePtBr.isNotBlank() && c.title == c.titlePtBr && PortugueseComicTranslator.isPortugueseText(c.title)) return true
        return c.synopsis.isNotBlank() && PortugueseComicTranslator.isPortugueseText(c.synopsis)
    }

    private fun identity(c: ComicEntry): String {
        canonicalIsbn(c.isbn)?.let { return "isbn:$it" }
        Regex("ab-(BR|US)-([0-9]+)").matchEntire(c.externalId.ifBlank { c.id })?.let { return "apple:${it.groupValues[2]}" }
        if (c.format == "Série de mangá" && c.releaseYear > 0) return "manga-series:${normalize(c.displayTitle)}|${c.releaseYear}"
        if (c.publisher.isBlank() || c.authors.isBlank() || c.releaseYear <= 0 || c.format.isBlank()) return "id:${c.externalId.ifBlank { c.id }}"
        return "edition:${normalize(c.displayTitle)}|${normalize(c.authors)}|${normalize(c.publisher)}|${c.releaseYear}|${normalize(c.format)}|${c.issueNumber}"
    }
    private fun quality(c: ComicEntry) =
        (if (canonicalIsbn(c.isbn) != null) 4 else 0) +
        (if (c.pageCount > 0) 6 else 0) +
        (if (c.coverUrl.isNotBlank()) 3 else 0) +
        (if (c.externalId.startsWith("ab-BR-")) 2 else 0) +
        (if (isPortugueseEdition(c)) 4 else 0) +
        (if (c.authors.isNotBlank()) 2 else 0) +
        (if (c.publisher.isNotBlank()) 1 else 0) +
        (if (c.synopsis.isNotBlank() || c.synopsisPtBr.isNotBlank()) 2 else 0)

    fun unique(items: List<ComicEntry>): List<ComicEntry> {
        val result = LinkedHashMap<String, ComicEntry>()
        for (candidate in items) {
            if (candidate.id.isBlank()) continue
            var key = identity(candidate)
            val first = result[key]
            if (key.startsWith("manga-series:") && first != null && first.authors.isNotBlank() && candidate.authors.isNotBlank() && normalize(first.authors) != normalize(candidate.authors)) key += "|authors:${normalize(candidate.authors)}"
            val existing = result[key]
            if (existing == null || quality(candidate) > quality(existing)) result[key] = candidate
        }
        return result.values.toList()
    }

    private fun isCollectedFormat(c: ComicEntry): Boolean {
        val titleNorm = normalize("${c.title} ${c.displayTitle}")
        val formatNorm = normalize(c.format)
        val hasMarker = Regex("""\b(vol 1|volume 1|livro 1|book 1|tpb|encadernado|graphic novel|coletanea|omnibus|edicao definitiva|deluxe)\b""").containsMatchIn("$titleNorm $formatNorm")
        return hasMarker || (c.format in listOf("Coleção", "Graphic Novel", "Encadernado (TPB)", "Capa dura")) || c.pageCount >= 90
    }

    private fun primaryAuthor(raw: String): String {
        val first = raw.split(',', ';', '&', '/').firstOrNull()?.trim() ?: ""
        return normalize(first)
    }

    internal fun shouldMergeEntries(a: ComicEntry, b: ComicEntry): Boolean {
        // 1. Immediate merge if both have valid, identical normalized ISBNs (or equivalent ISBN-10/13)
        val isbnA = canonicalIsbn(a.isbn)
        val isbnB = canonicalIsbn(b.isbn)
        if (isbnA != null && isbnB != null) {
            if (isbnA == isbnB) return true
            return false // Valid but different ISBNs -> Never merge
        }

        // 2. Never merge if page counts differ by more than 60 pages (e.g. 800-page omnibus vs 140-page trade)
        if (a.pageCount > 0 && b.pageCount > 0 && kotlin.math.abs(a.pageCount - b.pageCount) > 60) return false

        // 3. Never merge different non-zero issue numbers (e.g. #1 vs #2, #2 vs #3)
        if (a.issueNumber > 0 && b.issueNumber > 0 && a.issueNumber != b.issueNumber) return false

        // 4. Never merge proven distinct languages when both have full localized titles that differ
        if (isPortugueseEdition(a) && !isPortugueseEdition(b) && b.sourceLabel.contains("US", true) && a.title.isNotBlank() && b.title.isNotBlank() && !a.title.equals(b.title, true) && a.releaseYear > 0 && b.releaseYear > 0 && kotlin.math.abs(a.releaseYear - b.releaseYear) > 10) return false

        // 5. Bilingual canonical work key comparison
        val rawTitleA = a.titlePtBr.ifBlank { PortugueseComicTranslator.translateTitleToPtBr(a.displayTitle.ifBlank { a.title }).ifBlank { a.displayTitle.ifBlank { a.title } } }
        val rawTitleB = b.titlePtBr.ifBlank { PortugueseComicTranslator.translateTitleToPtBr(b.displayTitle.ifBlank { b.title }).ifBlank { b.displayTitle.ifBlank { b.title } } }
        val baseKeyA = PortugueseComicTranslator.canonicalWorkKey(rawTitleA).replace(Regex("""\b(vol 1|volume 1|livro 1)\b"""), "").trim()
        val baseKeyB = PortugueseComicTranslator.canonicalWorkKey(rawTitleB).replace(Regex("""\b(vol 1|volume 1|livro 1)\b"""), "").trim()

        val altBaseKeyA = PortugueseComicTranslator.canonicalWorkKey(a.title).replace(Regex("""\b(vol 1|volume 1|livro 1)\b"""), "").trim()
        val altBaseKeyB = PortugueseComicTranslator.canonicalWorkKey(b.title).replace(Regex("""\b(vol 1|volume 1|livro 1)\b"""), "").trim()

        val keysMatch = (baseKeyA.isNotBlank() && (baseKeyA == baseKeyB || baseKeyA == altBaseKeyB)) ||
            (altBaseKeyA.isNotBlank() && (altBaseKeyA == baseKeyB || altBaseKeyA == altBaseKeyB))
        if (!keysMatch) return false

        // Check primary author compatibility when both entries provide authors
        val authA = primaryAuthor(a.authors)
        val authB = primaryAuthor(b.authors)
        if (authA.isNotBlank() && authB.isNotBlank() && authA != "equipe criativa nao informada" && authB != "equipe criativa nao informada") {
            val authorCompatible = authA == authB || authA.contains(authB) || authB.contains(authA)
            if (!authorCompatible) return false
        }

        // 6. Same base key. Check if one is unnumbered (0) and other is volume 1 (1)
        val oneIsZeroOtherIsOne = (a.issueNumber == 0 && b.issueNumber == 1) || (a.issueNumber == 1 && b.issueNumber == 0)
        if (oneIsZeroOtherIsOne) {
            // Only merge if at least one is clearly a collection/volume and not a single monthly magazine issue of ~18-44 pages
            val aIsCollected = isCollectedFormat(a)
            val bIsCollected = isCollectedFormat(b)
            val aIsMonthlySingle = a.issueNumber == 1 && a.pageCount in 18..44 && !aIsCollected
            val bIsMonthlySingle = b.issueNumber == 1 && b.pageCount in 18..44 && !bIsCollected
            if (aIsMonthlySingle || bIsMonthlySingle) return false
            return aIsCollected || bIsCollected
        }

        // If issue numbers are identical
        return a.issueNumber == b.issueNumber
    }

    private fun workDeduplicationKey(c: ComicEntry): String {
        val rawTitle = c.titlePtBr.ifBlank {
            PortugueseComicTranslator.translateTitleToPtBr(c.displayTitle.ifBlank { c.title })
                .ifBlank { c.displayTitle.ifBlank { c.title } }
        }
        val canonicalWork = PortugueseComicTranslator.canonicalWorkKey(rawTitle)
            .ifBlank { normalize(c.displayTitle.ifBlank { c.title }) }
        val issuePart = if (c.issueNumber > 0) "#${c.issueNumber}" else ""
        return "$canonicalWork$issuePart"
    }

    internal fun mergeComplementaryEditionData(primary: ComicEntry, secondary: ComicEntry): ComicEntry {
        // Safe authors merge: Never replace a real author with empty or generic placeholder
        val selectedAuthors = when {
            primary.authors.isNotBlank() && !primary.authors.contains("Equipe criativa não informada", ignoreCase = true) -> {
                if (secondary.authors.isNotBlank() && !secondary.authors.contains("Equipe criativa não informada", ignoreCase = true) && secondary.authors.length > primary.authors.length) {
                    secondary.authors
                } else primary.authors
            }
            secondary.authors.isNotBlank() && !secondary.authors.contains("Equipe criativa não informada", ignoreCase = true) -> secondary.authors
            else -> primary.authors.ifBlank { secondary.authors }
        }

        // Safe publisher merge: prefer specific comic publisher over generic store label
        val selectedPublisher = when {
            primary.publisher.isNotBlank() && !primary.publisher.equals("Editora Independente", ignoreCase = true) -> primary.publisher
            secondary.publisher.isNotBlank() && !secondary.publisher.equals("Editora Independente", ignoreCase = true) -> secondary.publisher
            else -> primary.publisher.ifBlank { secondary.publisher }
        }

        // Safe synopsis merge: prefer clean synopsis with content over empty or placeholder
        val selectedSynopsis = if (primary.synopsis.length >= 40) primary.synopsis else secondary.synopsis.ifBlank { primary.synopsis }
        val selectedSynopsisPtBr = if (primary.synopsisPtBr.length >= 40) primary.synopsisPtBr else secondary.synopsisPtBr.ifBlank { primary.synopsisPtBr }

        // Safe coverUrl: prefer valid image URL that does not contain placeholder
        val selectedCoverUrl = when {
            primary.coverUrl.isNotBlank() && !primary.coverUrl.contains("placeholder", true) -> primary.coverUrl
            secondary.coverUrl.isNotBlank() && !secondary.coverUrl.contains("placeholder", true) -> secondary.coverUrl
            else -> primary.coverUrl.ifBlank { secondary.coverUrl }
        }

        // Safe ISBN: prefer normalized canonical ISBN
        val selectedIsbn = canonicalIsbn(primary.isbn)?.let { primary.isbn } ?: canonicalIsbn(secondary.isbn)?.let { secondary.isbn } ?: primary.isbn.ifBlank { secondary.isbn }

        return primary.copy(
            titlePtBr = primary.titlePtBr.ifBlank {
                secondary.titlePtBr.ifBlank {
                    if (isPortugueseEdition(secondary) && PortugueseComicTranslator.isPortugueseText(secondary.title)) secondary.title else ""
                }
            },
            synopsisPtBr = selectedSynopsisPtBr,
            synopsis = selectedSynopsis,
            authors = selectedAuthors,
            publisher = selectedPublisher,
            pageCount = if (primary.pageCount > 0) primary.pageCount else secondary.pageCount,
            releaseYear = if (primary.releaseYear > 0) primary.releaseYear else secondary.releaseYear,
            isbn = selectedIsbn,
            coverUrl = selectedCoverUrl
        )
    }

    internal fun deduplicateByCanonicalWork(items: List<ComicEntry>, language: String = "ALL"): List<ComicEntry> {
        val uniqueItems = unique(items)
        val result = mutableListOf<ComicEntry>()

        for (item in uniqueItems) {
            val matchIndex = result.indexOfFirst { existing -> shouldMergeEntries(existing, item) }
            if (matchIndex < 0) {
                result.add(item)
            } else {
                val existing = result[matchIndex]
                val existingScore = quality(existing) + (if (isPortugueseEdition(existing)) (if (language == "PT") 8 else 4) else 0)
                val candidateScore = quality(item) + (if (isPortugueseEdition(item)) (if (language == "PT") 8 else 4) else 0)
                val merged = if (candidateScore > existingScore) {
                    mergeComplementaryEditionData(item, existing)
                } else {
                    mergeComplementaryEditionData(existing, item)
                }
                result[matchIndex] = merged
            }
        }
        return result
    }

    fun ranked(items: List<ComicEntry>, queries: List<String>, language: String = "ALL"): List<ComicEntry> {
        val expanded = queries.flatMap { searchTerms(it) }.map(::normalize).filter(String::isNotBlank).distinct()
        val primaryTerms = queries.map(::normalize).filter(String::isNotBlank)
        val words = expanded.flatMap { it.split(' ') }.filter { it.length >= 3 }.distinct()
        val deduplicated = deduplicateByCanonicalWork(items, language)
        val withPagesCount = deduplicated.count { it.pageCount > 0 }
        val filtered = if (withPagesCount >= 4) {
            deduplicated.filter { it.pageCount > 0 || it.authors.isNotBlank() || !it.isOnlineResult }
        } else {
            deduplicated
        }
        return filtered.sortedByDescending { c ->
            val displayTitle = normalize(c.displayTitle)
            val origTitle = normalize(c.title)
            val transTitle = normalize(PortugueseComicTranslator.translateTitleToPtBr(c.title))
            val titles = listOf(displayTitle, origTitle, transTitle).filter(String::isNotBlank).distinct()
            val relevance = when {
                primaryTerms.any { q -> titles.any { it == q } } -> 180
                expanded.any { q -> titles.any { it == q } } -> 160
                primaryTerms.any { q -> titles.any { it.startsWith(q) } } -> 140
                expanded.any { q -> titles.any { it.contains(q) } } -> 120
                else -> words.count { w -> titles.any { " $it ".contains(" $w ") } } * 12
            }
            val langBonus = if (isPortugueseEdition(c)) (if (language == "PT") 36 else 16) else 0
            val pageBonus = if (c.pageCount > 0) 28 else -18
            val metadataBonus = (if (c.authors.isNotBlank()) 10 else 0) + (if (c.synopsis.isNotBlank() || c.synopsisPtBr.isNotBlank()) 8 else 0)
            val canonicalMatches = primaryTerms.flatMap { verifiedCanonicalWorks[it].orEmpty() }
            val canonicalBonus = if (canonicalMatches.any { can -> titles.any { it.contains(can) } }) 120 else 0
            relevance + canonicalBonus + langBonus + pageBonus + metadataBonus + quality(c) * 2
        }
    }

    /**
     * Filtra e ordena candidatos (locais e online) de forma tolerante a hífens, acentos, caixa,
     * títulos parciais, nomes em português/inglês e pequenos erros de digitação,
     * mantendo correspondências exatas e relevantes no topo.
     */
    fun filterAndRankForQuery(
        candidates: List<ComicEntry>,
        rawQuery: String,
        language: String,
        excludedIds: Set<String> = emptySet(),
        limit: Int = 80
    ): List<ComicEntry> = filterAndRankForQueryInternal(candidates, rawQuery, language, excludedIds, limit)

    fun filterAndRankForQuery(
        candidates: List<ComicEntry>,
        rawQuery: String,
        excludedIds: Set<String> = emptySet(),
        limit: Int = 80
    ): List<ComicEntry> = filterAndRankForQueryInternal(candidates, rawQuery, "PT", excludedIds, limit)

    private fun filterAndRankForQueryInternal(
        candidates: List<ComicEntry>,
        rawQuery: String,
        language: String,
        excludedIds: Set<String> = emptySet(),
        limit: Int = 80
    ): List<ComicEntry> {
        val cleanQuery = rawQuery.trim()
        if (cleanQuery.isEmpty()) {
            return deduplicateByCanonicalWork(candidates.filter { it.id !in excludedIds }, language).take(limit)
        }
        val q = normalize(cleanQuery)
        if (q.isEmpty()) return emptyList()
        val expandedTerms = searchTerms(cleanQuery, candidates.map { it.displayTitle })
            .map(::normalize)
            .filter(String::isNotBlank)
            .distinct()
        val queryTokens = q.split(' ').filter { it.isNotBlank() }

        val deduplicated = deduplicateByCanonicalWork(candidates, language)
        val scored = mutableListOf<Pair<ComicEntry, Int>>()
        for (c in deduplicated) {
            if (c.id in excludedIds) continue
            val displayNorm = normalize(c.displayTitle)
            val origNorm = normalize(c.title)
            val ptNorm = normalize(c.titlePtBr)
            val transNorm = normalize(PortugueseComicTranslator.translateTitleToPtBr(c.title))
            val seriesNorm = normalize(c.seriesName)
            val authorsNorm = normalize(c.authors)
            val titleVariants = listOf(displayNorm, origNorm, ptNorm, transNorm, seriesNorm).filter(String::isNotBlank).distinct()
            val titleWords = titleVariants.flatMap { it.split(' ') }.filter { it.length >= 3 }.distinct()

            val matchScore = when {
                titleVariants.any { it == q } -> 320
                expandedTerms.any { term -> titleVariants.any { it == term } } -> 280
                titleVariants.any { it.startsWith(q) } -> 240
                expandedTerms.any { term -> titleVariants.any { it.startsWith(term) } } -> 220
                titleVariants.any { " $it ".contains(" $q ") } -> 200
                expandedTerms.any { term -> titleVariants.any { " $it ".contains(" $term ") } } -> 185
                titleVariants.any { it.contains(q) } -> 165
                expandedTerms.any { term -> titleVariants.any { it.contains(term) } } -> 150
                queryTokens.size > 1 && queryTokens.all { tok -> titleVariants.any { it.contains(tok) } || authorsNorm.contains(tok) } -> 135
                authorsNorm == q || " $authorsNorm ".contains(" $q ") -> 125
                authorsNorm.contains(q) -> 110
                q.length >= 4 && titleVariants.any { distance(q, it, if (q.length >= 6) 2 else 1) <= (if (q.length >= 6) 2 else 1) } -> 95
                queryTokens.all { tok ->
                    tok.length >= 4 && titleWords.any { w ->
                        w.startsWith(tok) || distance(tok, w, if (tok.length >= 6) 2 else 1) <= (if (tok.length >= 6) 2 else 1)
                    }
                } -> 80
                else -> 0
            }

            if (matchScore > 0) {
                val langBonus = if (isPortugueseEdition(c)) (if (language == "PT") 32 else 18) else 0
                val pageBonus = if (c.pageCount > 0) 26 else -14
                val metaBonus = (if (c.authors.isNotBlank()) 8 else 0) + (if (c.synopsis.isNotBlank() || c.synopsisPtBr.isNotBlank()) 6 else 0)
                val canonicalTargets = (listOf(q) + expandedTerms).flatMap { verifiedCanonicalWorks[it].orEmpty() }.distinct()
                val canonicalBonus = if (canonicalTargets.any { can -> titleVariants.any { it.contains(can) } }) 140 else 0
                val totalScore = matchScore + canonicalBonus + langBonus + pageBonus + metaBonus + quality(c) * 2
                scored.add(c to totalScore)
            }
        }
        val sorted = scored.sortedByDescending { it.second }.map { it.first }
        val withPages = sorted.filter { it.pageCount > 0 || !it.isOnlineResult }
        return (if (withPages.size >= 6) withPages + sorted.filterNot { it in withPages }.take(4) else sorted).take(limit)
    }
}
