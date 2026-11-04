package com.example.data

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject
import java.io.IOException

internal object FavoritePortraits {
    private val cvId = Regex("cv-([1-9][0-9]{0,11})")
    private val scales = Regex("""/(?:scale_[^/]+|screen_[^/]+|original)/""")
    private val hosts = setOf("comicvine.gamespot.com", "www.comicvine.com", "comicvine1.cbsistatic.com", "static.comicvine.com")
    private val concurrencySemaphore = Semaphore(2)
    private val inflightMutex = Mutex()
    private val inflight = mutableMapOf<String, Deferred<List<String>>>()

    private data class CacheEntry(val items: List<String>, val savedAt: Long)
    private val cache = object : LinkedHashMap<String, CacheEntry>(24, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>?) = size > 24
    }

    private val canonicalIds = mapOf(
        "batman (bruce wayne)" to "1699",
        "superman (clark kent)" to "1807",
        "mulher-maravilha (diana prince)" to "2048",
        "mulher maravilha (diana prince)" to "2048",
        "homem-aranha (peter parker)" to "1443",
        "homem aranha (peter parker)" to "1443",
        "homem-aranha (miles morales)" to "79021",
        "homem aranha (miles morales)" to "79021",
        "lanterna verde (hal jordan)" to "11202",
        "lanterna verde (john stewart)" to "11205",
        "lanterna verde (kyle rayner)" to "40439",
        "lanterna verde (guy gardner)" to "10506",
        "flash (barry allen)" to "22804",
        "flash (wally west)" to "23879",
        "demolidor (matt murdock)" to "24694",
        "wolverine (logan)" to "1440",
        "asa noturna (dick grayson)" to "1678",
        "capuz vermelho (jason todd)" to "7035",
        "robin (tim drake)" to "9290",
        "robin (damian wayne)" to "48509",
        "mulher-gato (selina kyle)" to "1698",
        "mulher gato (selina kyle)" to "1698",
        "coringa" to "1702",
        "arlequina (harleen quinzel)" to "1696",
        "aquaman (arthur curry)" to "2357",
        " arqueiro verde (oliver queen)" to "5936",
        "arqueiro verde (oliver queen)" to "5936",
        "constantine (john constantine)" to "3329",
        "john constantine" to "3329",
        "monstro do pantano (alec holland)" to "6959",
        "monstro do pantano" to "6959",
        "sandman (morpheus)" to "2385",
        "deadpool (wade wilson)" to "7606",
        " justiceiro (frank castle)" to "1525",
        "justiceiro (frank castle)" to "1525",
        "homem de ferro (tony stark)" to "1455",
        "capitao america (steve rogers)" to "1442",
        "thor (odinson)" to "2268",
        "hulk (bruce banner)" to "2267",
        "viuva negra (natasha romanoff)" to "3200",
        " Magneto (erik lehnsherr)" to "1441",
        "magneto (erik lehnsherr)" to "1441",
        "ciclope (scott summers)" to "1459",
        "tempestade (ororo munroe)" to "1444",
        "jean grey" to "3552",
        "noturno (kurt wagner)" to "1461",
        "vampira (anna marie)" to "1446",
        "gambit (remy lebeau)" to "1475",
        "invencivel (mark grayson)" to "41127"
    )

    private fun normalized(text: String) = CatalogSearchPolicy.normalize(text)
    private fun snapshot(raw: String): JSONObject? = runCatching { JSONObject(raw) }.getOrNull()
    internal fun linkedId(raw: String): String? = snapshot(raw)?.optString("id")?.let { cvId.matchEntire(it)?.groupValues?.get(1) }

    internal fun exactId(display: String, original: String, candidates: List<CharacterResult>): String? {
        fun id(c: CharacterResult) = cvId.matchEntire(c.id)?.groupValues?.get(1)
        val valid = candidates.filter { id(it) != null }.distinctBy { it.id }
        val full = normalized(display)
        val exact = valid.filter { full.isNotBlank() && normalized(it.name) == full }
        if (exact.size == 1) return id(exact.single())
        val bases = listOf(display.substringBefore('('), original).map(::normalized).filter { it.isNotBlank() }.toSet()
        val civil = normalized(display.substringAfter('(', "").substringBefore(')'))
        val matches = valid.filter { c ->
            (normalized(c.originalName) in bases || normalized(c.name.substringBefore('(')) in bases) &&
                (civil.isBlank() || normalized(c.name.substringAfter('(', "").substringBefore(')')) == civil || c.aliases.any { normalized(it) == civil })
        }
        return matches.singleOrNull()?.let(::id)
    }

    private fun key(raw: String): String {
        val u = raw.toHttpUrlOrNull() ?: return raw
        return u.host + u.encodedPath.replace(scales, "/") + (u.query?.let { "?$it" } ?: "")
    }

    private fun publicImage(raw: String): Boolean {
        val u = raw.toHttpUrlOrNull() ?: return false
        return raw.length <= 2000 && u.scheme == "https" && u.username.isEmpty() && u.password.isEmpty() &&
            u.queryParameterNames.none { it.lowercase() in setOf("api_key", "apikey", "api-key", "token", "access_token") }
    }

    private fun providerImage(raw: String): Boolean {
        if (!publicImage(raw)) return false
        val u = raw.toHttpUrlOrNull() ?: return false
        return u.host in hosts && u.port == 443 && u.query == null && u.fragment == null &&
            (u.encodedPath.startsWith("/a/uploads/") || u.encodedPath.startsWith("/uploads/"))
    }

    internal fun merge(current: String, alternatives: List<String>): List<String> =
        (listOf(current).filter { it.isNotBlank() } + alternatives.filter(::publicImage)).distinctBy(::key).take(6)

    internal fun resourceImages(detail: CatalogDetails, id: String): List<String> {
        if (detail.type != "character" || detail.id != id) return emptyList()
        return (detail.covers + detail.image).filter(::providerImage).distinctBy(::key).take(6)
    }

    internal fun issuePortraits(detail: CatalogDetails, id: String): List<String> {
        if (detail.type != "character" || detail.id != id) return emptyList()
        val charNorm = normalized(detail.title.substringBefore('('))
        return detail.issues
            .sortedByDescending { issue ->
                val titleNorm = normalized(issue.displayTitle)
                if (charNorm.isNotBlank() && titleNorm.contains(charNorm)) 1 else 0
            }
            .map { it.coverUrl }
            .filter(::providerImage)
            .distinctBy(::key)
            .take(6)
    }

    internal fun matchVerifiedCharacterPortraits(
        display: String,
        original: String,
        candidates: List<CharacterResult>
    ): List<String> {
        val bases = listOf(display.substringBefore('('), original).map(::normalized).filter { it.isNotBlank() }.toSet()
        val civil = normalized(display.substringAfter('(', "").substringBefore(')'))
        return candidates.filter { c ->
            val nameBase = normalized(c.name.substringBefore('('))
            val origBase = normalized(c.originalName.substringBefore('('))
            val cCivil = normalized(c.name.substringAfter('(', "").substringBefore(')'))
            val baseMatches = origBase in bases || nameBase in bases || (civil.isNotBlank() && (origBase == civil || nameBase == civil))
            val civilMatches = if (civil.isBlank()) {
                cCivil.isBlank()
            } else {
                cCivil == civil || c.aliases.any { normalized(it) == civil } || origBase == civil
            }
            baseMatches && civilMatches
        }.map { it.image }.filter(::publicImage).distinctBy(::key)
    }

    private suspend fun resolveCharacterId(display: String, original: String, rawSnapshot: String): String? {
        linkedId(rawSnapshot)?.let { return it }
        canonicalIds[normalized(display)]?.let { return it }
        val primaryCandidates = runCatching { ComicVineCatalog.characters(original) }.getOrDefault(emptyList())
        exactId(display, original, primaryCandidates)?.let { return it }

        val civil = display.substringAfter('(', "").substringBefore(')').trim()
        if (civil.length >= 3) {
            val civilCandidates = runCatching { ComicVineCatalog.characters(civil) }.getOrDefault(emptyList())
            exactId(display, civil, civilCandidates)?.let { return it }
            val exactCivil = civilCandidates.filter { c ->
                cvId.matches(c.id) && (
                    normalized(c.originalName) == normalized(civil) ||
                        normalized(c.name.substringBefore('(')) == normalized(civil) ||
                        normalized(c.name.substringAfter('(', "").substringBefore(')')) == normalized(civil)
                    )
            }.distinctBy { it.id }
            if (exactCivil.size == 1) {
                return cvId.matchEntire(exactCivil.single().id)?.groupValues?.get(1)
            }
        }
        return null
    }

    fun observeChoices(display: String, current: String, rawSnapshot: String): Flow<List<String>> = flow {
        val initial = merge(current, emptyList())
        if (initial.isNotEmpty()) emit(initial)
        val loaded = choices(display, rawSnapshot)
        emit(merge(current, loaded))
    }.flowOn(Dispatchers.IO)

    suspend fun choices(display: String, rawSnapshot: String): List<String> = withContext(Dispatchers.IO) {
        val cacheKey = "${normalized(display)}|${linkedId(rawSnapshot).orEmpty()}|${snapshot(rawSnapshot)?.optString("name").orEmpty()}"
        synchronized(cache) {
            val hit = cache[cacheKey]
            if (hit != null && System.currentTimeMillis() - hit.savedAt < 300_000L && hit.items.isNotEmpty()) {
                return@withContext hit.items
            }
        }

        val deferred = coroutineScope {
            inflightMutex.withLock {
                inflight[cacheKey]?.takeIf { it.isActive } ?: async {
                    concurrencySemaphore.withPermit {
                        loadChoicesInternal(display, rawSnapshot)
                    }
                }.also { inflight[cacheKey] = it }
            }
        }
        try {
            val items = deferred.await()
            if (items.isNotEmpty()) {
                synchronized(cache) {
                    cache[cacheKey] = CacheEntry(items, System.currentTimeMillis())
                }
            }
            items
        } finally {
            inflightMutex.withLock {
                if (inflight[cacheKey] === deferred) inflight.remove(cacheKey)
            }
        }
    }

    private suspend fun loadChoicesInternal(display: String, rawSnapshot: String): List<String> {
        return withTimeoutOrNull(8500L) {
            val snap = snapshot(rawSnapshot)
            val snapImg = snap?.optString("imageUrl").orEmpty()
            val original = snap?.optString("name").orEmpty().ifBlank {
                val base = display.substringBefore('(').trim()
                CharacterDirectory.portuguese.entries.firstOrNull { (_, pt) -> normalized(pt) == normalized(base) }?.key ?: base
            }
            val linked = linkedId(rawSnapshot)
            if (linked == null && original.trim().length < 2) return@withTimeoutOrNull emptyList<String>()

            val collected = mutableListOf<String>()
            if (publicImage(snapImg)) collected += snapImg

            val id = resolveCharacterId(display, original, rawSnapshot)
            if (id != null) {
                val detail = runCatching { ComicVineCatalog.details("character", id) }.getOrNull()
                if (detail != null) {
                    collected += resourceImages(detail, id)
                    if (collected.distinctBy(::key).size < 6) {
                        collected += issuePortraits(detail, id)
                    }
                }
            }

            if (collected.distinctBy(::key).size < 6) {
                val civil = display.substringAfter('(', "").substringBefore(')').trim()
                val query = if (civil.length >= 3 && id == null) "$original $civil" else original
                val supplementary = runCatching { ComicVineCatalog.superheroCharacters(query) }.getOrDefault(emptyList())
                collected += matchVerifiedCharacterPortraits(display, original, supplementary)
            }

            collected.filter(::publicImage).distinctBy(::key).take(6)
        } ?: throw IOException("Imagens temporariamente indisponíveis")
    }
}

