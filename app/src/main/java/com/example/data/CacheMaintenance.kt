package com.example.data

import android.content.Context
import android.os.Environment
import com.example.BuildConfig
import com.example.updates.InAppUpdateDownload
import java.io.File
import kotlinx.coroutines.*

/** Only app-owned, recoverable files. Never traverses collection/profile storage. */
object CacheMaintenance {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    fun schedule(context: Context) { scope.launch { runCatching { prune(context.applicationContext) } } }

    internal fun prune(
        context: Context,
        now: Long = System.currentTimeMillis(),
        maxOrphans: Int = 2,
        maxOrphanBytes: Long = 64L * 1024 * 1024
    ) {
        val active = InAppUpdateDownload.load(context)
        if (active != null && active.update.code <= BuildConfig.VERSION_CODE) InAppUpdateDownload.cancel(context, active)
        val activeCode = active?.update?.code?.takeIf { it > BuildConfig.VERSION_CODE }
        RecoverableFilePolicy.pruneUpdates(
            roots = listOfNotNull(
                File(context.cacheDir, "updates"),
                File(context.filesDir, "updates"),
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.resolve("updates")
            ),
            installedCode = BuildConfig.VERSION_CODE.toLong(),
            activeCode = activeCode,
            now = now,
            maxOrphans = maxOrphans,
            maxOrphanBytes = maxOrphanBytes
        )
        pruneSharedCards(context, now)
        SafeStorage(context).trimTranslationCache()
    }

    internal fun pruneSharedCards(context: Context, now: Long = System.currentTimeMillis()) {
        val root = context.cacheDir.resolve("shared_cards")
        var keptBytes = 0L
        var keptCount = 0
        root.listFiles().orEmpty()
            .filter { it.isFile && it.extension == "png" && it.canonicalFile.parentFile == root.canonicalFile }
            .sortedByDescending { it.lastModified() }.forEach { file ->
                val size = file.length()
                if (keptCount >= 8 || size > 32L * 1024 * 1024 - keptBytes || now - file.lastModified() > 3L * 24 * 60 * 60 * 1000) {
                    file.delete()
                } else {
                    keptCount++
                    keptBytes += size
                }
            }
    }
}
