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

/**
 * Registro em memória das URLs pertencentes à biblioteca e ao perfil do usuário ativo.
 * Apenas URLs registradas aqui têm permissão para gravar arquivos no DiskCache do Coil.
 * Todas as outras capas (Explorar, Busca, sequências não salvas, vitrines) são mantidas
 * apenas na memória RAM (CachePolicy.READ_ONLY no disco), impedindo o inchaço contínuo de dados.
 */
object LibraryDiskCacheRegistry {
    private val allowedUrls = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    fun registerUrls(urls: Collection<String>) {
        urls.forEach { raw ->
            val clean = raw.trim().replace("http://", "https://")
            if (clean.startsWith("https://")) {
                allowedUrls.add(clean)
            }
        }
    }

    fun unregisterUrl(raw: String) {
        val clean = raw.trim().replace("http://", "https://")
        allowedUrls.remove(clean)
    }

    fun clear() {
        allowedUrls.clear()
    }

    fun isAllowedForDiskWrite(rawUrl: String): Boolean {
        if (rawUrl.isBlank()) return false
        val clean = rawUrl.trim().replace("http://", "https://")
        return allowedUrls.contains(clean)
    }

    fun diskCachePolicyFor(rawUrl: String): coil.request.CachePolicy {
        return if (isAllowedForDiskWrite(rawUrl)) {
            coil.request.CachePolicy.ENABLED
        } else {
            coil.request.CachePolicy.READ_ONLY
        }
    }
}

object StorageMaintenance {
    const val RECOVERABLE_CACHE_BUDGET_BYTES = 24L * 1024 * 1024
    const val OFFLINE_DB_BUDGET_BYTES = 10L * 1024 * 1024
    private const val MAX_DIAGNOSTIC_ENTRIES = 20000
    private const val MAX_DIAGNOSTIC_DEPTH = 8
    private val uidSegmentRegex = Regex("(account_state_v1_|profile_showcase_|community_pending_|community_published_rev_|recs_v2_)([A-Za-z0-9_.-]+)")
    private val emailSegmentRegex = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")

