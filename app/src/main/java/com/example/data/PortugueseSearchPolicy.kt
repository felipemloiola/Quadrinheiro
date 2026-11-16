package com.example.data

import com.example.model.ComicEntry
import java.util.Locale

/** A translated label or a Brazilian storefront does not prove the edition's language. */
internal object PortugueseSearchPolicy {
    fun accepts(comic: ComicEntry): Boolean {
        val language=comic.editionLanguage.trim().lowercase(Locale.ROOT).replace('_','-').substringBefore('-')
        if(language.isNotEmpty())return language in setOf("pt","por")
        val online=comic.isOnlineResult || listOf(comic.id,comic.externalId).any {id ->
            listOf("ab-","gb-","cv-","mt-").any(id::startsWith)
        }
        if(!online)return true // Curated/local entries retain their original identity.
        return PortugueseBookSources.supportedPortugueseDescription(comic.synopsis) &&
            PortugueseComicTranslator.isPortugueseText(comic.synopsis) &&
            !PortugueseComicTranslator.isUnwantedForeignOrBundleResult(comic.title,comic.synopsis)
    }
}
