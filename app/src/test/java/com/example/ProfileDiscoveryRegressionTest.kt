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
@Config(sdk = [34])
class ProfileDiscoveryRegressionTest {
    @Test fun showcaseSurvivesLogoutAndLateSnapshotWithoutLeakingToOtherAccount() {
        val storage = SafeStorage(ApplicationProvider.getApplicationContext<Context>())
        val uid = "profile49-alice"
        val settings = AppSettings(profileFavoriteComicIds = listOf("external-hq"), profileFavoriteCharacters = listOf("Batman"),
            profileShowcaseDetails = mapOf("character:Batman" to "https://example.com/batman.jpg"), profileBannerUrl = "data:image/jpeg;base64,banner", profileShowcaseUpdatedAt = 200)
        try {
            storage.saveState(emptyList(), ReaderProfile(updatedAt = 100, isDirty = true), AppSettings(), uid)
            storage.saveShowcase(uid, settings)
            storage.clearAuthSession()
            val restored = storage.loadAccountState(uid)!!.settings
            assertEquals(settings.profileFavoriteComicIds, restored.profileFavoriteComicIds)
            assertEquals(settings.profileFavoriteCharacters, restored.profileFavoriteCharacters)
            assertEquals(settings.profileBannerUrl, restored.profileBannerUrl)
            assertEquals(settings.profileShowcaseDetails, restored.profileShowcaseDetails)
            storage.saveState(emptyList(), ReaderProfile(updatedAt = 300, isDirty = true), restored, uid)
            storage.markProfileSynced(100, uid)
            assertTrue(storage.loadAccountState(uid)!!.profile.isDirty)
            storage.markProfileSynced(300, uid)
            assertFalse(storage.loadAccountState(uid)!!.profile.isDirty)
            storage.saveState(emptyList(), ReaderProfile(), AppSettings(), "profile49-bob")
            assertTrue(storage.loadAccountState("profile49-bob")!!.settings.profileFavoriteCharacters.isEmpty())
        } finally {
            listOf(uid, "profile49-bob").forEach { storage.removeItem(SafeStorage.ACCOUNT_STATE_PREFIX + it); storage.removeItem("profile_showcase_$it") }
        }
    }
    @Test fun oldCloudCannotEraseFavoritesButNewerExplicitRemovalCan() {
        val local = AppSettings(profileFavoriteCharacters = listOf("Batman"), profileShowcaseUpdatedAt = 20)
        assertEquals(local.profileFavoriteCharacters, ProfileShowcase.merge(local, AppSettings()).profileFavoriteCharacters)
        assertTrue(ProfileShowcase.merge(local, AppSettings(profileShowcaseUpdatedAt = 21)).profileFavoriteCharacters.isEmpty())
        val legacy = local.copy(profileShowcaseUpdatedAt = 0)
        assertEquals(legacy.profileFavoriteCharacters, ProfileShowcase.merge(legacy, AppSettings()).profileFavoriteCharacters)
    }
    @Test fun externalComicStillHasCoverAndTitleAfterCatalogChanges() {
        val comic = ComicEntry("outside-catalog", "", "Uma obra", coverUrl = "https://example.com/cover.jpg", authors = "Autora", publisher = "Editora")
        val snapshot = ProfileShowcase.restoreComic(ProfileShowcase.comicSnapshot(comic))!!
        assertEquals(comic.id, snapshot.id); assertEquals(comic.coverUrl, snapshot.coverUrl); assertEquals(comic.title, snapshot.title)
    }
    @Test fun characterSearchMatchesPortugueseEnglishAndCivilianNameWithPortrait() {
        val data = CharacterDirectory.parse("""[{"id":1,"name":"Swamp Thing","images":{"md":"https://example.com/swamp.jpg"},"biography":{"fullName":"Alec Holland","aliases":[]}}]""")
        listOf("monstro do pantano", "Swamp Thing", "Alec Holland").forEach { query ->
            assertEquals("https://example.com/swamp.jpg", CharacterDirectory.match(data, query).single().image)
        }
    }
    @Test fun favoritePublisherInfluencesRecommendationsWithoutReadingHistory() {
        val marvel = ComicEntry("marvel", "", "Marvel HQ", publisher = "Marvel Comics", authors = "Autor")
        val dc = ComicEntry("dc", "", "DC HQ", publisher = "DC Comics", authors = "Autor")
        assertEquals(dc.id, PersonalizedDiscovery.recommend(listOf(marvel, dc), emptyList(), "DC / Vertigo").first().id)
    }
    @Test fun standaloneComicVineVolumeDoesNotAcquireMonthlyNumbering() {
        val comic = ComicVineCatalog.comic(org.json.JSONObject("""{"id":"123","volume":"Batman: Year One","issue":"1","name":"","date":"1987-01-01"}"""))
        assertEquals(0, comic.issueNumber)
        assertEquals("Graphic Novel", comic.format)
    }
    @Test fun paginatedCatalogDeduplicatesAcrossProvidersAndBoundsVisibleWindow() {
        val window = CatalogWindow()
        val first = ComicEntry("a", "", "Batman: Ano Um", authors = "Frank Miller", publisher = "DC", isbn = "9781234567890")
        window.accept(MultiApiSearchEngine.CatalogPage(listOf(first), true))
        window.accept(MultiApiSearchEngine.CatalogPage(listOf(first.copy(id = "b", externalId = "other")), true))
        assertEquals(1, window.items.size); assertEquals(2, window.nextPage); assertFalse(window.autoLoad)
        repeat(50) { page -> window.accept(MultiApiSearchEngine.CatalogPage((0..11).map { i -> ComicEntry("$page-$i", "", "Obra $page número $i", authors = "Autor $page", publisher = "Editora") }, true)) }
        assertTrue(window.items.size <= 396)
        assertEquals(window.items.size, window.items.map { it.id }.distinct().size)
    }
}
