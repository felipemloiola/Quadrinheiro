package com.example

import com.example.data.QuadrinheiroCloudRepository
import com.example.model.ComicEntry
import com.example.model.ReaderProfile
import com.example.model.ReadingStatus
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class QuadrinheiroRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun sampleComic(id: String = "hq-watchmen") = ComicEntry(
        id = id,
        externalId = "cat-watchmen",
        title = "Watchmen: Edição Definitiva",
        authors = "Alan Moore, Dave Gibbons",
        publisher = "DC Comics",
        genre = "DC / Vertigo",
        format = "Edição de Luxo (Capa Dura)",
        releaseYear = 1986,
        pageCount = 416,
        currentPage = 416,
        status = ReadingStatus.READ,
        rating = 5.0f,
        favorite = true,
        rereadCount = 1,
        tags = listOf("Clássico absoluto", "Roteiro impecável"),
        review = "Leitura indispensável.",
        readDate = "2026-03-15",
        addedAt = "2026-03-15T10:00:00Z",
        isbn = "9781779501127",
        coverUrl = "https://books.google.com/books/content?id=watchmen",
        synopsis = "Sinopse de Watchmen.",
        sourceLabel = "Catálogo Base"
    )

    @Test
    fun saveAndGetUserProfileAndComic_authenticatedOwner_succeeds() = runBlocking {
        val repo = QuadrinheiroCloudRepository(context, db, auth)
        val aliceUid = signInTestUser("alice@example.com")

        val profile = ReaderProfile(
            name = "Alice Quadrinheira",
            handle = "@alicehq",
            bio = "Colecionadora de encadernados e mangás.",
            favoriteUniverse = "DC / Vertigo",
            avatarPreset = "indigo",
            yearlyGoal = 30,
            dailyPagesGoal = 40,
            showGoalOnHome = true
        )

        withTimeout(5000L) {
            repo.saveUserProfile(aliceUid, profile)
        }

        val fetchedProfile = withTimeout(5000L) {
            repo.getUserProfile(aliceUid)
        }
        assertNotNull(fetchedProfile)
        assertEquals("Alice Quadrinheira", fetchedProfile?.name)
        assertEquals(30, fetchedProfile?.yearlyGoal)

        val comic = sampleComic("hq-watchmen")
        withTimeout(5000L) {
            repo.saveComic(aliceUid, comic)
        }

        val fetchedComic = withTimeout(5000L) {
            repo.getComic(aliceUid, "hq-watchmen")
        }
        assertNotNull(fetchedComic)
        assertEquals("Watchmen: Edição Definitiva", fetchedComic?.title)
        assertEquals(5.0f, fetchedComic?.rating ?: 0f, 0.01f)
        assertTrue(fetchedComic?.favorite == true)
    }

    @Test
    fun saveComic_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val repo = QuadrinheiroCloudRepository(context, db, auth)
        val aliceUid = signInTestUser("alice@example.com")
        val comic = sampleComic("hq-cross-user")

        withTimeout(5000L) {
            repo.saveComic(aliceUid, comic)
        }

        signInTestUser("bob@example.com")
        try {
            withTimeout(5000L) {
                repo.saveComic(aliceUid, comic.copy(title = "Overwrite by Bob"))
            }
            fail("Expected PERMISSION_DENIED when Bob writes to Alice's collection")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun observeComics_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = QuadrinheiroCloudRepository(context, db, auth)
        try {
            withTimeout(3000L) {
                repo.observeUserComics("unauth_target_uid").first()
            }
            fail("Expected PERMISSION_DENIED when unauthenticated user observes comics")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }
}
