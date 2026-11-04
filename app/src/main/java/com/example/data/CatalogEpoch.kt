package com.example.data

import com.example.model.AppSettings
import com.example.model.ComicEntry
import com.example.model.ReaderProfile

/** One-time reset explicitly requested by the project owner for every old account catalog. */
object CatalogEpoch {
    const val VERSION=51
    const val RESET_AT=1704067200000L
    fun settings(old:AppSettings)=old.copy(profileFavoriteComicIds=emptyList(),profileShowcaseDetails=old.profileShowcaseDetails.filterKeys { !it.startsWith("comic:") },profileShowcaseUpdatedAt=maxOf(old.profileShowcaseUpdatedAt,RESET_AT))
    fun profile(old:ReaderProfile)=old.copy(activeReadingComicId="")
    fun afterReset(comic:ComicEntry)=comic.updatedAt<=0L || comic.updatedAt>RESET_AT
}
