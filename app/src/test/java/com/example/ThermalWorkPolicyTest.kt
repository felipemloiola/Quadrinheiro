package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.CacheMaintenance
import com.example.data.ImageMemoryPolicy
import com.example.data.SafeStorage
import com.example.data.StorageMaintenance
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThermalWorkPolicyTest {
    @Test fun decodedImageRetentionFitsSmallHeapsAndHasAnAbsoluteCeiling() {
        val mib=1024L*1024
        assertEquals((128*mib/10).toInt(), ImageMemoryPolicy.budgetBytes(128*mib,false))
        assertEquals((12*mib).toInt(), ImageMemoryPolicy.budgetBytes(512*mib,true))
        assertEquals((32*mib).toInt(), ImageMemoryPolicy.budgetBytes(4096*mib,false))
        assertTrue(ImageMemoryPolicy.budgetBytes(32*mib,true)<=32*mib/10)
    }

    @Test fun legacyCacheMigrationRunsOnceAndNeverTouchesAccountData() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val storage=SafeStorage(context)
        val flag="quadrinheiro_heif_cache_purged_v1"
        storage.removeItem(flag)
        val covers=context.cacheDir.resolve("quadrinheiro_covers_disk_cache").apply{mkdirs()}
        covers.resolve("old").writeText("legacy")
        val account=context.filesDir.resolve("thermal-account-sentinel").apply{writeText("preserve")}
        try {
            CacheMaintenance.migrateLegacyImageCaches(context)
            assertFalse(covers.exists())
            covers.mkdirs();covers.resolve("new").writeText("decoded after migration")
            CacheMaintenance.migrateLegacyImageCaches(context)
            assertTrue(covers.resolve("new").exists())
            assertEquals("preserve",account.readText())
        } finally { covers.deleteRecursively();account.delete();storage.removeItem(flag) }
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    @Test fun largeSnapshotsDoNotTriggerCoverEviction() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val snapshots=context.cacheDir.resolve("quadrinheiro_snapshots").apply{mkdirs()}
        val file=snapshots.resolve("thermal-sentinel.json")
        java.io.RandomAccessFile(file,"rw").use{it.setLength(StorageMaintenance.RECOVERABLE_CACHE_BUDGET_BYTES+1)}
        val disk=checkNotNull(coil.Coil.imageLoader(context).diskCache)
        val key="thermal-retain-cover"
        checkNotNull(disk.openEditor(key)).apply {
            data.toFile().writeText("decoded-cover-payload")
            metadata.toFile().writeText("test")
            commit()
        }
        try {
            StorageMaintenance.autoCompactIfNeeded(context)
            assertTrue(file.exists())
            val cached=disk.openSnapshot(key)
            assertNotNull("A large unrelated snapshot must not evict cached covers",cached)
            cached?.close()
        } finally {file.delete();disk.remove(key)}
    }
}
