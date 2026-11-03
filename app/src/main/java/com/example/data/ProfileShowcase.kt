package com.example.data

import com.example.model.AppSettings
import com.example.model.ComicEntry
import org.json.JSONObject

/** Showcase has its own revision; old clients and stale profile acknowledgements cannot erase it. */
object ProfileShowcase {
    fun differs(a: AppSettings, b: AppSettings) = a.profileFavoriteComicIds != b.profileFavoriteComicIds ||
        a.profileFavoriteCharacters != b.profileFavoriteCharacters || a.profileShowcaseDetails != b.profileShowcaseDetails || a.profileBannerUrl != b.profileBannerUrl
    fun merge(local: AppSettings, incoming: AppSettings): AppSettings {
        val legacyLocal = local.profileShowcaseUpdatedAt == 0L && incoming.profileShowcaseUpdatedAt == 0L &&
            (local.profileFavoriteComicIds.isNotEmpty() || local.profileFavoriteCharacters.isNotEmpty()) &&
            incoming.profileFavoriteComicIds.isEmpty() && incoming.profileFavoriteCharacters.isEmpty()
        return if (local.profileShowcaseUpdatedAt > incoming.profileShowcaseUpdatedAt || legacyLocal) incoming.copy(
            profileFavoriteComicIds = local.profileFavoriteComicIds, profileFavoriteCharacters = local.profileFavoriteCharacters,
            profileShowcaseDetails = local.profileShowcaseDetails, profileBannerUrl = local.profileBannerUrl,
            profileShowcaseUpdatedAt = local.profileShowcaseUpdatedAt) else incoming
    }
    fun comicSnapshot(comic: ComicEntry): String = JSONObject().put("id", comic.id).put("title", comic.displayTitle.take(300))
        .put("cover", comic.coverUrl.take(2000)).put("authors", comic.authors.take(300)).put("publisher", comic.publisher.take(160)).toString()
    fun restoreComic(raw: String): ComicEntry? = runCatching { val j = JSONObject(raw)
        ComicEntry(j.getString("id"), "", j.getString("title"), authors = j.optString("authors"), publisher = j.optString("publisher"), coverUrl = j.optString("cover"))
    }.getOrNull()
    fun encode(s: AppSettings) = JSONObject().put("ids", org.json.JSONArray(s.profileFavoriteComicIds))
        .put("characters", org.json.JSONArray(s.profileFavoriteCharacters)).put("details", JSONObject(s.profileShowcaseDetails))
        .put("banner", s.profileBannerUrl).put("revision", s.profileShowcaseUpdatedAt).toString()
    fun decode(raw: String): AppSettings? = runCatching { val j = JSONObject(raw)
        fun strings(key: String) = j.optJSONArray(key).let { a -> (0 until (a?.length() ?: 0)).map { a!!.optString(it) }.filter { it.isNotBlank() }.distinct().take(4) }
        val details = j.optJSONObject("details")
        AppSettings(profileFavoriteComicIds = strings("ids"), profileFavoriteCharacters = strings("characters"),
            profileShowcaseDetails = details?.keys()?.asSequence()?.take(8)?.associateWith { details.optString(it).take(5000) }.orEmpty(),
            profileBannerUrl = j.optString("banner").take(200000), profileShowcaseUpdatedAt = j.optLong("revision"))
    }.getOrNull()
}
