package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.coroutineContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/** A large SDK queue must never be discarded silently or opened before the user can review it. */
internal object CloudCacheRecovery {
    @Volatile private var allowedForProcess=false
    private val recoveryMutex=Mutex()
    internal fun reviewThresholdBytes(heap: Long): Long =
        (heap/2).coerceIn(64L*1024*1024,256L*1024*1024)

    internal fun databaseFiles(context: Context): List<File> {
        val root=context.getDatabasePath("unused").parentFile ?: return emptyList()
        val databaseSuffix="."+context.getString(R.string.firestore_database_id)
        return root.listFiles().orEmpty().filter {
            it.isFile && it.name.startsWith("firestore.") &&
                listOf("","-wal","-shm","-journal").any{suffix->it.name.endsWith(databaseSuffix+suffix)} &&
                it.canonicalFile.parentFile==root.canonicalFile
        }
    }

    private fun pendingFile(context: Context)=context.noBackupFilesDir.resolve("sync-recovery-backups/pending")
    fun needsReview(context: Context): Boolean = !allowedForProcess &&
        (pendingFile(context).exists() || databaseFiles(context).sumOf {it.length()} > reviewThresholdBytes(Runtime.getRuntime().maxMemory()))

    fun continueWithoutRepair() { allowedForProcess=true }
    internal fun resetForTests() {allowedForProcess=false}

    suspend fun repair(context: Context): File = repair(context) {
        // This is deliberately the first Firestore operation in the recovery boot.
        // Repositories and background workers are gated until review completes.
        FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id)).clearPersistence().await()
    }

    internal suspend fun repair(context: Context, clearPersistence: suspend () -> Unit): File = recoveryMutex.withLock { withContext(Dispatchers.IO) {
        val originals=databaseFiles(context)
        val parent=context.noBackupFilesDir.resolve("sync-recovery-backups").apply{mkdirs()}
        val previous=pendingFile(context).takeIf{it.isFile}?.readText()?.takeIf{it.matches(Regex("[0-9]+"))}?.let{parent.resolve(it)}
        val backup=if(originals.isEmpty()) {
            checkNotNull(previous?.takeIf{it.resolve("backup-complete").isFile}) {"No verified recovery backup"}
        } else {
            check(parent.usableSpace > originals.sumOf{it.length()}+2L*1024*1024) {"Insufficient space for backup"}
            val target=previous?.takeIf{!it.resolve("backup-complete").exists()}
                ?: parent.resolve(System.currentTimeMillis().toString()).apply{check(mkdir())}
            writeDurable(pendingFile(context),target.name.toByteArray())
            val preferences=context.getSharedPreferences(SafeStorage.STORAGE_KEY,Context.MODE_PRIVATE)
            writeDurable(target.resolve("local-state.json"),JSONObject(preferences.all).toString().toByteArray())
            for(original in originals) {
                val copy=target.resolve(original.name)
                FileOutputStream(copy).use { output -> original.inputStream().use{it.copyTo(output)};output.fd.sync() }
                check(original.length()==copy.length() && digest(original).contentEquals(digest(copy))) {"Backup verification failed"}
            }
            writeDurable(target.resolve("backup-complete"),JSONObject().put("files",org.json.JSONArray(originals.map{it.name})).toString().toByteArray())
            target
        }
        // No cache reset occurs until every original file and the local account state are backed up.
        coroutineContext.ensureActive()
        if(originals.isNotEmpty())clearPersistence()
        val storage=SafeStorage(context)
        val preferences=context.getSharedPreferences(SafeStorage.STORAGE_KEY,Context.MODE_PRIVATE)
        for(key in preferences.all.keys.toList()) {
            if(!key.startsWith(SafeStorage.ACCOUNT_STATE_PREFIX))continue
            val uid=key.removePrefix(SafeStorage.ACCOUNT_STATE_PREFIX)
            val state=storage.loadAccountState(uid) ?: continue
            storage.saveState(state.comics.map{it.copy(isDirty=true)},state.profile.copy(isDirty=true),state.settings,uid)
        }
        check(preferences.edit().putBoolean("sync_recovery_local_state_preserved",true).commit()) {"Could not persist recovery state"}
        check(!pendingFile(context).exists() || pendingFile(context).delete()) {"Could not finish recovery record"}
        allowedForProcess=true
        backup
    } }

    private fun writeDurable(file: File, bytes: ByteArray) = FileOutputStream(file).use {it.write(bytes);it.fd.sync()}
    private fun digest(file: File): ByteArray {
        val digest=MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input -> val buffer=ByteArray(64*1024);while(true){val n=input.read(buffer);if(n<0)break;digest.update(buffer,0,n)} }
        return digest.digest()
    }
}
