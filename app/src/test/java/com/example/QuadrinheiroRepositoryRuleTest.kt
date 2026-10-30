package com.example

import com.example.data.QuadrinheiroCloudRepository
import com.example.model.ComicEntry
import com.example.model.ReaderProfile
import com.example.model.ReadingStatus
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
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
        try {
            withTimeout(5000L) {
                db.collection("users").document("unauth_target_uid")
                    .collection("comics")
                    .get()
                    .await()
            }
            fail("Expected PERMISSION_DENIED when unauthenticated user observes comics")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun `test strict firestore rules reject oversized diary moment note and invalid data types`() = runBlocking {
        val uid = signInTestUser("user_alpha@example.com")
        val oversizedNote = "A".repeat(2500) // > 2000 chars allowed by isValidComicMomentItem
        val invalidMomentMap = mapOf(
            "id" to "m-1",
            "page" to 12,
            "tag" to "⚡ Momento Épico",
            "note" to oversizedNote,
            "date" to "2026-10-14",
            "isSpoiler" to false
        )
        val rawDoc = mapOf(
            "ownerId" to uid,
            "externalId" to "hq-invalid-moment",
            "title" to "Watchmen",
            "titlePtBr" to "Watchmen",
            "authors" to "Alan Moore",
            "publisher" to "DC / Vertigo",
            "genre" to "DC / Vertigo",
            "format" to "Edição Definitiva",
            "releaseYear" to 1987,
            "pageCount" to 416,
            "currentPage" to 416,
            "status" to "READ",
            "rating" to 5.0,
            "favorite" to true,
            "rereadCount" to 0,
            "tags" to listOf("Clássico"),
            "review" to "Resenha válida",
            "readDate" to "2026-10-14",
            "addedAt" to "2026-10-14T10:00:00Z",
            "isbn" to "9781779501127",
            "coverUrl" to "",
            "synopsis" to "Sinopse",
            "synopsisPtBr" to "Sinopse",
            "sourceLabel" to "Catálogo Base",
            "moments" to listOf(invalidMomentMap),
            "updatedAtMillis" to 1700000000000L,
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        try {
            db.collection("users").document(uid)
                .collection("comics").document("hq-invalid-moment")
                .set(rawDoc).await()
            fail("Expected PERMISSION_DENIED when diary moment note exceeds 2000 characters")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun `test hybrid sync conflict resolution keeps largest updatedAt and only syncs dirty items`() = runBlocking {
        val localCleanOlder = ComicEntry(
            id = "hq-1",
            externalId = "hq-1",
            title = "Batman: Ano Um",
            authors = "Frank Miller",
            publisher = "DC Comics",
            currentPage = 20,
            status = ReadingStatus.READING,
            updatedAt = 1000L,
            isDirty = false
        )
        val localDirtyNewer = ComicEntry(
            id = "hq-2",
            externalId = "hq-2",
            title = "Sandman: Prelúdios",
            authors = "Neil Gaiman",
            publisher = "DC / Vertigo",
            currentPage = 160,
            status = ReadingStatus.READ,
            rating = 5f,
            updatedAt = 5000L,
            isDirty = true
        )
        val remoteNewerForHq1 = localCleanOlder.copy(
            currentPage = 80,
            updatedAt = 3000L,
            isDirty = false
        )
        val remoteOlderForHq2 = localDirtyNewer.copy(
            currentPage = 45,
            status = ReadingStatus.READING,
            rating = 0f,
            updatedAt = 2000L,
            isDirty = false
        )

        val (merged, dirtyToPush) = QuadrinheiroCloudRepository.mergeLocalAndRemoteComics(
            localComics = listOf(localCleanOlder, localDirtyNewer),
            remoteComics = listOf(remoteNewerForHq1, remoteOlderForHq2)
        )

        assertEquals(2, merged.size)
        val resolvedHq1 = merged.first { it.id == "hq-1" }
        val resolvedHq2 = merged.first { it.id == "hq-2" }

        // hq-1: remote had larger updatedAt (3000L > 1000L), so remote wins
        assertEquals(80, resolvedHq1.currentPage)
        assertEquals(3000L, resolvedHq1.updatedAt)

        // hq-2: local was dirty and had larger updatedAt (5000L > 2000L), so local wins and is queued for cloud push
        assertEquals(160, resolvedHq2.currentPage)
        assertEquals(ReadingStatus.READ, resolvedHq2.status)
        assertEquals(1, dirtyToPush.size)
        assertEquals("hq-2", dirtyToPush.first().id)
    }
}

