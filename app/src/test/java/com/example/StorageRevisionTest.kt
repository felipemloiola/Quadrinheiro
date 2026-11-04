package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.RecoverableFilePolicy
import com.example.data.StorageMaintenance
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StorageRevisionTest {
    @get:Rule val temporary = TemporaryFolder()
    private val day = 86400000L
    private val now = 10 * day

    private fun apk(root: File, code: Long, age: Long = 0L, size: Int = 3): File =
        File(root, "quadrinheiro-$code.apk").apply {
            writeBytes(ByteArray(size))
            check(setLastModified(now - age))
        }

    @Test fun installedAndExpiredFilesAreRemovedButActiveAndBackupsRemain() {
        val root = temporary.newFolder("updates")
        val installed = apk(root, 100)
        val expired = apk(root, 101, age = 2 * day)
        val active = apk(root, 105, age = 3 * day)
        val recent = apk(root, 102)
        val backup = File(root, "backup.zip").apply { writeText("keep") }
        val nested = File(root, "personal").apply { mkdirs() }
        val nestedApk = apk(nested, 99)
        RecoverableFilePolicy.pruneUpdates(listOf(root), 100, 105, now)
        assertFalse(installed.exists())
        assertFalse(expired.exists())
        assertTrue(active.exists())
        assertTrue(recent.exists())
        assertTrue(backup.exists())
        assertTrue(nestedApk.exists())
    }

    @Test fun orphanCountIsGlobalAcrossBothDownloadLocations() {
        val cached = temporary.newFolder("cache")
        val downloaded = temporary.newFolder("downloads")
        val oldest = apk(cached, 101, age = 3000)
        val second = apk(downloaded, 102, age = 2000)
        val newest = apk(cached, 103, age = 1000)
        RecoverableFilePolicy.pruneUpdates(listOf(cached, downloaded, cached), 100, null, now)
        assertFalse(oldest.exists())
        assertTrue(second.exists())
        assertTrue(newest.exists())
    }

    @Test fun rejectedLargeFileDoesNotConsumeTheRemainingBudget() {
        val root = temporary.newFolder("budget")
        val oversized = apk(root, 101, age = 0, size = 65)
        val small = apk(root, 102, age = 1000, size = 16)
        RecoverableFilePolicy.pruneUpdates(listOf(root), 100, null, now, maxOrphanBytes = 64)
        assertFalse(oversized.exists())
        assertTrue(small.exists())
    }

    @Test fun activeUpdateIsNotEvictedByOrphanCountOrSizeLimits() {
        val root = temporary.newFolder("active")
        val active = apk(root, 110, size = 100)
        val orphan = apk(root, 109)
        RecoverableFilePolicy.pruneUpdates(listOf(root), 100, 110, now, maxOrphans = 0, maxOrphanBytes = 0)
        assertTrue(active.exists())
        assertFalse(orphan.exists())
    }

    @Test fun measurementCountsRegularFilesAndIncludesPreferencesAndDatabases() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val privateFile = File(context.filesDir, "storage-revision-fixture").apply { writeBytes(ByteArray(1024)) }
        try {
            assertEquals(1024L, StorageMaintenance.bytes(privateFile))
            val usage = StorageMaintenance.usage(context)
            assertTrue(usage.localBytes >= 1024L)
            assertTrue(usage.categories.getValue("Arquivos e backups") >= 1024L)
            assertTrue(usage.categories.containsKey("Preferências e estado local"))
            assertTrue(usage.categories.containsKey("Bancos e sincronização offline"))
            assertEquals(usage.cacheBytes + usage.localBytes, usage.totalBytes)
            assertTrue(usage.categories.values.all { it >= 0L })
        } finally { privateFile.delete() }
    }
}
