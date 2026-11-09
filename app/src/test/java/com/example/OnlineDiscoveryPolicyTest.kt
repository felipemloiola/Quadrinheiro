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
    @Test fun cachedTranslationCannotInventAFavoriteCharacter() {
        val entry=comic("siktc","Something is Killing the Children").copy(
            authors="James Tynion IV",titlePtBr="Monstro do Pântano is Killing the Children",
            seriesName="Monstro do Pântano")
        val verified=PersonalizedDiscovery.verifiedCandidate(entry)!!
        assertEquals("Something is Killing the Children",verified.displayTitle)
        assertEquals("",verified.seriesName)
        assertTrue(PersonalizedDiscovery.recommendWithReasons(listOf(entry),emptyList(),
            settings=AppSettings(profileFavoriteCharacters=listOf("Monstro do Pântano"))).isEmpty())
    }

    @Test fun explicitlyForeignEditionsAndLegacyForeignTitlesAreExcluded() {
        assertFalse(ComicVineCatalog.supportedEdition(org.json.JSONObject().put("language","de")))
        assertFalse(ComicVineCatalog.supportedEdition(org.json.JSONObject().put("language","it")))
        assertFalse(ComicVineCatalog.supportedEdition(org.json.JSONObject().put("language","zh")))
        assertTrue(ComicVineCatalog.supportedEdition(org.json.JSONObject().put("language","pt-BR")))
        assertTrue(ComicVineCatalog.supportedEdition(org.json.JSONObject().put("language","en")))
        assertNull(PersonalizedDiscovery.verifiedCandidate(comic("de","Swamp Thing: Grüne Hölle")))
        assertNull(PersonalizedDiscovery.verifiedCandidate(comic("it","Swamp Thing di Alan Moore - Volume 3")))
        assertEquals("Uma história em português",PersonalizedDiscovery.verifiedCandidate(
            comic("translated","An unfamiliar story").copy(titlePtBr="Uma história em português"))?.displayTitle)
    }

    @Test fun emptyNormalizedSeriesIsNotAContinuationAndStandaloneDoesNotImplyASequel() {
        val reading=comic("reading","Batman: Ano Um").copy(status=ReadingStatus.READING,issueNumber=0)
        val chinese=comic("zh","雲海爭奇錄 期刊十六").copy(authors="蘆葦草",publisher="",seriesName="雲海爭奇錄")
        assertNull(PersonalizedDiscovery.verifiedCandidate(chinese))
        val other=comic("other","Batman: O Longo Dia das Bruxas").copy(issueNumber=1)
        val recs=PersonalizedDiscovery.recommendWithReasons(listOf(chinese,other),listOf(reading))
        assertFalse(recs.any { it.signalSource=="READING_CONTINUATION" || it.comic.id=="zh" })
        val monthly=reading.copy(id="monthly",externalId="monthly",title="Batman Absoluto #1",
            seriesName="Batman Absoluto",format="Edição",issueNumber=1,pageCount=24)
        val next=monthly.copy(id="next",externalId="next",title="Batman Absoluto #2",issueNumber=2)
        assertTrue(PersonalizedDiscovery.recommendWithReasons(listOf(next),listOf(monthly))
            .any { it.signalSource=="READING_CONTINUATION" })
    }

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
