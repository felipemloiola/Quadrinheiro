package com.example

import com.example.data.ComicVineCatalog
import com.example.data.PortugueseComicTranslator
import com.example.model.ComicEntry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class CatalogRevision52Test {
    @Test fun editionsKeepRealPageCountsAndUnknownPagesAreNotInventedByLocalization(){
        val json=JSONObject("""{"id":"184296","type":"metron-issue","name":"Batman: Year One - The Deluxe Edition","format":"Capa dura","pageCount":144,"isbn":"9781401207526"}""")
        val entry=ComicVineCatalog.comic(json)
        assertEquals("mt-184296",entry.externalId);assertEquals(144,entry.pageCount)
        assertEquals("9781401207526",entry.isbn)
        val unknown=entry.copy(id="unknown52",externalId="mt-999999",pageCount=0,synopsis="",synopsisPtBr="")
        val localized=PortugueseComicTranslator.localizeComic(unknown,allowNetworkTranslation=false)
        assertEquals(0,localized.pageCount);assertTrue(localized.synopsis.isBlank())
    }
    @Test fun monthlyIssuesRetainTheirNumberAndCollectionsDoNotGetAMonthlySuffix(){
        val monthly=ComicVineCatalog.comic(JSONObject("""{"id":"1","type":"metron-issue","name":"Absolute Batman","issue":"1","format":"Edição"}"""))
        assertTrue(monthly.title.endsWith("#1"));assertEquals(1,monthly.issueNumber)
        val collection=ComicVineCatalog.comic(JSONObject("""{"id":"2","type":"metron-issue","name":"Watchmen","format":"Encadernado (TPB)","pageCount":416}"""))
        assertFalse(collection.title.contains("#"));assertEquals(416,collection.pageCount)
    }
}
