package com.example.data

import java.io.File

/** Only recognizes updater-owned files; never traverses collection or backup storage. */
internal object RecoverableFilePolicy {
    private const val DAY = 24L * 60 * 60 * 1000
    private val apkName = Regex("quadrinheiro-(\\d+)\\.apk")

    fun pruneUpdates(
        roots: List<File>, installedCode: Long, activeCode: Long?, now: Long,
        maxOrphans: Int = 2, maxOrphanBytes: Long = 64L * 1024 * 1024
    ) {
        require(maxOrphans >= 0 && maxOrphanBytes >= 0)
        val files = roots.distinctBy { it.canonicalPath }.flatMap { root ->
            root.listFiles().orEmpty().filter { file ->
                file.isFile && file.canonicalFile.parentFile == root.canonicalFile && apkName.matches(file.name)
            }
        }.sortedWith(compareByDescending<File> { it.lastModified() }.thenBy { it.absolutePath })
        var retained = 0
        var retainedBytes = 0L
        for (file in files) {
            val code = apkName.matchEntire(file.name)?.groupValues?.get(1)?.toLongOrNull() ?: continue
            if (code == activeCode && code > installedCode) continue
            val expired = now - file.lastModified() > DAY
            val size = file.length()
            if (code <= installedCode || expired || retained >= maxOrphans || size > maxOrphanBytes - retainedBytes) {
                file.delete()
            } else {
                retained++
                retainedBytes += size
            }
        }
    }
}
