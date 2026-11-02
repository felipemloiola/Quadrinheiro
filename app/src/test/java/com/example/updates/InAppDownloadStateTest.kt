package com.example.updates

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InAppDownloadStateTest {
    @Test fun downloadIdentitySurvivesReloadAndCancellationClearsIt() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val update = AvailableUpdate(1039L, "1.0.39", UPDATE_DOWNLOAD_PREFIX + "v1.0.39/quadrinheiro.apk", "a".repeat(64))
        val record = InAppUpdateDownload.start(context, update)
        assertEquals(record, InAppUpdateDownload.load(context))
        val file = InAppUpdateDownload.privateApk(context, update)
        file.parentFile!!.mkdirs()
        file.writeText("fixture")
        InAppUpdateDownload.cancel(context, record)
        assertNull(InAppUpdateDownload.load(context))
        assertFalse(file.exists())
    }
}
