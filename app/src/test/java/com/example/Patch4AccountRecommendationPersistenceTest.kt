package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.CloudUserPreferences
import com.example.data.SafeStorage
import com.example.model.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Patch4AccountRecommendationPersistenceTest {

    @Test
    fun appSettingsSerializesAndRestoresRecommendationFieldsInSafeStorage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = SafeStorage(context)

        val originalSettings = AppSettings(
            cachedRecommendationsJson = """[{"id":"rec1","title":"Watchmen","reasonLabel":"Clássico absoluto"}]""",
            recommendationTasteHash = "a1b2c3d4e5f60718"
        )

        storage.saveState(emptyList(), com.example.model.ReaderProfile(), originalSettings, ownerUid = "user_test_42")
        val loaded = storage.loadAccountState("user_test_42")?.settings ?: storage.loadState().settings

        assertEquals(originalSettings.cachedRecommendationsJson, loaded.cachedRecommendationsJson)
        assertEquals(originalSettings.recommendationTasteHash, loaded.recommendationTasteHash)
    }

    @Test
    fun cloudUserPreferencesPreservesCompactJsonWithinLimits() {
        val longJson = "a".repeat(20000)
        val settings = AppSettings(
            cachedRecommendationsJson = longJson,
            recommendationTasteHash = "hash12345"
        )

        val cloud = CloudUserPreferences.fromAppSettings(settings)

        assertTrue(cloud.cachedRecommendationsJson.length <= 12288)
        assertEquals("hash12345", cloud.recommendationTasteHash)

        val restored = cloud.toAppSettings()
        assertEquals(cloud.cachedRecommendationsJson, restored.cachedRecommendationsJson)
        assertEquals(cloud.recommendationTasteHash, restored.recommendationTasteHash)
    }

    @Test
    fun cloudUserPreferencesNeverSlicesJsonArrayMidElementAndCloudComicKeepsEditionLanguage() {
        val largeArray = org.json.JSONArray()
        for (i in 1..60) {
            largeArray.put(
                org.json.JSONObject()
                    .put("id", "rec-$i")
                    .put("title", "Edição Especial Muito Longa Número $i com Subtítulo Completo")
                    .put("authors", "Autor Um, Autor Dois, Autor Três")
                    .put("coverUrl", "https://example.org/covers/very-long-cover-path-number-$i.jpg")
                    .put("reasonLabel", "Continuação da sua leitura favorita")
            )
        }
        val rawLargeJson = largeArray.toString()
        assertTrue(rawLargeJson.length > 12288)

        val cloud = CloudUserPreferences.fromAppSettings(
            AppSettings(cachedRecommendationsJson = rawLargeJson, recommendationTasteHash = "hash_v16")
        )
        assertTrue(cloud.cachedRecommendationsJson.length <= 12288)
        val parsedBack = org.json.JSONArray(cloud.cachedRecommendationsJson)
        assertTrue(parsedBack.length() > 0)

        val comic = com.example.model.ComicEntry(
            id = "mt-777",
            externalId = "mt-777",
            title = "Daredevil: Born Again",
            authors = "Frank Miller",
            publisher = "Marvel",
            editionLanguage = "pt"
        )
        val cloudDoc = com.example.data.CloudComicDocument.fromComicEntry("uid_test", comic)
        assertEquals("pt", cloudDoc.editionLanguage)
        assertEquals("pt", cloudDoc.toComicEntry().editionLanguage)

        val sanitizedLog = com.example.data.formatSanitizedFirestoreLog(
            IllegalStateException("secret@email.com /users/uid_secret_123"),
            com.example.data.OperationType.WRITE
        )
        org.junit.Assert.assertFalse(sanitizedLog.contains("secret@email.com"))
        org.junit.Assert.assertFalse(sanitizedLog.contains("uid_secret_123"))
        assertTrue(sanitizedLog.contains("\"operationType\":\"write\""))
    }
}
