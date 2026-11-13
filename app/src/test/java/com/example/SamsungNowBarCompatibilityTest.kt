package com.example

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.example.reading.samsung.SamsungNowBarCompatibility
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SamsungNowBarCompatibilityTest {
    private fun notification(ongoing: Boolean = true): Notification {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return NotificationCompat.Builder(context, "test").setSmallIcon(R.drawable.ic_reading_notification)
            .setContentTitle("HQ de teste").setContentText("Pág. 24 de 120 · 20%")
            .setShortCriticalText("20%").setRequestPromotedOngoing(true).setOngoing(ongoing).build()
    }

    @Test fun samsungExtensionPreservesStandardContentAndPackageIdentity() {
        val notification = notification()
        val originalFlags = notification.flags
        val result = SamsungNowBarCompatibility.applyTo(notification, "Samsung", 37)
        // AOSP has no semFlags field: optional reflection must degrade gracefully.
        assertEquals(SamsungNowBarCompatibility.Result.EXTRAS_ONLY, result)
        assertEquals("HQ de teste", notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("20%", notification.extras.getString("android.ongoingActivityNoti.primaryInfo"))
        assertEquals("Pág. 24 de 120 · 20%", notification.extras.getString("android.ongoingActivityNoti.nowbarSecondaryInfo"))
        assertEquals(originalFlags, notification.flags)
        assertNull(notification.contentView)
    }

    @Test fun otherManufacturersAndOlderAndroidAreUntouched() {
        listOf("Google" to 37, "samsung" to 24, "samsung" to 34).forEach { (brand, sdk) ->
            val notification = notification()
            assertEquals(SamsungNowBarCompatibility.Result.NOT_APPLICABLE, SamsungNowBarCompatibility.applyTo(notification, brand, sdk))
            assertFalse(notification.extras.containsKey("android.ongoingActivityNoti.style"))
        }
    }

    @Test fun ordinaryNotificationsNeverBecomeSamsungLiveActivities() {
        val notification = notification(ongoing = false)
        assertEquals(SamsungNowBarCompatibility.Result.NOT_APPLICABLE, SamsungNowBarCompatibility.applyTo(notification, "samsung", 37))
    }
}
