package com.example

import com.example.data.FirebaseAiRecommendationEnhancer
import com.example.data.UserTasteProfile
import com.example.model.ComicEntry
import com.example.model.ReadingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Patch5FirebaseAiSafetyContractTest {

    private fun sample(id: String, title: String) = ComicEntry(
        id = id,
        externalId = id,
        title = title,
        authors = "Autor Teste",
        publisher = "DC / Vertigo",
        status = ReadingStatus.READ
    )

    @Test
    fun anonymizedPayloadDoesNotContainSensitivePersonalData() {
        val taste = UserTasteProfile(
            readingSeries = listOf("Monstro do Pântano" to 1),
            readSeries = listOf("Batman" to 3),
            topAuthors = listOf("Alan Moore"),
            favoriteCharacters = listOf("Sandman (Morpheus)"),
            favoriteComics = emptyList(),
            wantToReadSeries = listOf("Hellblazer"),
            favoriteUniverse = "DC / Vertigo",
            tasteHash = "hash12345"
        )
        val candidates = listOf(sample("c1", "Monstro do Pântano #2"))

        val payload = FirebaseAiRecommendationEnhancer.buildSafeAnonymizedPayload(taste, candidates)

        assertFalse(payload.contains("email"))
        assertFalse(payload.contains("felipe"))
        assertFalse(payload.contains("password"))
        assertTrue(payload.contains("candidatePool"))
        assertTrue(payload.contains("c1"))
    }

    @Test
    fun responseValidationRejectsHallucinatedCandidateIds() {
        val realCandidates = listOf(
            sample("real1", "Watchmen"),
            sample("real2", "Sandman")
        )

        // The AI responded with real1, but also hallucinated fake999
        val aiJson = """
            {
                "orderedCandidateIds": ["real1", "fake999"],
                "reasonOverrides": {
                    "real1": "Clássico absoluto dos quadrinhos",
                    "fake999": "Obra inventada"
                },
                "relatedSearchQueries": ["Alan Moore"]
            }
        """.trimIndent()

        val validated = FirebaseAiRecommendationEnhancer.validateAiResponse(aiJson, realCandidates)

        assertNotNull(validated)
        assertEquals(listOf("real1"), validated!!.orderedCandidateIds)
        assertTrue(validated.reasonOverrides.containsKey("real1"))
        assertFalse(validated.reasonOverrides.containsKey("fake999"))
        assertEquals(listOf("Alan Moore"), validated.relatedSearchTerms)
    }

    @Test
    fun invalidOrEmptyResponseReturnsNullForSafeLocalFallback() {
        val realCandidates = listOf(sample("real1", "Watchmen"))

        val invalidJson = "{ corrupt json ]"
        val result = FirebaseAiRecommendationEnhancer.validateAiResponse(invalidJson, realCandidates)

        assertNull(result)
    }
}
