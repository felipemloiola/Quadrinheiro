package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SafeStorage
import com.example.model.AppSettings
import com.example.model.ComicEntry
import com.example.model.ReaderProfile
import com.example.model.UserProfile
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AccountPersistenceRegressionTest {
    private lateinit var storage: SafeStorage
    private val alice = "hardening_test_alice"
    private val bob = "hardening_test_bob"
    private val settings = AppSettings()

    @Before
    fun setup() {
        storage = SafeStorage(ApplicationProvider.getApplicationContext<Context>())
        clearTestState()
    }

    @After
    fun cleanup() = clearTestState()

    private fun clearTestState() {
        storage.clearAuthSession()
        storage.removeItem(SafeStorage.STORAGE_KEY)
        storage.removeItem(SafeStorage.GUEST_STATE_KEY)
        storage.removeItem(SafeStorage.ACCOUNT_STATE_PREFIX + alice)
        storage.removeItem(SafeStorage.ACCOUNT_STATE_PREFIX + bob)
    }

    private fun login(uid: String) {
        storage.saveAuthSession(UserProfile(uid = uid, displayName = uid, email = null,
            photoUrl = null, isGuest = false, provider = "email"))
    }

    private fun comic(title: String) = ComicEntry(id = "shared-id", externalId = "shared-external-id", title = title,
        authors = "Author", publisher = "Publisher", updatedAt = 100L, isDirty = true)

    @Test
    fun saveWithoutSessionPersistsInsteadOfSilentlyDroppingData() {
        storage.saveState(listOf(comic("Local")), ReaderProfile(name = "Local"), settings)
        assertEquals("Local", storage.loadState().comics.single().title)
    }

    @Test
    fun switchingAccountsKeepsTheirLibrariesSeparate() {
        login(alice)
        storage.saveState(listOf(comic("Alice")), ReaderProfile(name = "Alice"), settings)
        login(bob)
        assertTrue(storage.loadState().comics.isEmpty())
        storage.saveState(listOf(comic("Bob")), ReaderProfile(name = "Bob"), settings)
        login(alice)
        assertEquals("Alice", storage.loadState().comics.single().title)
        login(bob)
        assertEquals("Bob", storage.loadState().comics.single().title)
    }

    @Test
    fun lateSaveForAliceDoesNotReplaceBobsSharedMirror() {
        login(bob)
        storage.saveState(listOf(comic("Bob")), ReaderProfile(name = "Bob"), settings)
        storage.saveState(listOf(comic("Alice late")), ReaderProfile(name = "Alice"), settings, alice)
        val mirror = JSONObject(storage.getItem()!!)
        assertEquals("Bob", mirror.getJSONObject("profile").getString("name"))
        assertEquals("Alice late", storage.loadAccountState(alice)!!.comics.single().title)
        assertEquals("Bob", storage.loadState().comics.single().title)
    }

    @Test
    fun syncAcknowledgementForAliceDoesNotClearBobsDirtyFlags() {
        login(alice)
        storage.saveState(listOf(comic("Alice")), ReaderProfile(name = "Alice", isDirty = true), settings)
        login(bob)
        storage.saveState(listOf(comic("Bob")), ReaderProfile(name = "Bob", isDirty = true), settings)
        storage.markComicsSynced(setOf("shared-id"), 100L, ownerUid = alice)
        storage.markProfileSynced(100L, ownerUid = alice)
        assertFalse(storage.loadAccountState(alice)!!.comics.single().isDirty)
        assertFalse(storage.loadAccountState(alice)!!.profile.isDirty)
        assertTrue(storage.loadAccountState(bob)!!.comics.single().isDirty)
        assertTrue(storage.loadAccountState(bob)!!.profile.isDirty)
        val mirror = JSONObject(storage.getItem()!!)
        assertTrue(mirror.getJSONArray("comics").getJSONObject(0).getBoolean("isDirty"))
        assertTrue(mirror.getJSONObject("profile").getBoolean("isDirty"))
    }

    @Test
    fun guestLibraryRemainsSeparateFromAccounts() {
        login(alice)
        storage.saveState(listOf(comic("Alice")), ReaderProfile(name = "Alice"), settings)
        storage.setGuestMode(true)
        storage.saveState(listOf(comic("Guest")), ReaderProfile(name = "Convidado"), settings)
        assertEquals("Guest", storage.loadGuestState().comics.single().title)
        assertEquals("Alice", storage.loadAccountState(alice)!!.comics.single().title)
    }
}
