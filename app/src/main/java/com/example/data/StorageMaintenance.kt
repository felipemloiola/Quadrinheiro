package com.example.data

import android.content.Context
import android.os.Environment
import coil.Coil
import java.io.File

data class StorageItem(val path: String, val bytes: Long)
data class StorageUsage(
    val cacheBytes: Long,
    val localBytes: Long,
    val categories: Map<String, Long> = emptyMap(),
    val largestItems: List<StorageItem> = emptyList()
) {
    val totalBytes: Long get() = cacheBytes + localBytes
}

object StorageMaintenance {
    private const val MAX_DIAGNOSTIC_ENTRIES = 20000
    private const val MAX_DIAGNOSTIC_DEPTH = 8

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

    private fun largestFiles(roots: List<File>, limit: Int = 20): List<StorageItem> {
        val top = java.util.PriorityQueue<StorageItem>(compareBy { it.bytes })
        var visited = 0
        val stack = ArrayDeque<Pair<File, Int>>()
        roots.forEach { root -> if (root.exists()) stack.add(root to 0) }
        while (stack.isNotEmpty() && visited < MAX_DIAGNOSTIC_ENTRIES) {
            val (file, depth) = stack.removeLast()
            visited++
            if (file.isFile) {
                val size = runCatching { file.length() }.getOrDefault(0L)
                if (size > 0L && (top.size < limit || size > top.peek().bytes)) {
                    if (top.size == limit) top.poll()
                    top.add(StorageItem(runCatching { file.canonicalPath }.getOrElse { file.name }.takeLast(220), size))
                }
            } else if (file.isDirectory && depth < MAX_DIAGNOSTIC_DEPTH) {
                runCatching { file.listFiles().orEmpty() }.getOrDefault(emptyArray()).forEach { child ->
                    val safe = runCatching { child.canonicalFile.toPath().startsWith(file.canonicalFile.toPath()) }.getOrDefault(false)
                    if (safe) stack.add(child to depth + 1)
                }
            }
        }
        return top.toList().sortedByDescending { it.bytes }
    }

    /** Measures file paths and sizes only. File contents are never read, uploaded, or deleted here. */
    fun usage(context: Context): StorageUsage {
        val data = File(context.applicationInfo.dataDir).canonicalFile
        val cache = context.cacheDir.canonicalFile
        val externalCache = context.externalCacheDir?.canonicalFile
        val external = context.getExternalFilesDir(null)?.canonicalFile
        val cacheInternalPath = cache.path + File.separator
        val cacheExternalPath = externalCache?.let { it.path + File.separator }
        val externalPath = external?.let { it.path + File.separator }
        val cachePaths = listOfNotNull(cacheInternalPath, cacheExternalPath)
        val externalPaths = listOfNotNull(externalPath)
        fun inRoots(path: String, roots: List<String>) = roots.any { path == it.removeSuffix(File.separator) || path.startsWith(it) }
        val dataBytes = data.walkTopDown().onEnter { file ->
            val path = runCatching { file.canonicalPath }.getOrNull() ?: return@onEnter false
            (path == data.path || path.startsWith(data.path + File.separator)) && !inRoots(path, cachePaths + externalPaths)
        }.filter { it.isFile }.sumOf { it.length() }
        val externalBytes = external?.let(::bytes) ?: 0L
        val cacheBytes = bytes(cache) + (externalCache?.let(::bytes) ?: 0L)
        val localBytes = dataBytes + externalBytes
        fun safeSize(file: File?): Long = file?.let { runCatching { bytes(it) }.getOrDefault(0L) } ?: 0L
        fun under(root: File, relative: String) = safeSize(root.resolve(relative))
        val downloads = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.resolve("updates")
        val sharedPreferences = under(data, "shared_prefs")
        val databases = under(data, "databases")
        val files = under(data, "files")
        val noBackup = context.noBackupFilesDir.canonicalFile.takeIf { it != data }?.let(::safeSize) ?: 0L
        val cacheCovers = under(cache, "quadrinheiro_covers_disk_cache")
        val httpCache = under(cache, "quadrinheiro_http_cache")
        val sharedCards = under(cache, "shared_cards")
        val updateCache = under(cache, "updates")
        val externalUpdates = safeSize(downloads)
        val categories = linkedMapOf(
            "Preferências e estado local" to sharedPreferences,
            "Bancos e sincronização offline" to databases,
            "Arquivos do app" to files,
            "Backups locais" to noBackup,
            "Outros dados locais" to (dataBytes - sharedPreferences - databases - files - noBackup).coerceAtLeast(0L),
            "Capas em cache" to cacheCovers,
            "Respostas de rede" to httpCache,
            "Cartões compartilhados" to sharedCards,
            "Atualizações em cache" to updateCache,
            "Outros caches" to (cacheBytes - cacheCovers - httpCache - sharedCards - updateCache).coerceAtLeast(0L),
            "Arquivos externos do app" to (externalBytes - externalUpdates).coerceAtLeast(0L),
            "Atualizações baixadas" to externalUpdates
        )
        val largest = largestFiles(listOf(data, cache) + listOfNotNull(external))
        return StorageUsage(cacheBytes, localBytes, categories, largest)
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
