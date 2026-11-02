package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SafeStorage
import com.example.model.AppSettings
import com.example.model.ComicEntry
import com.example.model.ReaderProfile
import com.example.model.UserProfile
import com.example.widget.QuadrinheiroWidgetsSync
import com.example.widget.UpdatePageAction
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetSessionTest {
    private lateinit var context: Context
    private lateinit var storage: SafeStorage
    private fun user(uid: String) = UserProfile(uid, uid, null, null, false, "email")
    private fun comic(title: String) = ComicEntry("shared-id", "", title, authors = "", publisher = "", currentPage = 10)

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        storage = SafeStorage(context)
        storage.clearAuthSession()
    }

    @Test fun switchAndLogoutNeverExposeThePreviousAccountSnapshot() {
        storage.saveAuthSession(user("widget-account-a"))
        storage.saveState(listOf(comic("Conta A")), ReaderProfile(), AppSettings(), "widget-account-a")
        assertEquals("Conta A", storage.loadWidgetState().comics.single().title)
        storage.saveAuthSession(user("widget-account-b"))
        storage.saveState(listOf(comic("Conta B")), ReaderProfile(), AppSettings(), "widget-account-b")
        val snapshot = storage.loadWidgetSessionSnapshot()
        assertEquals("widget-account-b", snapshot.ownerUid)
        assertEquals("Conta B", snapshot.state.comics.single().title)
        storage.clearAuthSession()
        assertTrue(storage.loadWidgetState().comics.isEmpty())
        assertEquals("Conta A", storage.loadAccountState("widget-account-a")!!.comics.single().title)
    }

    @Test fun staleWidgetActionCannotEditTheNewAccountEvenWithTheSameComicId() = runBlocking {
        storage.saveAuthSession(user("widget-account-b"))
        storage.saveState(listOf(comic("Conta B")), ReaderProfile(), AppSettings(), "widget-account-b")
        assertNull(UpdatePageAction.applyPageDelta(context, "shared-id", 1, "widget-account-a"))
        assertEquals(10, storage.loadWidgetState().comics.single().currentPage)
        storage.clearAuthSession()
        assertNull(UpdatePageAction.applyPageDelta(context, "shared-id", 1, "widget-account-b"))
    }

    @Test fun revisionsAreUniqueEvenForConsecutiveUpdatesInTheSameMillisecond() {
        val first = QuadrinheiroWidgetsSync.bumpInMemoryRevision()
        val next = QuadrinheiroWidgetsSync.bumpInMemoryRevision()
        assertTrue(next > first)
    }
}
