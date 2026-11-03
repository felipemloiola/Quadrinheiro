package com.example.data

import android.content.Context
import coil.Coil
import java.io.File

data class StorageUsage(val cacheBytes: Long, val localBytes: Long)

object StorageMaintenance {
    internal fun bytes(root: File): Long {
        if (!root.exists()) return 0L
        val canonical = root.canonicalFile
        val prefix = canonical.path + File.separator
        val visited = HashSet<String>()
        return root.walkTopDown().onEnter { file ->
            val path = runCatching { file.canonicalPath }.getOrNull()
            path != null && (path == canonical.path || path.startsWith(prefix)) && visited.add(path)
        }.filter { it.isFile && runCatching { it.canonicalPath.startsWith(prefix) }.getOrDefault(false) }.sumOf { it.length() }
    }
    fun usage(context: Context): StorageUsage {
        val data = File(context.applicationInfo.dataDir)
        return StorageUsage(bytes(context.cacheDir) + (context.externalCacheDir?.let(::bytes) ?: 0L),
            bytes(context.filesDir) + bytes(File(data, "shared_prefs")) + bytes(File(data, "databases")) + bytes(context.noBackupFilesDir) + (context.getExternalFilesDir(null)?.let(::bytes) ?: 0L))
    }
    /** SDK cache APIs preserve open stores and never touch the library or offline pending writes. */
    fun clearRecoverable(context: Context) {
        val loader = Coil.imageLoader(context)
        loader.memoryCache?.clear()
        loader.diskCache?.clear()
        MultiApiSearchEngine.sharedHttpClient.cache?.evictAll()
        SafeStorage(context).removeItem(SafeStorage.TRANSLATION_CACHE_KEY)
        CacheMaintenance.prune(context)
    }
    fun formatBytes(bytes: Long): String = if (bytes < 1024 * 1024) "${bytes / 1024} KB"
        else String.format(java.util.Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
}
