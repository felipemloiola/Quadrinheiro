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
    private fun quality(c: ComicEntry) = (if (canonicalIsbn(c.isbn) != null) 4 else 0) + (if (c.pageCount > 0) 3 else 0) +
        (if (c.coverUrl.isNotBlank()) 2 else 0) + (if (c.externalId.startsWith("ab-BR-")) 2 else 0) +
        (if (isPortugueseEdition(c)) 2 else 0) + (if (c.authors.isNotBlank()) 1 else 0)
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
    fun ranked(items: List<ComicEntry>, queries: List<String>, language: String = "ALL"): List<ComicEntry> {
        val expanded = queries.flatMap { searchTerms(it) }.map(::normalize).filter(String::isNotBlank).distinct()
        val primaryTerms = queries.map(::normalize).filter(String::isNotBlank)
        val words = expanded.flatMap { it.split(' ') }.filter { it.length >= 3 }.distinct()
        return unique(items).sortedByDescending { c ->
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
            val langBonus = if (isPortugueseEdition(c)) (if (language == "PT") 30 else 8) else 0
            relevance + langBonus + quality(c) * 2
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
    ): List<ComicEntry> = filterAndRankForQuery(candidates, rawQuery, excludedIds, limit)

    fun filterAndRankForQuery(
        candidates: List<ComicEntry>,
        rawQuery: String,
        excludedIds: Set<String> = emptySet(),
        limit: Int = 80
    ): List<ComicEntry> {
        val cleanQuery = rawQuery.trim()
        if (cleanQuery.isEmpty()) {
            return unique(candidates.filter { it.id !in excludedIds }).take(limit)
        }
        val q = normalize(cleanQuery)
        if (q.isEmpty()) return emptyList()
        val expandedTerms = searchTerms(cleanQuery, candidates.map { it.displayTitle })
            .map(::normalize)
            .filter(String::isNotBlank)
            .distinct()
        val queryTokens = q.split(' ').filter { it.isNotBlank() }

        val scored = mutableListOf<Pair<ComicEntry, Int>>()
        for (c in unique(candidates)) {
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
                val totalScore = matchScore + (if (isPortugueseEdition(c)) 14 else 0) + quality(c) * 2
                scored.add(c to totalScore)
            }
        }
        return scored.sortedByDescending { it.second }.map { it.first }.take(limit)
    }
}
