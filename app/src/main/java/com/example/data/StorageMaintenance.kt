package com.example.data

import android.content.Context
import android.os.Environment
import coil.Coil
import java.io.File

data class StorageUsage(
    val cacheBytes: Long,
    val localBytes: Long,
    val categories: Map<String, Long> = emptyMap()
) {
    val totalBytes: Long get() = cacheBytes + localBytes
}

object StorageMaintenance {
    internal fun bytes(root: File): Long {
        if (!root.exists()) return 0L
        if (root.isFile) return root.length()
        val canonical = root.canonicalFile
        val prefix = canonical.path + File.separator
        val visited = HashSet<String>()
        return root.walkTopDown().onEnter { file ->
            val path = runCatching { file.canonicalPath }.getOrNull()
            path != null && (path == canonical.path || path.startsWith(prefix)) && visited.add(path)
        }.filter { it.isFile && runCatching { it.canonicalPath.startsWith(prefix) }.getOrDefault(false) }
            .sumOf { it.length() }
    }

    /** Metadata-only measurement. It does not read or export account contents. */
    fun usage(context: Context): StorageUsage {
        val data = File(context.applicationInfo.dataDir)
        val internalCache = bytes(context.cacheDir)
        val externalCache = context.externalCacheDir?.let(::bytes) ?: 0L
        val externalFiles = context.getExternalFilesDir(null)?.let(::bytes) ?: 0L
        val cacheTotal = internalCache + externalCache
        val localTotal = (bytes(data) - internalCache).coerceAtLeast(0L) + externalFiles
        val covers = bytes(context.cacheDir.resolve("quadrinheiro_covers_disk_cache"))
        val http = bytes(context.cacheDir.resolve("quadrinheiro_http_cache"))
        val cards = bytes(context.cacheDir.resolve("shared_cards"))
        val cachedUpdates = bytes(context.cacheDir.resolve("updates"))
        val downloadedUpdates = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?.resolve("updates")?.let(::bytes) ?: 0L
        val preferences = bytes(File(data, "shared_prefs"))
        val databases = bytes(File(data, "databases"))
        val files = bytes(context.filesDir)
        val backups = bytes(context.noBackupFilesDir)
        val categories = linkedMapOf(
            "Capas em cache" to covers,
            "Respostas de rede" to http,
            "Cartões compartilhados" to cards,
            "Atualizações baixadas" to cachedUpdates + downloadedUpdates,
            "Outros caches" to (cacheTotal - covers - http - cards - cachedUpdates).coerceAtLeast(0L),
            "Preferências e estado local" to preferences,
            "Bancos e sincronização offline" to databases,
            "Arquivos e backups" to files + backups + (externalFiles - downloadedUpdates).coerceAtLeast(0L),
            "Outros dados locais" to (localTotal - preferences - databases - files - backups - externalFiles).coerceAtLeast(0L)
        )
        return StorageUsage(cacheTotal, localTotal, categories)
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

    fun formatBytes(bytes: Long): String = when {
        bytes >= 1024L * 1024 * 1024 -> String.format(java.util.Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024 * 1024))
        bytes >= 1024L * 1024 -> String.format(java.util.Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024))
        bytes >= 1024L -> "${bytes / 1024} KB"
        else -> "$bytes B"
    }
}
