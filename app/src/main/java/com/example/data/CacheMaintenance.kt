package com.example.data

import android.content.Context
import android.os.Environment
import com.example.BuildConfig
import com.example.updates.InAppUpdateDownload
import java.io.File
import kotlinx.coroutines.*

/** Only app-owned, recoverable files. Never traverses collection/profile storage. */
object CacheMaintenance {
    const val SHARED_CARDS_BUDGET_BYTES = 4L * 1024 * 1024
    const val MAX_SHARED_CARD_FILES = 3
    private const val SHARED_CARDS_TTL_MS = 6L * 60 * 60 * 1000
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun schedule(context: Context) {
        val appCtx = context.applicationContext
        scope.launch {
            runCatching { migrateLegacyImageCaches(appCtx) }
            runCatching { StorageMaintenance.autoCompactIfNeeded(appCtx) }
        }
    }

    @Synchronized
    internal fun migrateLegacyImageCaches(context: Context) {
        val storage = SafeStorage(context)
        val migrations = listOf(
            "quadrinheiro_legacy_http_cache_purged_v1" to "quadrinheiro_http_cache",
            "quadrinheiro_heif_cache_purged_v1" to "quadrinheiro_covers_disk_cache"
        )
        for ((flag, directory) in migrations) {
            if (storage.getItem(flag) == "true") continue
            val target = context.cacheDir.resolve(directory)
            if (!target.exists() || target.deleteRecursively()) storage.setItem(flag, "true")
        }
    }

    internal fun prune(
        context: Context,
        now: Long = System.currentTimeMillis(),
        maxOrphans: Int = 1,
        maxOrphanBytes: Long = 32L * 1024 * 1024
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
        val storage = SafeStorage(context)
        storage.compactStorageIfNeeded()
        storage.trimTranslationCache()
        runCatching { androidx.work.WorkManager.getInstance(context).pruneWork() }
    }

    internal fun pruneSharedCards(context: Context, now: Long = System.currentTimeMillis()) {
        val root = context.cacheDir.resolve("shared_cards")
        var keptBytes = 0L
        var keptCount = 0
        root.listFiles().orEmpty()
            .filter { it.isFile && it.extension == "png" && it.canonicalFile.parentFile == root.canonicalFile }
            .sortedByDescending { it.lastModified() }.forEach { file ->
                val size = file.length()
                if (keptCount >= MAX_SHARED_CARD_FILES ||
                    size > SHARED_CARDS_BUDGET_BYTES - keptBytes ||
                    now - file.lastModified() > SHARED_CARDS_TTL_MS
                ) {
                    file.delete()
                } else {
                    keptCount++
                    keptBytes += size
                }
            }
    }
}
