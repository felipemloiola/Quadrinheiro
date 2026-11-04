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
    private fun identity(c: ComicEntry): String {
        canonicalIsbn(c.isbn)?.let { return "isbn:$it" }
        Regex("ab-(BR|US)-([0-9]+)").matchEntire(c.externalId.ifBlank { c.id })?.let { return "apple:${it.groupValues[2]}" }
        if (c.format == "Série de mangá" && c.releaseYear > 0) return "manga-series:${normalize(c.displayTitle)}|${c.releaseYear}"
        if (c.publisher.isBlank() || c.authors.isBlank() || c.releaseYear <= 0 || c.format.isBlank()) return "id:${c.externalId.ifBlank { c.id }}"
        return "edition:${normalize(c.displayTitle)}|${normalize(c.authors)}|${normalize(c.publisher)}|${c.releaseYear}|${normalize(c.format)}|${c.issueNumber}"
    }
    private fun quality(c: ComicEntry) = (if (canonicalIsbn(c.isbn) != null) 4 else 0) + (if (c.pageCount > 0) 3 else 0) +
        (if (c.coverUrl.isNotBlank()) 2 else 0) + (if (c.externalId.startsWith("ab-BR-")) 2 else 0) + (if (c.authors.isNotBlank()) 1 else 0)
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
    fun ranked(items: List<ComicEntry>, queries: List<String>): List<ComicEntry> {
        val terms = queries.map(::normalize).filter(String::isNotBlank)
        val words = terms.flatMap { it.split(' ') }.filter { it.length >= 3 }.distinct()
        return unique(items).sortedByDescending { c ->
            val title = normalize(c.displayTitle)
            val relevance = when {
                terms.any { title == it } -> 160
                terms.any { title.contains(it) } -> 120
                else -> words.count { " $title ".contains(" $it ") } * 10
            }
            relevance + quality(c) * 2
        }
    }
}
