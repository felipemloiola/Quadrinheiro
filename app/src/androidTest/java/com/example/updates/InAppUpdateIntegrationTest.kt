package com.example.updates

import android.app.DownloadManager
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import com.example.BuildConfig

/** Uses an immutable, already-published signed APK. Never installs over the test app. */
@RunWith(AndroidJUnit4::class)
class InAppUpdateIntegrationTest {
    @Test fun downloadResumesFromSavedRecordVerifiesRealApkAndBuildsNativeInstallIntent() = runBlocking {
        assumeTrue(BuildConfig.DEBUG)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val update = AvailableUpdate(
            1038L, "1.0.38", UPDATE_DOWNLOAD_PREFIX + "v1.0.38/quadrinheiro.apk",
            "69fb9f637cc677e5ecbb58459e48d05f86f1348c6efed71edca3777fc044612b"
        )
        val record = InAppUpdateDownload.start(context, update)
        try {
            val restored = InAppUpdateDownload.load(context)
            assertEquals(record, restored)
            withTimeout(150_000L) {
                while (InAppUpdateDownload.progress(context, restored!!).status != DownloadManager.STATUS_SUCCESSFUL) delay(500)
            }
            val apk = InAppUpdateDownload.prepare(context, record)
            assertTrue(apk.length() > 1_000_000)
            val install = InAppUpdateDownload.installIntent(context, apk)
            assertEquals(Intent.ACTION_VIEW, install.action)
            assertEquals("content", install.data!!.scheme)
            assertEquals(APK_MIME_TYPE, install.type)
            assertTrue(install.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
            context.contentResolver.openInputStream(install.data!!).use { input ->
                assertEquals('P'.code, input!!.read())
                assertEquals('K'.code, input.read())
            }
        } finally {
            InAppUpdateDownload.cancel(context, record)
        }
        assertNull(InAppUpdateDownload.load(context))
        assertFalse(InAppUpdateDownload.privateApk(context, update).exists())
    }
}
