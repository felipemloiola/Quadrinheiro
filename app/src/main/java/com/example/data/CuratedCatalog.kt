package com.example.data

import com.example.model.ComicEntry

/** Compatibility facade; all active records come from the current API. */
object CuratedCatalog {
 val curatedComicsCatalog:List<ComicEntry> get()=ModernCatalog.current
 val initialUserCollection:List<ComicEntry> = emptyList()
 val shelfGenres=listOf("Todos","DC / Vertigo","Marvel","Mangás","Independentes")
 fun findCoverUrlByTitle(title:String):String?=ModernCatalog.current.firstOrNull { PortugueseComicTranslator.canonicalWorkKey(it.title)==PortugueseComicTranslator.canonicalWorkKey(title) }?.coverUrl
 val quickSearchSuggestions=listOf("Batman", "Watchmen", "Sandman", "Homem-Aranha", "Mangás")
}
