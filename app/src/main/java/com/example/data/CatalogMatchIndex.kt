package com.example.data

import com.example.model.ComicEntry
import java.util.TreeMap

/** Indexed equivalent of the existing first matching catalog-entry search. */
internal class CatalogMatchIndex(entries: List<ComicEntry>) {
    private val byExternalId = HashMap<String, IndexedValue<ComicEntry>>()
    private val byId = HashMap<String, IndexedValue<ComicEntry>>()
    private val byIsbn = HashMap<String, IndexedValue<ComicEntry>>()
    private val byTitle = TreeMap<String, IndexedValue<ComicEntry>>(String.CASE_INSENSITIVE_ORDER)

    init {
        entries.forEachIndexed { index, comic ->
            val entry = IndexedValue(index, comic)
            byExternalId.putIfAbsent(comic.externalId, entry)
            byId.putIfAbsent(comic.id, entry)
            byIsbn.putIfAbsent(comic.isbn, entry)
            byTitle.putIfAbsent(comic.title, entry)
        }
    }

    fun findMatch(comic: ComicEntry): ComicEntry? {
        var winner: IndexedValue<ComicEntry>? = null
        fun consider(candidate: IndexedValue<ComicEntry>?) {
            if (candidate != null && candidate.index < (winner?.index ?: Int.MAX_VALUE)) {
                winner = candidate
            }
        }
        consider(byExternalId[comic.externalId])
        consider(byId[comic.id])
        consider(byId[comic.id.replace("init-", "cat-")])
        if (comic.isbn.isNotBlank()) consider(byIsbn[comic.isbn])
        consider(byTitle[comic.title])
        return winner?.value
    }

    fun findByFields(
        idVal: String,
        extIdVal: String,
        isbnVal: String,
        titleVal: String,
        rawCover: String
    ): ComicEntry? {
        var winner: IndexedValue<ComicEntry>? = null
        fun consider(candidate: IndexedValue<ComicEntry>?) {
            if (candidate != null && candidate.index < (winner?.index ?: Int.MAX_VALUE)) {
                winner = candidate
            }
        }
        consider(byId[idVal])
        consider(byExternalId[extIdVal])
        consider(byId[extIdVal])
        consider(byId[idVal.replace("init-", "cat-")])
        if (isbnVal.isNotBlank()) consider(byIsbn[isbnVal])
        if (rawCover.isBlank() || rawCover.contains("covers.openlibrary.org")) {
            consider(byTitle[titleVal])
        }
        return winner?.value
    }
}
