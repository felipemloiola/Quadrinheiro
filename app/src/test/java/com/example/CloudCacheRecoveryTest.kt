package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.CloudCacheRecovery
import com.example.data.SafeStorage
import com.example.model.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class)
class CloudCacheRecoveryTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private val databaseName get()="firestore.test."+context.getString(R.string.firestore_database_id)
    @Before fun reset(){CloudCacheRecovery.resetForTests()}
    @After fun cleanup(){
        CloudCacheRecovery.databaseFiles(context).forEach{it.delete()}
        context.noBackupFilesDir.resolve("sync-recovery-backups").deleteRecursively()
        listOf("a","b").forEach{SafeStorage(context).removeItem(SafeStorage.ACCOUNT_STATE_PREFIX+it)}
        CloudCacheRecovery.resetForTests()
    }
    @Test fun budgetScalesWithoutOpeningTheSdk() {
        assertEquals(64L*1024*1024,CloudCacheRecovery.reviewThresholdBytes(64L*1024*1024))
        assertEquals(256L*1024*1024,CloudCacheRecovery.reviewThresholdBytes(2048L*1024*1024))
    }
    @Test fun originalsAndEveryAccountAreBackedUpBeforeAnyReset()=runBlocking {
        val storage=SafeStorage(context)
        for(uid in listOf("a","b"))storage.saveState(listOf(ComicEntry(uid,"","Book $uid",authors="Author",publisher="Publisher",isDirty=false)),ReaderProfile(name=uid,isDirty=false),AppSettings(),uid)
        val db=context.getDatabasePath(databaseName).apply{parentFile!!.mkdirs();writeText("pending SDK operations")}
        val auth=context.getDatabasePath("unrelated-auth").apply{writeText("keep")}
        try {
            val backup=CloudCacheRecovery.repair(context) {
                val folder=context.noBackupFilesDir.resolve("sync-recovery-backups").listFiles()!!.single{it.isDirectory}
                assertEquals(db.readText(),folder.resolve(db.name).readText())
                assertTrue(folder.resolve("local-state.json").readText().contains("Book a"))
                assertTrue(folder.resolve("backup-complete").isFile)
                db.delete()
            }
            assertTrue(backup.resolve(databaseName).isFile)
            assertEquals("keep",auth.readText())
            for(uid in listOf("a","b")) {
                val restored=storage.loadAccountState(uid)!!
                assertEquals("Book $uid",restored.comics.single().title)
                assertTrue(restored.comics.single().isDirty);assertTrue(restored.profile.isDirty)
            }
            assertFalse(CloudCacheRecovery.needsReview(context))
        } finally {auth.delete()}
    }
    @Test fun failedResetPreservesTheOriginalAndTheVerifiedBackup()=runBlocking {
        val db=context.getDatabasePath(databaseName).apply{parentFile!!.mkdirs();writeText("original")}
        val result=runCatching{CloudCacheRecovery.repair(context){error("not allowed")}}
        assertTrue(result.isFailure);assertEquals("original",db.readText())
        CloudCacheRecovery.resetForTests()
        assertTrue(CloudCacheRecovery.needsReview(context))
        val folder=context.noBackupFilesDir.resolve("sync-recovery-backups").listFiles()!!.single{it.isDirectory}
        assertEquals("original",folder.resolve(databaseName).readText())
    }
    @Test fun interruptedRecoveryResumesFromTheVerifiedBackupWithoutResettingAgain()=runBlocking {
        SafeStorage(context).saveState(listOf(ComicEntry("a","","Keep this",authors="Author",publisher="Publisher",isDirty=false)),ReaderProfile(name="a",isDirty=false),AppSettings(),"a")
        val db=context.getDatabasePath(databaseName).apply{parentFile!!.mkdirs();writeText("original")}
        assertTrue(runCatching{CloudCacheRecovery.repair(context){db.delete();error("process interrupted")}}.isFailure)
        CloudCacheRecovery.resetForTests()
        assertTrue(CloudCacheRecovery.needsReview(context))
        CloudCacheRecovery.repair(context){fail("A completed reset must not run again")}
        val state=SafeStorage(context).loadAccountState("a")!!
        assertEquals("Keep this",state.comics.single().title)
        assertTrue(state.comics.single().isDirty)
        assertFalse(CloudCacheRecovery.needsReview(context))
    }
}
