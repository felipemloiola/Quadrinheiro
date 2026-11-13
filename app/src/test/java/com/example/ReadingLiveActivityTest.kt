package com.example

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SafeStorage
import com.example.model.*
import com.example.reading.ReadingLiveActivity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReadingLiveActivityTest {
    private lateinit var context: Context
    private lateinit var storage: SafeStorage
    private val comics = listOf("first", "second").map {
        ComicEntry(it, "", "Fixture $it", authors = "Autor", publisher = "", pageCount = 100, currentPage = 20, status = ReadingStatus.READING)
    }
    private fun save(owner: String, selected: String = "first", enabled: Boolean = true) {
        storage.saveAuthSession(UserProfile(owner, owner, null, null, false, "email"))
        storage.saveState(comics, ReaderProfile(activeReadingComicId = selected), AppSettings(showReadingNowBar = enabled), owner)
    }
    private fun notification(): Notification? = shadowOf(context.getSystemService(NotificationManager::class.java)).getNotification(ReadingLiveActivity.ID)

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        storage = SafeStorage(context)
        ReadingLiveActivity.stop(context)
        shadowOf(context as android.app.Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        save("live-owner-a")
    }
    @After fun cleanup() { ReadingLiveActivity.stop(context); storage.clearAuthSession() }

    @Test fun explicitSessionUsesStandardProgressTemplateAndHasStopAction() = runBlocking {
        ReadingLiveActivity.start(context, "first")
        val posted = notification()!!
        assertEquals("Fixture first", posted.extras.getString(Notification.EXTRA_TITLE))
        assertEquals(20, posted.extras.getInt(Notification.EXTRA_PROGRESS))
        assertEquals(100, posted.extras.getInt(Notification.EXTRA_PROGRESS_MAX))
        assertTrue(posted.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertNull(posted.contentView)
        assertEquals(listOf("− 1 pág.", "+ 1 pág.", "Encerrar"), posted.actions.map { it.title.toString() })
        ReadingLiveActivity.handle(context, "live-owner-a", "first", 0)
        assertNull(notification())
        assertFalse(ReadingLiveActivity.isActive(context))
    }
    @Test fun sessionTracksSelectedComicAndCancelsOnAccountSwitch() = runBlocking {
        ReadingLiveActivity.start(context, "first")
        save("live-owner-a", "second")
        ReadingLiveActivity.refresh(context)
        assertEquals("Fixture second", notification()!!.extras.getString(Notification.EXTRA_TITLE))
        save("live-owner-b", enabled = false)
        ReadingLiveActivity.handle(context, "live-owner-a", "second", 1)
        assertEquals(20, storage.loadWidgetState().comics.last().currentPage)
        ReadingLiveActivity.refresh(context)
        assertNull(notification())
        assertFalse(ReadingLiveActivity.isActive(context))
    }
    @Test fun aSessionNeverStartsForAnAbsentOrFinishedComic() = runBlocking {
        ReadingLiveActivity.start(context, "missing")
        assertNull(notification())
        storage.saveState(comics.map { it.copy(status = ReadingStatus.READ) }, ReaderProfile(), AppSettings(showReadingNowBar = true), "live-owner-a")
        ReadingLiveActivity.start(context, "first")
        assertNull(notification())
    }
    @Test fun localGuestCanStartAReadingSessionWithoutCloudAuthentication() = runBlocking {
        storage.clearAuthSession()
        storage.setGuestMode(true)
        storage.saveState(comics, ReaderProfile(activeReadingComicId = "first"), AppSettings(showReadingNowBar = true), "guest_local")
        ReadingLiveActivity.start(context, "first")
        assertEquals("Fixture first", notification()!!.extras.getString(Notification.EXTRA_TITLE))
        storage.clearAuthSession()
        ReadingLiveActivity.refresh(context)
        assertNull(notification())
    }
    @Test fun preferenceStartsAutomaticallyAndDisablingRemovesTheLockScreenNotification() = runBlocking {
        save("live-owner-a", enabled = false)
        ReadingLiveActivity.start(context, "first")
        ReadingLiveActivity.refresh(context)
        assertNull(notification())
        save("live-owner-a", enabled = true)
        ReadingLiveActivity.refresh(context)
        assertEquals(Notification.VISIBILITY_PUBLIC, notification()!!.visibility)
        save("live-owner-a", enabled = false)
        ReadingLiveActivity.refresh(context)
        assertNull(notification())
        val preferences = com.example.data.CloudUserPreferences.fromAppSettings(AppSettings(showReadingNowBar = true))
        assertTrue(preferences.toAppSettings().showReadingNowBar)
        assertFalse(com.example.data.CloudUserPreferences().toAppSettings().showReadingNowBar)
    }
    @Test fun previousComicActionsNeverRetargetOrEndTheNewSelection() = runBlocking {
        ReadingLiveActivity.refresh(context)
        val original = notification()!!.actions.first().actionIntent
        save("live-owner-a", "second")
        ReadingLiveActivity.refresh(context)
        val current = notification()!!.actions.first().actionIntent
        assertNotEquals(original, current)
        assertEquals("first", shadowOf(original).savedIntent.getStringExtra("comic"))
        ReadingLiveActivity.handle(context, "live-owner-a", "first", 0)
        assertEquals("Fixture second", notification()!!.extras.getString(Notification.EXTRA_TITLE))
    }
    @Test fun dismissingKeepsTheSameComicClosedUntilPreferenceIsToggled() = runBlocking {
        ReadingLiveActivity.refresh(context)
        ReadingLiveActivity.handle(context, "live-owner-a", "first", 0)
        ReadingLiveActivity.refresh(context)
        assertNull(notification())
        save("live-owner-a", enabled = false)
        ReadingLiveActivity.refresh(context)
        save("live-owner-a", enabled = true)
        ReadingLiveActivity.refresh(context)
        assertNotNull(notification())
    }

    @Test fun blockedChannelDoesNotReportAnActiveSession() = runBlocking {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(android.app.NotificationChannel(ReadingLiveActivity.CHANNEL, "Leitura", NotificationManager.IMPORTANCE_NONE))
        ReadingLiveActivity.start(context, "first")
        assertNull(notification())
        assertFalse(ReadingLiveActivity.active.value)
        assertFalse(ReadingLiveActivity.isActive(context))
    }

    @Test fun missingPageCountUsesIndeterminateProgressInsteadOfInventingATotal() = runBlocking {
        storage.saveState(listOf(comics.first().copy(pageCount = 0, currentPage = 12)), ReaderProfile(activeReadingComicId = "first"),
            AppSettings(showReadingNowBar = true), "live-owner-a")
        ReadingLiveActivity.start(context, "first")
        val posted = notification()!!
        assertTrue(posted.extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE))
        assertEquals("Pág. 12 · Total não informado", posted.extras.getString(Notification.EXTRA_TEXT))
    }

    @Test fun progressPercentageDoesNotOverflowForLargePageCounts() {
        assertEquals("Pág. 50000000 de 100000000 · 50%", com.example.reading.readingProgressText(comics.first().copy(pageCount = 100_000_000, currentPage = 50_000_000)))
    }

    @Test @Config(sdk = [24]) fun androidSevenUsesAppDetailsForNotificationSettings() {
        val intent = com.example.reading.readingNotificationSettingsIntent(context)
        assertEquals(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
        assertEquals("package:${context.packageName}", intent.dataString)
    }

    @Test @Config(sdk = [36]) fun androidSixteenUsesNativePromotableProgressStyle() = runBlocking {
        org.robolectric.util.ReflectionHelpers.setStaticField(android.os.Build::class.java, "MANUFACTURER", "samsung")
        ReadingLiveActivity.start(context, "first")
        val posted = notification()!!
        assertEquals("android.app.Notification\$ProgressStyle", posted.extras.getString(Notification.EXTRA_TEMPLATE))
        assertTrue("Promotion requested", posted.extras.getBoolean("android.requestPromotedOngoing"))
        assertTrue(posted.hasPromotableCharacteristics())
        assertNull(posted.contentView)
        assertFalse("Samsung uses public Live Updates, not undocumented vendor extras",
            posted.extras.keySet().any { it.startsWith("android.ongoingActivityNoti.") })
    }
}
