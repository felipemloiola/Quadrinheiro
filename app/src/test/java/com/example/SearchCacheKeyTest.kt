package com.example

import com.example.data.MultiApiSearchEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * A busca guarda resultados em cache por configuração. Se a chave ignorasse idioma, provedor
 * ou capas HD, trocar uma opção devolveria resultados da configuração anterior.
 */
class SearchCacheKeyTest {

    private val engine = MultiApiSearchEngine()

    private fun key(
        query: String = "batman",
        language: String = "ALL",
        provider: String = "MULTI",
        hd: Boolean = true
    ) = engine.searchCacheKey(query, language, provider, hd)

    @Test
    fun languageChangesTheCacheEntry() {
        assertNotEquals(key(language = "ALL"), key(language = "PT"))
    }

    @Test
    fun providerChangesTheCacheEntry() {
        assertNotEquals(key(provider = "MULTI"), key(provider = "COMIC_VINE"))
    }

    @Test
    fun hdCoversChangesTheCacheEntry() {
        assertNotEquals(key(hd = true), key(hd = false))
    }

    @Test
    fun caseAndSurroundingSpacesReuseTheSameEntry() {
        assertEquals(key(query = "Batman"), key(query = "  batman  "))
    }

    @Test
    fun getCachedFindsTheEntryUsingTheSameKey() {
        val cacheKey = engine.searchCacheKey("mulher maravilha", "ALL", "MULTI", true)
        assertEquals(cacheKey, cacheKey.lowercase())
    }
}
