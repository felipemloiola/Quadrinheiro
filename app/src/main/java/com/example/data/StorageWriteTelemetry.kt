package com.example.data

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Telemetria leve em memória (zero escrita em disco) para auditar frequência e volume
 * de gravações por produtor durante a sessão e nos testes automatizados de armazenamento.
 */
object StorageWriteTelemetry {
    private val sessionStartMs = AtomicLong(System.currentTimeMillis())
    private val prefsWritesTotal = AtomicLong(0L)
    private val prefsBytesWrittenTotal = AtomicLong(0L)
    private val snapshotCacheWritesTotal = AtomicLong(0L)
    private val snapshotCacheBytesWrittenTotal = AtomicLong(0L)
    private val firestoreProfileWritesTotal = AtomicLong(0L)
    private val firestoreComicWritesTotal = AtomicLong(0L)
    private val firestoreComicBatchWritesTotal = AtomicLong(0L)
    private val communityPublishWritesTotal = AtomicLong(0L)
    private val autoCompactionsCount = AtomicLong(0L)
    private val writesByKey = ConcurrentHashMap<String, AtomicLong>()

    data class TelemetrySnapshot(
        val sessionUptimeMs: Long,
        val prefsWrites: Long,
        val prefsBytesWritten: Long,
        val snapshotCacheWrites: Long,
        val snapshotCacheBytesWritten: Long,
        val firestoreProfileWrites: Long,
        val firestoreComicWrites: Long,
        val firestoreComicBatchWrites: Long,
        val communityPublishWrites: Long,
        val autoCompactions: Long,
        val perKeyWrites: Map<String, Long>
    )

    fun recordPrefsWrite(key: String, bytesCount: Int) {
        prefsWritesTotal.incrementAndGet()
        prefsBytesWrittenTotal.addAndGet(bytesCount.toLong().coerceAtLeast(0L))
        val cleanKey = sanitizeKeyLabel(key)
        writesByKey.getOrPut(cleanKey) { AtomicLong(0L) }.incrementAndGet()
    }

    fun recordSnapshotCacheWrite(key: String, bytesCount: Int) {
        snapshotCacheWritesTotal.incrementAndGet()
        snapshotCacheBytesWrittenTotal.addAndGet(bytesCount.toLong().coerceAtLeast(0L))
        val cleanKey = "cache:" + sanitizeKeyLabel(key)
        writesByKey.getOrPut(cleanKey) { AtomicLong(0L) }.incrementAndGet()
    }

    fun recordFirestoreProfileWrite() {
        firestoreProfileWritesTotal.incrementAndGet()
    }

    fun recordFirestoreComicWrite(count: Int = 1) {
        firestoreComicWritesTotal.addAndGet(count.toLong().coerceAtLeast(1L))
    }

    fun recordFirestoreComicBatchWrite(batchSize: Int) {
        firestoreComicBatchWritesTotal.incrementAndGet()
        firestoreComicWritesTotal.addAndGet(batchSize.toLong().coerceAtLeast(0L))
    }

    fun recordCommunityPublishWrite() {
        communityPublishWritesTotal.incrementAndGet()
    }

    fun recordAutoCompaction() {
        autoCompactionsCount.incrementAndGet()
    }

    fun snapshot(nowMs: Long = System.currentTimeMillis()): TelemetrySnapshot {
        val perKey = writesByKey.entries
            .associate { it.key to it.value.get() }
            .toList()
            .sortedByDescending { it.second }
            .take(16)
            .toMap()
        return TelemetrySnapshot(
            sessionUptimeMs = (nowMs - sessionStartMs.get()).coerceAtLeast(0L),
            prefsWrites = prefsWritesTotal.get(),
            prefsBytesWritten = prefsBytesWrittenTotal.get(),
            snapshotCacheWrites = snapshotCacheWritesTotal.get(),
            snapshotCacheBytesWritten = snapshotCacheBytesWrittenTotal.get(),
            firestoreProfileWrites = firestoreProfileWritesTotal.get(),
            firestoreComicWrites = firestoreComicWritesTotal.get(),
            firestoreComicBatchWrites = firestoreComicBatchWritesTotal.get(),
            communityPublishWrites = communityPublishWritesTotal.get(),
            autoCompactions = autoCompactionsCount.get(),
            perKeyWrites = perKey
        )
    }

    fun resetForTesting(nowMs: Long = System.currentTimeMillis()) {
        sessionStartMs.set(nowMs)
        prefsWritesTotal.set(0L)
        prefsBytesWrittenTotal.set(0L)
        snapshotCacheWritesTotal.set(0L)
        snapshotCacheBytesWrittenTotal.set(0L)
        firestoreProfileWritesTotal.set(0L)
        firestoreComicWritesTotal.set(0L)
        firestoreComicBatchWritesTotal.set(0L)
        communityPublishWritesTotal.set(0L)
        autoCompactionsCount.set(0L)
        writesByKey.clear()
    }

    internal fun sanitizeKeyLabel(rawKey: String): String {
        return when {
            rawKey.startsWith(SafeStorage.ACCOUNT_STATE_PREFIX) -> "account_state_<uid>"
            rawKey.startsWith("profile_showcase_") -> "profile_showcase_<uid>"
            rawKey.startsWith("community_pending_") -> "community_pending_<uid>"
            rawKey.startsWith("community_published_rev_") -> "community_published_rev_<uid>"
            rawKey.startsWith("community_comments_") -> "community_comments_<post>"
            else -> rawKey.take(64)
        }
    }
}
