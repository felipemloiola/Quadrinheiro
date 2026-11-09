package com.example

import com.example.data.*
import com.example.model.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MemoryCacheSettings
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class OnlineDiscoveryPolicyTest {
    private fun comic(id:String,title:String,publisher:String="DC Comics") = ComicEntry(
        id=id,externalId=id,title=title,authors="Frank Miller",publisher=publisher,
        format="Graphic Novel",pageCount=144,coverUrl="https://example.org/$id.jpg")

    @Test fun sdkUsesOnlyMemoryAndConfigurationIsIdempotent() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val app=FirebaseApp.initializeApp(context,FirebaseOptions.Builder()
            .setProjectId("demo-online-policy").setApplicationId("1:123:android:abc")
            .setApiKey("fake-test-key").build(),"online-policy-test")
        try {
            val db=FirebaseFirestore.getInstance(app)
            OnlineFirestorePolicy.configure(db)
            OnlineFirestorePolicy.configure(db)
            assertTrue(db.firestoreSettings.cacheSettings is MemoryCacheSettings)
        } finally {app.delete()}
    }

    @Test fun editorialSelectionRejectsUnrelatedAlphabeticalResults() {
        val unrelated=comic("a","Action Comics")
        val correct=comic("b","Watchmen")
        assertEquals("b",EditorialDiscovery.select("Watchmen",listOf(unrelated,correct))?.id)
        assertNull(EditorialDiscovery.select("Watchmen",listOf(unrelated)))
        assertEquals(setOf("Para começar","Universo DC","Universo Marvel"),EditorialDiscovery.selections.keys)
    }

    @Test fun translatedEditionsAppearOnlyOnceInRecommendations() {
        val recommendations=PersonalizedDiscovery.recommendWithReasons(
            listOf(comic("en","Batman: Year One").copy(issueNumber=1),comic("pt","Batman: Ano Um")),
            emptyList(),"DC / Vertigo")
        assertEquals(1,recommendations.size)
        assertTrue(recommendations.single().reasonLabel.contains("DC"))
    }

    @Test fun savedSelectionSurvivesRestartWithSameTasteAndExcludesOtherAccounts() {
        val storage=SafeStorage(ApplicationProvider.getApplicationContext())
        val entries=listOf(RecommendedComic(comic("watchmen","Watchmen"),"Sua editora favorita",60,"FAVORITE_UNIVERSE"))
        val fingerprint=PersonalizedDiscovery.computeCanonicalTasteFingerprint(emptyList(),AppSettings(),"DC")
        storage.saveAccountRecommendationsSnapshot("online-reader",fingerprint,entries,0)
        val restored=SafeStorage(ApplicationProvider.getApplicationContext()).loadAccountRecommendationsSnapshot("online-reader")
        assertEquals(fingerprint,restored?.tasteFingerprint)
        assertEquals("watchmen",restored?.recommendations?.single()?.comic?.id)
        assertNull(storage.loadAccountRecommendationsSnapshot("other-online-reader"))
    }
}
