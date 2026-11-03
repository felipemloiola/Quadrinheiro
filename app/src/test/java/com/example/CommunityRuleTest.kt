package com.example

import com.example.data.CommunityRepository
import com.example.model.ComicEntry
import com.example.model.ReaderProfile
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Test

class CommunityRuleTest : FirestoreEmulatorTestBase() {
    private suspend fun denied(block: suspend () -> Unit) {
        try { block(); fail("Expected permission denied") }
        catch (e: FirebaseFirestoreException) { assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code) }
    }
    @Test fun publicationCommentsAndWithdrawalRespectOwnershipAndPrivacy() = runBlocking {
        val repo = CommunityRepository(context, db, auth)
        val alice = signInTestUser("community-alice@example.com")
        val id = repo.publish(alice, ReaderProfile(name = "Alice"), ComicEntry("hq", "", "Uma HQ", authors = "Autor", publisher = "Editora"), "Minha resenha", 4.5)
        val post = db.collection("communityReviews").document(id)
        assertEquals("Minha resenha", post.get(Source.SERVER).await().getString("body"))
        assertEquals(id, repo.reviews().items.single().id)
        val average = repo.rating(ComicEntry("hq", "", "Uma HQ", authors = "Autor", publisher = "Editora"))
        assertEquals(4.5, average.average, 0.001)
        assertEquals(1L, average.count)
        val bob = signInTestUser("community-bob@example.com")
        assertEquals("Alice", post.get(Source.SERVER).await().getString("author"))
        denied { post.update(mapOf("body" to "Adulterado", "updatedAt" to FieldValue.serverTimestamp())).await() }
        denied { db.collection("users").document(alice).get(Source.SERVER).await() }
        val comment = repo.comment(bob, id, ReaderProfile(name = "Bob"), "Gostei da indicação")
        assertEquals("Bob", comment.get(Source.SERVER).await().getString("author"))
        assertEquals(comment.id, repo.comments(id).items.single().id)
        denied { post.collection("comments").add(mapOf("ownerId" to alice, "author" to "Alice", "body" to "Falso", "createdAt" to FieldValue.serverTimestamp())).await() }
        auth.signOut()
        denied { post.get(Source.SERVER).await() }
        signInTestUser("community-alice@example.com")
        repo.withdraw(alice, id)
        signInTestUser("community-bob@example.com")
        denied { post.get(Source.SERVER).await() }
        denied { repo.comment(bob, id, ReaderProfile(name = "Bob"), "Depois de retirar") }
        repo.deleteComment(bob, id, comment.id)
        Unit
    }
    @Test fun invalidCommunityPayloadCannotBypassValidation() = runBlocking {
        val uid = signInTestUser("community-invalid@example.com")
        val repo = CommunityRepository(context, db, auth)
        val id = repo.publish(uid, ReaderProfile(name = "Alice"), ComicEntry("hq", "", "HQ", authors = "Autor", publisher = "Editora"), "Resenha", 5.0)
        val ref = db.collection("communityReviews").document(id)
        denied { ref.update(mapOf("rating" to 6.0, "updatedAt" to FieldValue.serverTimestamp())).await() }
        denied { ref.update(mapOf("body" to "x".repeat(5001), "updatedAt" to FieldValue.serverTimestamp())).await() }
        denied { ref.update(mapOf("ownerId" to "other", "updatedAt" to FieldValue.serverTimestamp())).await() }
        denied { ref.update(mapOf("admin" to true, "updatedAt" to FieldValue.serverTimestamp())).await() }
    }
    @Test fun communityCursorLoadsNextPageWithoutRepeatingReviews() = runBlocking {
        val uid = signInTestUser("community-pages@example.com")
        val repo = CommunityRepository(context, db, auth)
        repeat(21) { index -> repo.publish(uid, ReaderProfile(name = "Leitor"), ComicEntry("hq-$index", "", "HQ $index", authors = "Autor", publisher = "Editora"), "Resenha $index", 4.0) }
        val first = repo.reviews()
        assertEquals(20, first.items.size)
        assertTrue(first.hasMore)
        val second = repo.reviews(first.cursor)
        assertEquals(1, second.items.size)
        assertFalse(second.hasMore)
        assertTrue(first.items.none { it.id == second.items.single().id })
    }
}
