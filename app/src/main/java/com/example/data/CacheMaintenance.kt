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

    internal fun prune(context: Context, now: Long = System.currentTimeMillis()) {
        val active = InAppUpdateDownload.load(context)
        if (active != null && active.update.code <= BuildConfig.VERSION_CODE) InAppUpdateDownload.cancel(context, active)
        val activeCode = active?.update?.code?.takeIf { it > BuildConfig.VERSION_CODE }
        val apkName = Regex("quadrinheiro-(\\d+)\\.apk")
        val roots = listOfNotNull(File(context.cacheDir, "updates"), context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.resolve("updates"))
        roots.forEach { root ->
            root.listFiles().orEmpty().filter { it.isFile && it.canonicalFile.parentFile == root.canonicalFile }.forEach fileLoop@ { file ->
                val code = apkName.matchEntire(file.name)?.groupValues?.get(1)?.toLongOrNull() ?: return@fileLoop
                if (code <= BuildConfig.VERSION_CODE || (code != activeCode && now - file.lastModified() > 7L * 24 * 60 * 60 * 1000)) file.delete()
            }
        }
        pruneSharedCards(context, now)
        SafeStorage(context).trimTranslationCache()
    }

    internal fun pruneSharedCards(context: Context, now: Long = System.currentTimeMillis()) {
        val root = context.cacheDir.resolve("shared_cards")
        var keptBytes = 0L
        root.listFiles().orEmpty().filter { it.isFile && it.extension == "png" && it.canonicalFile.parentFile == root.canonicalFile }
            .sortedByDescending { it.lastModified() }.forEachIndexed { index, file ->
                keptBytes += file.length()
                if (index >= 8 || keptBytes > 32L * 1024 * 1024 || now - file.lastModified() > 3L * 24 * 60 * 60 * 1000) file.delete()
            }
    }
}
