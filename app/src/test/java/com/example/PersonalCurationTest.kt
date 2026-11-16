package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[24,34])
class PersonalCurationTest {
    @Test fun oldListsStillDecodeAndArtworkSurvivesBackups() {
        val item=ListComic.from(comic().copy(coverUrl="https://example.com/cover.jpg"))
        val list=ComicList(name="Destaque",items=listOf(item),coverKey=item.key,coverX=.2f,coverY=.8f,coverZoom=2f,featuredAt=10)
        assertEquals(list,CurationCodec.list(CurationCodec.encode(list)))
        val legacy=org.json.JSONObject(CurationCodec.encode(list)).apply {
            listOf("coverKey","coverX","coverY","coverZoom","featuredAt").forEach {remove(it)}
        }
        val old=CurationCodec.list(legacy.toString())
        assertEquals(item,old.coverItem());assertEquals(1f,old.coverZoom);assertEquals(0L,old.featuredAt)
        val storage=CurationStorage(ApplicationProvider.getApplicationContext())
        assertEquals(list,storage.decode(storage.json(CurationSnapshot(lists=listOf(list)))).lists.single())
    }
    @Test fun invalidArtworkTransformsAreRejectedBeforePersistence() {
        val base=ComicList(name="Lista")
        listOf(base.copy(coverX=Float.NaN),base.copy(coverY=1.1f),base.copy(coverZoom=.5f),
            base.copy(coverZoom=Float.POSITIVE_INFINITY),base.copy(coverKey="not-a-key"),base.copy(featuredAt=-1)).forEach {
            assertTrue(runCatching {CurationCodec.encode(it)}.isFailure)
        }
    }
    @Test fun removedCoverFallsBackAndOnlyOneListIsFeatured() {
        val a=ListComic.from(comic().copy(coverUrl="https://example.com/a.jpg"))
        val b=ListComic.from(comic("two","9781234567891").copy(coverUrl="https://example.com/b.jpg"))
        val first=ComicList(name="Primeira",items=listOf(a,b),coverKey=b.key,featuredAt=10)
        val second=ComicList(name="Segunda",featuredAt=20)
        assertEquals(b,first.coverItem());assertEquals(a,first.copy(items=listOf(a)).coverItem())
        assertEquals(second,featuredComicList(listOf(first,second)))
        assertEquals(first,featuredComicList(listOf(first,second.copy(deleted=true))))
        assertNull(featuredComicList(emptyList()))
    }
    private fun comic(id: String="one", isbn: String="9781234567890") = ComicEntry(id,"external-$id","Batman: Silêncio",
        authors="Jeph Loeb",publisher="DC Comics",isbn=isbn,editionLanguage="pt",pageCount=120,genre="Mistério")
    @Test fun editionIdentityAndListRoundTripPreserveOrderNotesAndRanking() {
        val a=ListComic.from(comic()); val same=ListComic.from(comic("another")); val b=ListComic.from(comic("two","9781234567891"))
        assertEquals(a.key,same.key); assertNotEquals(a.key,b.key)
        val list=ComicList(name="Meu ranking",ranked=true,items=listOf(b.copy(note="Primeiro"),a))
        assertEquals(list,CurationCodec.list(CurationCodec.encode(list)))
        assertEquals(0,b.comic().pageCount)
    }
    @Test fun duplicateEditionsAndOversizedUserInputAreRejectedBeforeWrite() {
        val a=ListComic.from(comic())
        listOf(ComicList(name="Teste",items=listOf(a,a)),ComicList(name="x".repeat(101)),
            ComicList(name="Teste",description="x".repeat(2001)),ComicList(name="Teste",items=listOf(a.copy(note="x".repeat(1001))))).forEach {
            assertTrue(runCatching {CurationCodec.encode(it)}.isFailure)
        }
    }
    @Test fun accountSnapshotsAreIsolatedAndOldBackupsHaveEmptyCuration() {
        val context=ApplicationProvider.getApplicationContext<Context>(); val storage=CurationStorage(context)
        val list=ComicList(name="Privada",items=listOf(ListComic.from(comic())))
        storage.save("alice",CurationSnapshot(listOf(list)))
        assertEquals(list,storage.load("alice").lists.single())
        assertTrue(storage.load("bob").lists.isEmpty())
        assertTrue(storage.decode(org.json.JSONObject()).lists.isEmpty())
        storage.saveDraft("alice",list);assertEquals(list,storage.draft("alice"));assertNull(storage.draft("bob"))
        storage.clearDraft("alice");assertNull(storage.draft("alice"))
    }
    @Test fun damagedSnapshotIsPreservedAndNeverTreatedAsAnEmptyCollection() {
        val context=ApplicationProvider.getApplicationContext<Context>();val storage=CurationStorage(context)
        val file=java.io.File(context.filesDir,"personal-curation/${CurationCodec.hash("damaged")}.json")
        file.writeText("{invalid")
        assertTrue(runCatching {storage.load("damaged")}.isFailure)
        assertEquals("{invalid",file.readText())
    }
    @Test fun providerLabelChangesDoNotDuplicateAnEditionWithoutIsbn() {
        val c=comic().copy(isbn="",sourceLabel="Metron")
        assertEquals(ListComic.from(c).key,ListComic.from(c.copy(sourceLabel="Metron · Catálogo",id="local-id")).key)
        assertNotEquals(ListComic.from(c).key,ListComic.from(c.copy(externalId="another-edition")).key)
    }
    @Test fun savingOneListNeverDiscardsTheDraftOfAnotherList() {
        val storage=CurationStorage(ApplicationProvider.getApplicationContext())
        val a=ComicList(name="Primeiro rascunho");val b=ComicList(name="Segundo rascunho")
        storage.saveDraft("draft-owner",a);storage.saveDraft("draft-owner",b)
        assertEquals(setOf(a.id,b.id),storage.drafts("draft-owner").map {it.id}.toSet())
        storage.clearDraft("draft-owner",a.id)
        assertEquals(b,storage.drafts("draft-owner").single())
        assertTrue(storage.drafts("other-draft-owner").isEmpty())
        storage.clearDraft("draft-owner")
    }
    @Test fun feedbackChangesTasteButPagesRevisionsAndListEditsDoNot() {
        val c=comic(); val settings=AppSettings()
        val base=PersonalizedDiscovery.computeCanonicalTasteFingerprint(listOf(c),settings,"DC")
        assertEquals(base,PersonalizedDiscovery.computeCanonicalTasteFingerprint(listOf(c.copy(currentPage=99,updatedAt=9,isDirty=true)),settings,"DC"))
        val hidden=RecommendationFeedback(hidden=listOf(ListComic.from(c)))
        assertNotEquals(base,PersonalizedDiscovery.computeCanonicalTasteFingerprint(listOf(c),settings,"DC",hidden))
        assertEquals(hidden,CurationCodec.feedback(CurationCodec.encode(hidden)))
        assertTrue(hidden.hides(comic("other")))
        assertEquals(hidden.canonical(),hidden.copy(revision=99).canonical())
    }
    @Test fun explanationUsesTheActualScoringSignalAndSurvivesCacheRoundTrip() {
        val c=comic(); val recommended=PersonalizedDiscovery.recommendWithReasons(listOf(c),emptyList(),"DC",
            AppSettings(profileFavoriteCharacters=listOf("Batman"))).single()
        assertEquals("FAVORITE_CHARACTER",recommended.explanation!!.kind)
        assertEquals("Batman",recommended.explanation!!.reference)
        assertEquals(recommended.explanation,PersonalizedDiscovery.decodeCachedRecommendations(
            PersonalizedDiscovery.encodeCachedRecommendations(listOf(recommended))).single().explanation)
        val hidden=RecommendationFeedback(hidden=listOf(ListComic.from(c)))
        assertTrue(PersonalizedDiscovery.recommendWithReasons(listOf(c),emptyList(),"DC",feedback=hidden).isEmpty())
    }
    @Test fun positiveFeedbackDoesNotChangeLibraryFavoritesAndBecomesARetrievalSignal() {
        val c=comic(); val feedback=RecommendationFeedback(boosted=listOf(ListComic.from(c)))
        val taste=PersonalizedDiscovery.extractTasteProfile(emptyList(),AppSettings(),"DC",feedback)
        assertTrue(taste.favoriteComics.isEmpty());assertEquals(c.displayTitle,taste.moreLike.single().displayTitle)
        val next=comic("next","9781234567891").copy(title="Batman: Vitória Sombria")
        val result=PersonalizedDiscovery.recommendWithReasons(listOf(next),emptyList(),"",feedback=feedback)
        assertEquals("EXPLICIT_MORE",result.single().signalSource)
    }
}
