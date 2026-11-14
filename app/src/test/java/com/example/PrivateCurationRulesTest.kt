package com.example

import com.example.data.*
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Test

class PrivateCurationRulesTest : FirestoreEmulatorTestBase() {
    private fun envelope(uid: String, revision: Long=1, deleted: Boolean=false, payload: String="{}") =
        mapOf("ownerId" to uid,"revision" to revision,"deleted" to deleted,"payload" to payload,"updatedAt" to FieldValue.serverTimestamp())
    @Test fun ownerCanSaveQueryAndUpdateButCannotReplayResurrectOrBypassSchema() = runBlocking {
        val uid=signInTestUser(); val path="users/$uid/lists/12345678-1234-1234-1234-123456789abc";val ref=db.document(path)
        ref.set(envelope(uid)).await()
        assertEquals(1,db.collection("users/$uid/lists").limit(101).get().await().size())
        assertTrue(runCatching {ref.set(envelope(uid)).await()}.isFailure)
        assertTrue(runCatching {ref.set(envelope("other",2)).await()}.isFailure)
        assertTrue(runCatching {ref.set(envelope(uid,2,payload="x".repeat(262145))).await()}.isFailure)
        assertTrue(runCatching {ref.set(envelope(uid,2)+("admin" to true)).await()}.isFailure)
        assertTrue(runCatching {ref.set(envelope(uid,2)-"payload").await()}.isFailure)
        assertTrue(runCatching {ref.set(envelope(uid,2)+("deleted" to "false")).await()}.isFailure)
        ref.set(envelope(uid,2,deleted=true)).await()
        assertTrue(runCatching {ref.set(envelope(uid,3)).await()}.isFailure)
        assertTrue(runCatching {ref.delete().await()}.isFailure)
    }
    @Test fun privateListsAndFeedbackRejectOtherUsersAndUnauthenticatedQueries() = runBlocking {
        val alice=signInTestUser(); val ref=db.document("users/$alice/lists/12345678-1234-1234-1234-123456789abc")
        ref.set(envelope(alice)).await()
        val pref=db.document("users/$alice/recommendationPreferences/state");pref.set(envelope(alice)).await()
        assertTrue(runCatching {db.document("users/$alice/recommendationPreferences/other").set(envelope(alice)).await()}.isFailure)
        auth.signOut();val bob=signInTestUser("bob@example.com")
        assertTrue(runCatching {ref.get().await()}.isFailure)
        assertTrue(runCatching {pref.get().await()}.isFailure)
        assertTrue(runCatching {ref.set(envelope(bob,2)).await()}.isFailure)
        assertTrue(runCatching {db.collection("users/$alice/lists").get().await()}.isFailure)
        assertTrue(runCatching {db.document("users/$bob/lists/12345678-1234-1234-1234-123456789abc").set(envelope(alice)).await()}.isFailure)
        auth.signOut()
        assertTrue(runCatching {ref.get().await()}.isFailure)
        assertTrue(runCatching {db.collection("users/$alice/lists").get().await()}.isFailure)
    }
    @Test fun repositoryRevisionsProtectConcurrentEditsAndTombstones() = runBlocking {
        val uid=signInTestUser();val repo=CurationCloudRepository(context,db,auth)
        val list=ComicList(name="Privada")
        repo.saveList(uid,list)
        assertEquals(list.name,repo.load(uid).lists.single().name)
        assertTrue(runCatching {repo.saveList(uid,list.copy(name="Edição antiga"))}.isFailure)
        repo.saveList(uid,list.copy(revision=1,deleted=true))
        assertTrue(repo.load(uid).lists.isEmpty())
        assertEquals(true,db.document("users/$uid/lists/${list.id}").get().await().getBoolean("deleted"))
        assertTrue(runCatching {repo.saveList(uid,list.copy(revision=2))}.isFailure)
    }
}
