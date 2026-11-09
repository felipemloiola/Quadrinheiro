package com.example.data

/** Retain decoded covers across navigation without competing with the UI's heap. */
internal object ImageMemoryPolicy {
    private const val MIB = 1024L * 1024L
    fun budgetBytes(maxHeapBytes: Long, lowRam: Boolean): Int =
        (maxHeapBytes.coerceAtLeast(0L) / 10L)
            .coerceIn(1L, (if (lowRam) 12L else 32L) * MIB).toInt()
}
