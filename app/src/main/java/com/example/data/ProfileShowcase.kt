package com.example.data

import com.example.model.AppSettings
import com.example.model.ComicEntry
import org.json.JSONObject

/** Showcase has its own revision; old clients and stale profile acknowledgements cannot erase it. */
object ProfileShowcase {
    const val MAX_FAVORITE_ITEMS = 4
    const val MAX_SHOWCASE_DETAILS = 8
    const val MAX_DETAIL_KEY_CHARS = 240
    const val MAX_DETAIL_VALUE_CHARS = 5000
    const val MAX_BANNER_URL_CHARS = 200000

    fun sanitizeDetails(
        details: Map<String, String>,
        favoriteComicIds: List<String> = emptyList(),
        favoriteCharacters: List<String> = emptyList()
    ): Map<String, String> {
        if (details.isEmpty()) return emptyMap()
        val prioritizedKeys = buildList {
            favoriteComicIds.take(MAX_FAVORITE_ITEMS).forEach { id ->
                val clean = id.take(200)
                if (clean.isNotBlank()) add("comic:$clean")
            }
            favoriteCharacters.take(MAX_FAVORITE_ITEMS).forEach { name ->
                val clean = name.take(60)
                if (clean.isNotBlank()) add("character:$clean")
            }
        }
        val out = LinkedHashMap<String, String>()
        for (pKey in prioritizedKeys) {
            if (out.size >= MAX_SHOWCASE_DETAILS) break
            val v = details[pKey] ?: continue
            val kClean = pKey.take(MAX_DETAIL_KEY_CHARS)
            if (kClean.isNotBlank() && v.isNotBlank()) {
                out[kClean] = v.take(MAX_DETAIL_VALUE_CHARS)
            }
        }
        for ((k, v) in details) {
            if (out.size >= MAX_SHOWCASE_DETAILS) break
            val kClean = k.take(MAX_DETAIL_KEY_CHARS)
            if (kClean.isNotBlank() && v.isNotBlank() && !out.containsKey(kClean)) {
                out[kClean] = v.take(MAX_DETAIL_VALUE_CHARS)
            }
        }
        return out
    }

