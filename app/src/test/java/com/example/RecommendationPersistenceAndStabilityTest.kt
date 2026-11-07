package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PersonalizedDiscovery
import com.example.data.RecommendedComic
import com.example.data.SafeStorage
import com.example.data.StorageMaintenance
import com.example.model.AppSettings
import com.example.model.ComicEntry
import com.example.model.ReadingStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.charset.StandardCharsets

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecommendationPersistenceAndStabilityTest {

    private lateinit var context: Context
    private lateinit var storage: SafeStorage

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        storage = SafeStorage(context)
    }

    @After
    fun tearDown() {
        storage.clearAuthSession()
    }

    private fun sampleComic(
        id: String,
        title: String,
        authors: String = "Alan Moore",
        publisher: String = "DC / Vertigo",
        status: ReadingStatus = ReadingStatus.READ,
        rating: Float = 5f,
        favorite: Boolean = false,
        rereadCount: Int = 0,
        seriesName: String = "",
        issueNumber: Int = 0
    ) = ComicEntry(
        id = id,
        externalId = "ext-$id",
        title = title,
        authors = authors,
        publisher = publisher,
        status = status,
        rating = rating,
        favorite = favorite,
        rereadCount = rereadCount,
        seriesName = seriesName,
        issueNumber = issueNumber,
        coverUrl = "https://example.com/$id.jpg"
    )

    @Test
    fun tasteFingerprintIsDeterministicAndIgnoresVolatileFields() {
        val c1 = sampleComic("c1", "Watchmen", status = ReadingStatus.READ, rating = 5f)
        val c2 = sampleComic("c2", "Sandman #1", seriesName = "Sandman", authors = "Neil Gaiman", issueNumber = 1, status = ReadingStatus.READING)
        val listA = listOf(c1, c2)
        val listB = listOf(c2, c1) // Ordem invertida

        val settings = AppSettings(
            profileFavoriteCharacters = listOf("Batman", "Sandman"),
            profileFavoriteComicIds = listOf("c1")
        )

        val hashA = PersonalizedDiscovery.computeCanonicalTasteFingerprint(listA, settings, "DC / Vertigo")
        val hashB = PersonalizedDiscovery.computeCanonicalTasteFingerprint(listB, settings, "DC / Vertigo")

        assertEquals("Mesmo conteúdo em ordem diferente deve produzir exatamente o mesmo tasteFingerprint", hashA, hashB)

        // Alteração apenas de currentPage, coverUrl e updatedAt NÃO deve alterar o fingerprint
        val c2Mutated = c2.copy(currentPage = 50, coverUrl = "https://example.com/new_cover.jpg", updatedAt = 9999999L, isDirty = true)
        val listMutated = listOf(c1, c2Mutated)
        val hashMutated = PersonalizedDiscovery.computeCanonicalTasteFingerprint(listMutated, settings, "DC / Vertigo")

        assertEquals("Mutações de progresso/capa/data não devem alterar o tasteFingerprint", hashA, hashMutated)

        // Mudança semântica real (status ou rating) DEVE alterar o fingerprint
        val c1RatedLower = c1.copy(rating = 2f)
        val hashDifferentRating = PersonalizedDiscovery.computeCanonicalTasteFingerprint(listOf(c1RatedLower, c2), settings, "DC / Vertigo")
        assertNotEquals("Mudança de nota deve alterar o tasteFingerprint", hashA, hashDifferentRating)

        val c2Completed = c2.copy(status = ReadingStatus.READ)
        val hashDifferentStatus = PersonalizedDiscovery.computeCanonicalTasteFingerprint(listOf(c1, c2Completed), settings, "DC / Vertigo")
        assertNotEquals("Mudança de status deve alterar o tasteFingerprint", hashA, hashDifferentStatus)
    }

    @Test
    fun durableSnapshotSavesAndRestoresUpTo24RecommendationsAtomically() {
        val aliceUid = "user_alice_123"
        val sampleRecs = (1..24).map { idx ->
            RecommendedComic(
                comic = sampleComic("rec-$idx", "HQ Sugerida $idx", issueNumber = idx),
                reasonLabel = "Porque você leu HQ $idx",
                score = 100 - idx,
                signalSource = "SERIES_CONTINUATION"
            )
        }

        val fingerprint = "canonical_hash_v2_test"
        val saved = storage.saveAccountRecommendationsSnapshot(
            ownerUid = aliceUid,
            tasteFingerprint = fingerprint,
            items = sampleRecs,
            libraryCount = 5
        )
        assertTrue("Snapshot deve ser salvo com sucesso", saved)

        val loaded = storage.loadAccountRecommendationsSnapshot(aliceUid)
        assertNotNull(loaded)
        assertEquals(2, loaded!!.schemaVersion)
        assertEquals(aliceUid, loaded.ownerUid)
        assertEquals(fingerprint, loaded.tasteFingerprint)
        assertEquals(5, loaded.libraryCount)
        assertEquals(24, loaded.recommendations.size)
        assertFalse(loaded.isLegacyProvisional)
        assertEquals("rec-1", loaded.recommendations.first().comic.id)
        assertEquals("rec-24", loaded.recommendations.last().comic.id)

        // Verifica que o arquivo físico está em filesDir (fora de cacheDir) e não é apagado por clearRecoverable
        val targetFile = storage.durableRecommendationsFile(aliceUid)
        assertTrue(targetFile.exists())
        assertTrue("Arquivo deve estar dentro de filesDir", targetFile.canonicalPath.contains("files/quadrinheiro_recommendations"))

        // Executa limpeza de cache recuperável
        StorageMaintenance.clearRecoverable(context)

        // O arquivo durável da conta deve continuar existindo e legível
        val afterClear = storage.loadAccountRecommendationsSnapshot(aliceUid)
        assertNotNull("Limpeza de cache recuperável NUNCA deve apagar o snapshot durável de recomendações", afterClear)
        assertEquals(24, afterClear!!.recommendations.size)
    }

    @Test
    fun differentAccountUidsDoNotCollideInFilenames() {
        val uid1 = "user/with/slashes"
        val uid2 = "user_with_slashes"
        val key1 = storage.recommendationFileKey(uid1)
        val key2 = storage.recommendationFileKey(uid2)

        assertNotEquals("UIDs diferentes jamais devem colidir no mesmo nome de arquivo", key1, key2)
        assertTrue(key1.startsWith("recs_v2_"))
        assertTrue(key2.startsWith("recs_v2_"))
        assertEquals(72, key1.length) // "recs_v2_" (8) + 64 hex SHA-256 = 72 chars
    }

    @Test
    fun twoTierBudgetPreserves24LocallyAnd16ForCloudWithoutTruncation() {
        val recs = (1..24).map { idx ->
            RecommendedComic(
                comic = sampleComic("rec-$idx", "Edição Muito Longa e Detalhada $idx", issueNumber = idx),
                reasonLabel = "Baseado nos seus gostos recentes $idx",
                score = 150 - idx,
                signalSource = "TASTE_MATCH"
            )
        }

        // Camada Nuvem / Preferences: max 16 itens e <= 12 KB
        val cloudEncoded = PersonalizedDiscovery.encodeCachedRecommendations(recs)
        assertTrue(cloudEncoded.isNotBlank())
        val cloudBytes = cloudEncoded.toByteArray(StandardCharsets.UTF_8).size
        assertTrue("Nuvem deve ter no máximo 12288 bytes", cloudBytes <= 12288)
        val cloudDecoded = PersonalizedDiscovery.decodeCachedRecommendations(cloudEncoded)
        assertEquals("Nuvem deve ter no máximo 16 itens", 16, cloudDecoded.size)

        // Camada Local Durável: max 24 itens e <= 64 KB
        val durableEncoded = PersonalizedDiscovery.encodeDurableAccountRecommendations(recs)
        assertTrue(durableEncoded.isNotBlank())
        val durableBytes = durableEncoded.toByteArray(StandardCharsets.UTF_8).size
        assertTrue("Local deve ter no máximo 65536 bytes", durableBytes <= 65536)
        val durableDecoded = PersonalizedDiscovery.decodeCachedRecommendations(durableEncoded)
        assertEquals("Local durável deve manter todos os 24 itens", 24, durableDecoded.size)
    }
}
