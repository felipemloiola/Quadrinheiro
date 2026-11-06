package com.example.data

import java.io.File

/** Only recognizes updater-owned files; never traverses collection or backup storage. */
internal object RecoverableFilePolicy {
    private const val DAY = 24L * 60 * 60 * 1000
    private val apkName = Regex("quadrinheiro-(\\d+)(?:-(\\d+))?\\.apk(?:\\.tmp|\\.part)?")

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
        var activeRetained = false
        for (file in files) {
            val match = apkName.matchEntire(file.name) ?: continue
            val code = match.groupValues[1].toLongOrNull() ?: continue
            val isCollisionOrTemp = match.groupValues[2].isNotEmpty() ||
                file.name.endsWith(".tmp") ||
                file.name.endsWith(".part")
            if (code == activeCode && code > installedCode && !isCollisionOrTemp && !activeRetained) {
                activeRetained = true
                continue
            }
            val expired = now - file.lastModified() > DAY
            val size = file.length()
            if (isCollisionOrTemp || code <= installedCode || expired || retained >= maxOrphans || size > maxOrphanBytes - retainedBytes) {
                file.delete()
            } else {
                retained++
                retainedBytes += size
            }
        }
    }
}