    fun characterImage(raw:String):String=if(raw.trimStart().startsWith("{")) runCatching{JSONObject(raw).optString("image")}.getOrDefault("") else raw
    fun characterSnapshot(character:CharacterResult,image:String=character.image):String=JSONObject().put("id",character.id).put("name",character.originalName).put("image",image).toString()
    fun differs(a: AppSettings, b: AppSettings): Boolean {
        val aComics = a.profileFavoriteComicIds.map { it.take(200) }.filter { it.isNotBlank() }.distinct().take(MAX_FAVORITE_ITEMS)
        val bComics = b.profileFavoriteComicIds.map { it.take(200) }.filter { it.isNotBlank() }.distinct().take(MAX_FAVORITE_ITEMS)
        val aChars = a.profileFavoriteCharacters.map { it.take(60) }.filter { it.isNotBlank() }.distinct().take(MAX_FAVORITE_ITEMS)
        val bChars = b.profileFavoriteCharacters.map { it.take(60) }.filter { it.isNotBlank() }.distinct().take(MAX_FAVORITE_ITEMS)
        val aDetails = sanitizeDetails(a.profileShowcaseDetails, aComics, aChars)
        val bDetails = sanitizeDetails(b.profileShowcaseDetails, bComics, bChars)
        return aComics != bComics ||
            aChars != bChars ||
            aDetails != bDetails ||
            a.profileBannerUrl.take(MAX_BANNER_URL_CHARS) != b.profileBannerUrl.take(MAX_BANNER_URL_CHARS)
    }
    fun merge(local: AppSettings, incoming: AppSettings): AppSettings {
        val localComics = local.profileFavoriteComicIds.map { it.take(200) }.filter { it.isNotBlank() }.distinct().take(MAX_FAVORITE_ITEMS)
        val localChars = local.profileFavoriteCharacters.map { it.take(60) }.filter { it.isNotBlank() }.distinct().take(MAX_FAVORITE_ITEMS)
        val incomingComics = incoming.profileFavoriteComicIds.map { it.take(200) }.filter { it.isNotBlank() }.distinct().take(MAX_FAVORITE_ITEMS)
        val incomingChars = incoming.profileFavoriteCharacters.map { it.take(60) }.filter { it.isNotBlank() }.distinct().take(MAX_FAVORITE_ITEMS)
        val legacyLocal = local.profileShowcaseUpdatedAt == 0L && incoming.profileShowcaseUpdatedAt == 0L &&
            (localComics.isNotEmpty() || localChars.isNotEmpty()) &&
            incomingComics.isEmpty() && incomingChars.isEmpty()
        val baseMerged = if (local.profileShowcaseUpdatedAt > incoming.profileShowcaseUpdatedAt || legacyLocal) incoming.copy(
            profileFavoriteComicIds = localComics,
            profileFavoriteCharacters = localChars,
            profileShowcaseDetails = sanitizeDetails(local.profileShowcaseDetails, localComics, localChars),
            profileBannerUrl = local.profileBannerUrl.take(MAX_BANNER_URL_CHARS),
            profileShowcaseUpdatedAt = local.profileShowcaseUpdatedAt
        ) else incoming.copy(
            profileFavoriteComicIds = incomingComics,
            profileFavoriteCharacters = incomingChars,
            profileShowcaseDetails = sanitizeDetails(incoming.profileShowcaseDetails, incomingComics, incomingChars),
            profileBannerUrl = incoming.profileBannerUrl.take(MAX_BANNER_URL_CHARS)
        )
        return if (baseMerged.cachedRecommendationsJson.isBlank() && local.cachedRecommendationsJson.isNotBlank()) {
            baseMerged.copy(
                cachedRecommendationsJson = local.cachedRecommendationsJson.take(12288),
                recommendationTasteHash = local.recommendationTasteHash.replace("\n", "").take(64)
            )
        } else baseMerged
    }
    fun comicSnapshot(comic: ComicEntry): String = JSONObject().put("id", comic.id).put("title", comic.displayTitle.take(300))
        .put("externalId",comic.externalId.take(100)).put("pages",comic.pageCount).put("format",comic.format).put("year",comic.releaseYear).put("source",comic.sourceLabel)
        .put("cover", comic.coverUrl.take(2000)).put("authors", comic.authors.take(300)).put("publisher", comic.publisher.take(160)).toString()
    fun restoreComic(raw: String): ComicEntry? = runCatching { val j = JSONObject(raw)
        ComicEntry(j.getString("id"),j.optString("externalId"),j.getString("title"),authors=j.optString("authors"),publisher=j.optString("publisher"),coverUrl=j.optString("cover"),
            pageCount=j.optInt("pages",1),format=j.optString("format","Edição"),releaseYear=j.optInt("year",2026),sourceLabel=j.optString("source","Comic Vine"))
    }.getOrNull()
    fun encode(s: AppSettings): String {
        val cleanComics = s.profileFavoriteComicIds.map { it.take(200) }.filter { it.isNotBlank() }.distinct().take(MAX_FAVORITE_ITEMS)
        val cleanChars = s.profileFavoriteCharacters.map { it.take(60) }.filter { it.isNotBlank() }.distinct().take(MAX_FAVORITE_ITEMS)
        val cleanDetails = sanitizeDetails(s.profileShowcaseDetails, cleanComics, cleanChars)
        return JSONObject().put("ids", org.json.JSONArray(cleanComics))
            .put("characters", org.json.JSONArray(cleanChars)).put("details", JSONObject(cleanDetails))
            .put("banner", s.profileBannerUrl.take(MAX_BANNER_URL_CHARS)).put("revision", s.profileShowcaseUpdatedAt).toString()
    }
    fun decode(raw: String): AppSettings? = runCatching { val j = JSONObject(raw)
        fun strings(key: String, maxLen: Int) = j.optJSONArray(key).let { a -> (0 until (a?.length() ?: 0)).map { a!!.optString(it).take(maxLen) }.filter { it.isNotBlank() }.distinct().take(MAX_FAVORITE_ITEMS) }
        val ids = strings("ids", 200)
        val chars = strings("characters", 60)
        val detailsObj = j.optJSONObject("details")
        val rawMap = detailsObj?.keys()?.asSequence()?.associateWith { detailsObj.optString(it) }.orEmpty()
        AppSettings(profileFavoriteComicIds = ids, profileFavoriteCharacters = chars,
            profileShowcaseDetails = sanitizeDetails(rawMap, ids, chars),
            profileBannerUrl = j.optString("banner").take(MAX_BANNER_URL_CHARS), profileShowcaseUpdatedAt = j.optLong("revision"))
    }.getOrNull()
}
