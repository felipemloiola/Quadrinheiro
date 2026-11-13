package com.example.reading

import android.app.Notification
import android.os.Build
import androidx.core.app.NotificationCompat

/** Use the public Android Live Updates contract, including on Samsung.
 * Early Android 16 required colorization; newer 16 QPRs and 17 forbid it.
 * Ask the installed framework instead of guessing an OEM/QPR from the SDK number.
 * No vendor flags, package allowlist impersonation, or private Samsung metadata.
 */
internal fun buildPromotableReadingNotification(builder: NotificationCompat.Builder): Notification {
    val standard = builder.setColorized(false).build()
    if (Build.VERSION.SDK_INT == 36 && !standard.hasPromotableCharacteristics()) {
        val legacy = builder.setColorized(true).build()
        if (legacy.hasPromotableCharacteristics()) return legacy
    }
    return standard
}