    internal fun sanitizeDiagnosticPath(rawPath: String): String {
        return rawPath
            .replace(uidSegmentRegex, "$1<uid>")
            .replace(emailSegmentRegex, "<email>")
            .takeLast(220)
    }

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
        val seenCanonical = HashSet<String>()
        var visited = 0
        val stack = ArrayDeque<Pair<File, Int>>()
        roots.distinctBy { file -> runCatching { file.canonicalPath }.getOrElse { file.absolutePath } }.forEach { root ->
            if (root.exists()) stack.add(root to 0)
        }
        while (stack.isNotEmpty() && visited < MAX_DIAGNOSTIC_ENTRIES) {
            val (file, depth) = stack.removeLast()
            val canonicalPath = runCatching { file.canonicalPath }.getOrNull() ?: continue
            if (!seenCanonical.add(canonicalPath)) continue
            visited++
            if (file.isFile) {
                val size = runCatching { file.length() }.getOrDefault(0L)
                val smallestTopBytes = top.peek()?.bytes ?: Long.MAX_VALUE
                if (size > 0L && (top.size < limit || size > smallestTopBytes)) {
                    if (top.size == limit) top.poll()
                    top.add(StorageItem(sanitizeDiagnosticPath(canonicalPath), size))
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
        val codeCache = runCatching { context.codeCacheDir.canonicalFile }.getOrNull()
        val externalCache = context.externalCacheDir?.canonicalFile
        val external = context.getExternalFilesDir(null)?.canonicalFile
        val downloads = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.resolve("updates")?.canonicalFile
        val cacheInternalPath = cache.path + File.separator
        val codeCachePath = codeCache?.let { it.path + File.separator }
        val cacheExternalPath = externalCache?.let { it.path + File.separator }
        val downloadsPath = downloads?.let { it.path + File.separator }
        val externalPath = external?.let { it.path + File.separator }
        val cachePaths = listOfNotNull(cacheInternalPath, codeCachePath, cacheExternalPath, downloadsPath)
        val externalPaths = listOfNotNull(externalPath)
        fun inRoots(path: String, roots: List<String>) = roots.any { path == it.removeSuffix(File.separator) || path.startsWith(it) }
        val visitedLocalFiles = HashSet<String>()
        val dataBytes = data.walkTopDown().onEnter { file ->
            val path = runCatching { file.canonicalPath }.getOrNull() ?: return@onEnter false
            (path == data.path || path.startsWith(data.path + File.separator)) && !inRoots(path, cachePaths + externalPaths)
        }.filter { it.isFile && runCatching { visitedLocalFiles.add(it.canonicalPath) }.getOrDefault(false) }.sumOf { it.length() }
        
        fun safeSize(file: File?): Long = file?.let { runCatching { bytes(it) }.getOrDefault(0L) } ?: 0L
        fun under(root: File, relative: String) = safeSize(root.resolve(relative))

        val sharedPreferences = under(data, "shared_prefs")
        val databases = under(data, "databases")
        val files = under(data, "files")
        val noBackup = context.noBackupFilesDir.canonicalFile.takeIf {
            it != data && !it.path.startsWith(data.resolve("files").path + File.separator)
        }?.let(::safeSize) ?: 0L

        val codeCacheBytes = codeCache?.takeIf {
            it != data && it != cache && !it.path.startsWith(cache.path + File.separator)
        }?.let(::safeSize) ?: 0L
        val externalUpdates = safeSize(downloads)
        val externalBytes = (external?.let(::bytes) ?: 0L) - externalUpdates
        val cacheBytes = bytes(cache) + (externalCache?.takeIf { it != cache }?.let(::bytes) ?: 0L) + codeCacheBytes + externalUpdates
        val localBytes = (dataBytes + externalBytes).coerceAtLeast(0L)

        val cacheCovers = under(cache, "quadrinheiro_covers_disk_cache")
        val httpCache = under(cache, "quadrinheiro_http_cache")
        val snapshotsCache = under(cache, "quadrinheiro_snapshots")
        val sharedCards = under(cache, "shared_cards")
        val updateCache = under(cache, "updates")

        val categories = linkedMapOf(
            "Preferências e estado local" to sharedPreferences,
            "Bancos e sincronização offline" to databases,
            "Arquivos do app" to files,
            "Backups locais" to noBackup,
            "Cache do sistema (code_cache)" to codeCacheBytes,
            "Outros dados locais" to (dataBytes - sharedPreferences - databases - files - noBackup).coerceAtLeast(0L),
            "Capas em cache" to cacheCovers,
            "Respostas de rede" to httpCache,
            "Snapshots e traduções em cache" to snapshotsCache,
            "Cartões compartilhados" to sharedCards,
            "Atualizações em cache" to updateCache,
            "Outros caches" to (cacheBytes - cacheCovers - httpCache - snapshotsCache - sharedCards - updateCache - codeCacheBytes - externalUpdates).coerceAtLeast(0L),
            "Arquivos externos do app" to externalBytes.coerceAtLeast(0L),
            "Atualizações baixadas" to externalUpdates
        )
        val largest = largestFiles(listOfNotNull(data, cache, codeCache, externalCache, external, downloads))
        return StorageUsage(cacheBytes, localBytes, categories, largest)
    }

    /**
     * Auto-compacta instalações já inchadas no boot sem jamais tocar na biblioteca, contas,
     * modo convidado, vitrines, favoritos ou sincronizações pendentes.
     */
    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun autoCompactIfNeeded(context: Context): Boolean {
        val storage = SafeStorage(context)
        var compacted = storage.compactStorageIfNeeded()
        CacheMaintenance.prune(context)
        runCatching { android.database.sqlite.SQLiteDatabase.releaseMemory() }
        val cacheRoot = context.cacheDir
        val externalUpdateDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.resolve("updates")
        val currentCacheBytes = bytes(cacheRoot) + (externalUpdateDir?.let(::bytes) ?: 0L)
        if (currentCacheBytes > RECOVERABLE_CACHE_BUDGET_BYTES) {
            // Se ainda exceder o teto rígido de 80 MB, limpa cartões compartilhados e reduz caches recuperáveis
            runCatching { cacheRoot.resolve("shared_cards").deleteRecursively() }
            if (bytes(cacheRoot) > RECOVERABLE_CACHE_BUDGET_BYTES) {
                runCatching { MultiApiSearchEngine.sharedHttpClient.cache?.evictAll() }
            }
            if (bytes(cacheRoot) > RECOVERABLE_CACHE_BUDGET_BYTES) {
                runCatching { Coil.imageLoader(context).diskCache?.clear() }
            }
            compacted = true
            StorageWriteTelemetry.recordAutoCompaction()
        }
        return compacted
    }

    /**
     * Gera relatório técnico de diagnóstico 100% sanitizado (sem dados pessoais, sem conteúdo de arquivos,
     * com UIDs e e-mails mascarados) comparando a medição interna com o StorageStatsManager do Android.
     */
    fun buildSanitizedDiagnosticReport(
        context: Context,
        currentUsage: StorageUsage = usage(context)
    ): String {
        val sb = StringBuilder()
        val telemetry = StorageWriteTelemetry.snapshot()
        sb.appendLine("=== Diagnóstico de Armazenamento — Quadrinheiro ===")
        sb.appendLine("SDK Android: ${android.os.Build.VERSION.SDK_INT}")
        sb.appendLine("Tempo de sessão ativa: ${telemetry.sessionUptimeMs / 1000}s")
        sb.appendLine("Orçamento de caches recuperáveis: ${formatBytes(RECOVERABLE_CACHE_BUDGET_BYTES)}")
        sb.appendLine("Orçamento de banco offline: ${formatBytes(OFFLINE_DB_BUDGET_BYTES)}")
        sb.appendLine()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            runCatching {
                val ssm = context.getSystemService(android.app.usage.StorageStatsManager::class.java)
                val sm = context.getSystemService(android.os.storage.StorageManager::class.java)
                if (ssm != null && sm != null) {
                    val uuid = sm.getUuidForPath(context.filesDir)
                    val stats = ssm.queryStatsForPackage(uuid, context.packageName, android.os.Process.myUserHandle())
                    sb.appendLine("[Sistema Android - StorageStatsManager]")
                    sb.appendLine("Dados líquidos do app (data - cache): ${formatBytes((stats.dataBytes - stats.cacheBytes).coerceAtLeast(0L))}")
                    sb.appendLine("Cache segundo o Android (inclui code_cache): ${formatBytes(stats.cacheBytes)}")
                    sb.appendLine("APK e código compilado (appBytes): ${formatBytes(stats.appBytes)}")
                    sb.appendLine()
                }
            }
        }

        sb.appendLine("[Medição Interna Auditada]")
        sb.appendLine("Dados locais: ${formatBytes(currentUsage.localBytes)}")
        sb.appendLine("Cache recuperável: ${formatBytes(currentUsage.cacheBytes)}")
        sb.appendLine("Total medido: ${formatBytes(currentUsage.totalBytes)}")
        sb.appendLine()
        sb.appendLine("[Categorias]")
        currentUsage.categories.forEach { (label, size) ->
            sb.appendLine("- $label: ${formatBytes(size)} ($size B)")
        }
        sb.appendLine()
        sb.appendLine("[Telemetria de Escrita da Sessão]")
        sb.appendLine("- SharedPreferences escritas: ${telemetry.prefsWrites} (${formatBytes(telemetry.prefsBytesWritten)})")
        sb.appendLine("- Snapshots em cache escritas: ${telemetry.snapshotCacheWrites} (${formatBytes(telemetry.snapshotCacheBytesWritten)})")
        sb.appendLine("- Firestore Perfil escritas: ${telemetry.firestoreProfileWrites}")
        sb.appendLine("- Firestore HQs escritas: ${telemetry.firestoreComicWrites} (lotes: ${telemetry.firestoreComicBatchWrites})")
        sb.appendLine("- Comunidade publicações: ${telemetry.communityPublishWrites}")
        sb.appendLine("- Auto-compactações executadas: ${telemetry.autoCompactions}")
        if (telemetry.perKeyWrites.isNotEmpty()) {
            sb.appendLine("- Chaves mais gravadas: ${telemetry.perKeyWrites.entries.joinToString { "${it.key}=${it.value}" }}")
        }
        if (currentUsage.largestItems.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("[Maiores Arquivos (Caminhos Sanitizados)]")
            currentUsage.largestItems.take(15).forEach { item ->
                sb.appendLine("- ${formatBytes(item.bytes)} (${item.bytes} B): ${sanitizeDiagnosticPath(item.path)}")
            }
        }
        return sb.toString().trim()
    }

    /** SDK cache APIs preserve open stores and never touch the library or offline pending writes. */
    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun clearRecoverable(context: Context) {
        val loader = Coil.imageLoader(context)
        loader.memoryCache?.clear()
        loader.diskCache?.clear()
        MultiApiSearchEngine.sharedHttpClient.cache?.evictAll()
        val storage = SafeStorage(context)
        storage.removeItem(SafeStorage.TRANSLATION_CACHE_KEY)
        storage.removeItem(SafeStorage.MODERN_CATALOG_SNAPSHOT_KEY)
        storage.compactStorageIfNeeded()
        CacheMaintenance.prune(context, maxOrphans = 0, maxOrphanBytes = 0L)
    }

    fun formatBytes(bytes: Long): String = when {
        bytes >= 1024L * 1024 * 1024 -> String.format(java.util.Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024 * 1024))
        bytes >= 1024L * 1024 -> String.format(java.util.Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024))
        bytes >= 1024L -> "${bytes / 1024} KB"
        else -> "$bytes B"
    }
}
