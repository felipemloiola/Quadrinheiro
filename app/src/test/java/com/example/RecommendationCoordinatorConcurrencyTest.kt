package com.example

import com.example.data.MultiApiSearchEngine
import com.example.data.RecommendationCandidateRetriever
import com.example.data.UserTasteProfile
import com.example.model.ComicEntry
import com.example.model.ReadingStatus
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecommendationCoordinatorConcurrencyTest {

    @Before
    fun setUp() {
        RecommendationCandidateRetriever.clearForAccountSwitch()
    }

    @After
    fun tearDown() {
        RecommendationCandidateRetriever.clearForAccountSwitch()
    }

    private fun sampleComic(id: String, title: String): ComicEntry = ComicEntry(
        id = id,
        externalId = id,
        title = title,
        authors = "Grant Morrison",
        publisher = "DC Comics",
        genre = "Super-heróis",
        format = "Encadernado",
        releaseYear = 2020,
        pageCount = 200,
        currentPage = 200,
        status = ReadingStatus.READ,
        rating = 5f,
        favorite = true
    )

    @Test
    fun testCandidateCacheAccountIsolationAndSwitchClear() {
        val poolUserA = listOf(sampleComic("comic_1", "Batman RIP"))
        val hash = "hash_12345"

        // Cache pool for user_a
        RecommendationCandidateRetriever.updateCache(hash = hash, pool = poolUserA, ownerUid = "user_a")

        // Valid for user_a
        assertTrue(RecommendationCandidateRetriever.isCacheValid(hash, "user_a"))
        assertEquals(1, RecommendationCandidateRetriever.getCachedPool("user_a").size)

        // Invalid for user_b
        assertFalse(RecommendationCandidateRetriever.isCacheValid(hash, "user_b"))
        assertTrue(RecommendationCandidateRetriever.getCachedPool("user_b").isEmpty())

        // Clearing for account switch clears everything
        RecommendationCandidateRetriever.clearForAccountSwitch()
        assertFalse(RecommendationCandidateRetriever.isCacheValid(hash, "user_a"))
        assertTrue(RecommendationCandidateRetriever.getCachedPool("user_a").isEmpty())
    }

    @Test
    fun testZeroNetworkQueriesWhenFingerprintMatches() = runBlocking {
        val pool = listOf(sampleComic("comic_cached", "All-Star Superman"))
        val taste = UserTasteProfile(
            readingSeries = listOf("Batman" to 10),
            readSeries = emptyList(),
            topAuthors = listOf("Grant Morrison"),
            favoriteCharacters = listOf("Batman"),
            favoriteComics = pool,
            wantToReadSeries = emptyList(),
            favoriteUniverse = "DC",
            tasteHash = "cached_taste_hash_abc"
        )

        // Populate cache
        RecommendationCandidateRetriever.updateCache(taste.tasteHash, pool, "test_user")

        var queriesExecuted = 0
        // SearchEngine with counting flow
        val searchEngine = object : MultiApiSearchEngine() {
            override fun searchOnlineProgressive(query: String, searchLanguage: String, provider: String, hdCovers: Boolean) = flow {
                queriesExecuted++
                emit(listOf(sampleComic("unexpected", query)))
            }
        }

        val result = RecommendationCandidateRetriever.retrieveCandidates(
            taste = taste,
            searchEngine = searchEngine,
            ownerUid = "test_user"
        )

        assertEquals(0, queriesExecuted)
        assertEquals(1, result.size)
        assertEquals("All-Star Superman", result[0].title)
    }

    @Test
    fun testParallelCandidateRetrievalUnderBudgetWithPartialBatchRetention() = runBlocking {
        val taste = UserTasteProfile(
            readingSeries = listOf("Saga" to 5),
            readSeries = emptyList(),
            topAuthors = listOf("Brian K. Vaughan"),
            favoriteCharacters = listOf("Marko"),
            favoriteComics = emptyList(),
            wantToReadSeries = emptyList(),
            favoriteUniverse = "Image",
            tasteHash = "taste_hash_parallel_test"
        )

        val searchEngine = object : MultiApiSearchEngine() {
            override fun searchOnlineProgressive(query: String, searchLanguage: String, provider: String, hdCovers: Boolean) = flow {
                // Emit immediate batch
                emit(listOf(sampleComic("id_$query", "Result for $query")))
            }
        }

        val results = RecommendationCandidateRetriever.retrieveCandidates(
            taste = taste,
            searchEngine = searchEngine,
            ownerUid = "user_parallel"
        )

        assertTrue(results.isNotEmpty())
        assertTrue(RecommendationCandidateRetriever.isCacheValid(taste.tasteHash, "user_parallel"))
    }
}
