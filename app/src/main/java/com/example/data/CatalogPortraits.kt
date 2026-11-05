package com.example.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull

/** Small URL-only cache. Concurrent rows share requests for the same provider identity. */
internal object CatalogPortraits {
    private data class Entry(val image: String, val savedAt: Long)
    private val lock = Any()
    private val permits = Semaphore(3)
    private val memory = object : LinkedHashMap<String, Entry>(96, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>?) = size > 96
    }
    private val pending = HashMap<String, CompletableDeferred<String>>()
    internal fun validResource(type: String, id: String): Boolean =
        type in setOf("character", "person") && id.matches(Regex("(?:mt-)?[0-9]{1,12}"))

    fun cached(type: String, id: String): String? = synchronized(lock) {
        val key = "$type:$id"
        val entry = memory[key] ?: return@synchronized null
        val age = System.currentTimeMillis() - entry.savedAt
        val ttl = if (entry.image.isBlank()) 60_000L else 6L * 60 * 60 * 1000
        if (age in 0 until ttl) entry.image else { memory.remove(key); null }
    }

    suspend fun image(type: String, id: String): String {
        if (!validResource(type, id)) return ""
        cached(type, id)?.let { return it }
        val key = "$type:$id"
        val (result, owner) = synchronized(lock) {
            pending[key]?.let { it to false } ?: run {
                if (pending.size >= 96) return ""
                CompletableDeferred<String>().also { pending[key] = it } to true
            }
        }
        if (!owner) return result.await()
        try {
            val url = permits.withPermit {
                cached(type, id) ?: withTimeoutOrNull(4000L) { ComicVineCatalog.details(type, id).image }.orEmpty()
            }.takeIf { it.startsWith("https://") && it.length <= 2000 }.orEmpty()
            synchronized(lock) { memory[key] = Entry(url, System.currentTimeMillis()) }
            result.complete(url)
            return url
        } catch (cancel: CancellationException) {
            result.cancel(cancel)
            throw cancel
        } catch (_: Exception) {
            synchronized(lock) { memory[key] = Entry("", System.currentTimeMillis()) }
            result.complete("")
            return ""
        } finally {
            synchronized(lock) { if (pending[key] === result) pending.remove(key) }
        }
    }
}
