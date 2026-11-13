package com.example.reading.samsung

import android.app.Notification
import android.os.Build

/** Diagnostic-only Samsung extension; never compiled into the production APK.
 * Field names/legacy bit were investigated using MukulBolt9/xz (Apache-2.0).
 * Never changes package identity, requests overlays, or disables hidden-API enforcement.
 */
internal object SamsungNowBarCompatibility {
    const val LEGACY_ONGOING_FLAG = 0x8000
    enum class Result { NOT_APPLICABLE, EXTRAS_ONLY, LEGACY_FLAG }

    fun applyTo(
        notification: Notification,
        manufacturer: String = Build.MANUFACTURER,
        sdk: Int = Build.VERSION.SDK_INT
    ): Result {
        if (!manufacturer.equals("samsung", ignoreCase = true) || sdk < 35 ||
            notification.flags and Notification.FLAG_ONGOING_EVENT == 0) return Result.NOT_APPLICABLE
        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        if (title.isBlank()) return Result.NOT_APPLICABLE
        val progress = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val compact = extras.getString("android.shortCriticalText").orEmpty().ifBlank { "Lendo" }
        extras.putInt("android.ongoingActivityNoti.style", 1)
        extras.putString("android.ongoingActivityNoti.primaryInfo", compact)
        extras.putString("android.ongoingActivityNoti.secondaryInfo", progress)
        extras.putString("android.ongoingActivityNoti.nowbarPrimaryInfo", title)
        extras.putString("android.ongoingActivityNoti.nowbarSecondaryInfo", progress)
        extras.putString("android.ongoingActivityNoti.chipExpandedText", progress)
        return try {
            val field = Notification::class.java.getDeclaredField("semFlags")
            field.isAccessible = true
            field.setInt(notification, field.getInt(notification) or LEGACY_ONGOING_FLAG)
            Result.LEGACY_FLAG
        } catch (_: ReflectiveOperationException) {
            Result.EXTRAS_ONLY
        } catch (_: SecurityException) {
            Result.EXTRAS_ONLY
        } catch (_: RuntimeException) {
            Result.EXTRAS_ONLY
        }
    }
}
