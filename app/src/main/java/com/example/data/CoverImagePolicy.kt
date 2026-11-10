package com.example.data

/** Cheap raster alternatives only; never search the catalog again for every visible cover. */
internal object CoverImagePolicy {
    fun preferred(primary: String, other: String): String = when {
        primary.isBlank() -> other
        primary.startsWith("https://uploads.mangadex.org/") && other.startsWith("https://media.kitsu.app/") -> other
        else -> primary
    }
    fun candidates(raw: String): List<String> {
        val url=raw.trim().replace("http://","https://")
        if(url.isBlank())return emptyList()
        val alternatives=when {
            url.startsWith("https://media.kitsu.app/") -> listOf(url.replace("/medium.","/small."))
            url.contains("comicvine.gamespot.com/") -> listOf(url.replace("/scale_large/","/scale_medium/").replace("/screen_super/","/screen_medium/"))
            else -> emptyList()
        }
        return (listOf(url)+alternatives).distinct()
    }
}
