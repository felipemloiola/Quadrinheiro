package com.example.data

import com.example.model.ComicEntry
import java.util.Locale

/** A translated label or a Brazilian storefront does not prove a bookstore edition's language; specialist comic catalogs allow verified PT/EN metadata while still blocking third-language or bundle noise. */
internal object PortugueseSearchPolicy {
    private val specializedCatalogPrefixes = listOf("mt-", "cv-", "md-", "kt-")
    private val specializedCatalogLabels = setOf("Metron", "Comic Vine", "MangaDex", "Kitsu")
    private val supportedSpecializedLanguages = setOf("pt", "por", "portuguese", "en", "eng", "english")

    fun isSpecializedComicCatalog(comic: ComicEntry): Boolean {
        val ids = listOf(comic.id.trim(), comic.externalId.trim()).filter(String::isNotEmpty)
        return ids.any { id -> specializedCatalogPrefixes.any(id::startsWith) } ||
            comic.sourceLabel in specializedCatalogLabels
    }

    fun accepts(comic: ComicEntry): Boolean {
        val language = comic.editionLanguage.trim().lowercase(Locale.ROOT).replace('_', '-').substringBefore('-')
        if (isSpecializedComicCatalog(comic)) {
            if (language.isNotEmpty() && language !in supportedSpecializedLanguages) return false
            if (!CatalogSearchPolicy.normalize(comic.title).any { it in 'a'..'z' }) return false
            return !PortugueseComicTranslator.isUnwantedForeignOrBundleResult(comic.title, comic.synopsis)
        }
        if (language.isNotEmpty()) return language in setOf("pt", "por")
        val online = comic.isOnlineResult || listOf(comic.id, comic.externalId).any { id ->
            listOf("ab-", "gb-", "ol-").any(id::startsWith)
        }
        if (!online) return true // Curated/local entries retain their original identity.
        return PortugueseBookSources.supportedPortugueseDescription(comic.synopsis) &&
            PortugueseComicTranslator.isPortugueseText(comic.synopsis) &&
            !PortugueseComicTranslator.isUnwantedForeignOrBundleResult(comic.title, comic.synopsis)
    }
}
