package com.example

import com.example.data.*
import com.example.model.ComicEntry
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[24,34])
class EditorialContentPolicyTest {
    private fun book(id:String,title:String)=ComicEntry(id,id,title,authors="Scott Snyder",publisher="DC",pageCount=160,issueNumber=2,format="Encadernado (TPB)")
    @Test fun searchChoosesRichestWorkWithoutMixingTradeAndMonthlyOrChangingSavedEdition() {
        val italian=book("ab-IT-1","Batman Absoluto (2024) 2").copy(isbn="9781234567897",synopsis="Uma história para todos os leitores.")
        val full=book("mt-2","Absolute Batman Vol. 2").copy(pageCount=240,synopsis="A complete description with all credits.",editionLanguage="en")
        val monthly=book("mt-22","Absolute Batman #2").copy(pageCount=32,format="Edição")
        val next=full.copy(id="mt-3",externalId="mt-3",title="Absolute Batman Vol. 3",issueNumber=3)
        val result=CatalogWorkIdentity.preferred(listOf(italian,full,monthly,next))
        assertEquals(3,result.size);assertFalse(result.contains(italian));assertTrue(result.contains(full));assertTrue(result.contains(monthly))
        assertEquals(160,italian.pageCount);assertEquals("9781234567897",italian.isbn)
        assertEquals(1,CatalogWorkIdentity.preferred(listOf(italian.copy(title="Batman Assoluto (2024) 2"),full)).size)
    }
    @Test fun relatedCreditsRequireStoryEvidenceAndKeepEditionData() {
        val local=book("local","Batman Absoluto (2024) 2").copy(synopsis="Contiene: Absolute Batman (2024) 7-14",isbn="local-isbn")
        val full=CatalogDetails("2","metron-issue","Absolute Batman","","","Collects Absolute Batman #7-14.","DC","2026","",emptyList(),listOf(CatalogCredit("mt-1","Batman")),emptyList(),emptyList(),pageCount=240,issueNumber=2)
        val companion=CatalogWorkIdentity.companion(local,full)!!
        assertEquals("work",companion.creditsScope);assertEquals(160,companion.pageCount);assertEquals("local-isbn",companion.isbn)
        assertNull(CatalogWorkIdentity.companion(local,full.copy(description="Collects Absolute Batman #1-6.")))
        assertNull(CatalogWorkIdentity.companion(local,full.copy(title="Superman")))
    }
    @Test fun creditsFromSeriesOnlyFillMissingGroupsAndKeepTheSelectedEdition() {
        val hero = CatalogCredit("1", "Batman")
        val creator = CatalogCredit("5", "Frank Miller")
        val issue = CatalogDetails("9", "issue", "Ano Um", "cover", "", "", "DC", "1987", "",
            emptyList(), listOf(hero), emptyList(), emptyList(), pageCount = 32, isbn = "edition-isbn")
        val volume = issue.copy(id = "8", type = "volume", image = "volume-cover",
            pageCount = 180, characters = listOf(CatalogCredit("2", "Robin")), creators = listOf(creator))
        val complete = ComicVineCatalog.fillMissingCredits(issue, volume, "series")
        assertEquals(issue.copy(creators = listOf(creator), creditsScope = "series"), complete)
    }
    @Test fun italianIsNotPortugueseAndFullSynopsisIsTranslatedWithoutDroppingItsEnd() {
        val source=("Mentre Bruce Wayne indaga nella città, coinvolge gli amici in un gioco pericoloso. Cosa succede nella struttura? ").repeat(13).trim()+" FINALEDELLASTORIA"
        assertFalse(PortugueseComicTranslator.isPortugueseText(source))
        assertTrue(PortugueseComicTranslator.isPortugueseText("Uma história em português para todos os leitores da cidade."))
        val chunks=PortugueseComicTranslator.translationChunks(source)
        assertEquals(source.trim(),chunks.joinToString(" "))
        assertTrue(chunks.all {it.length<=480})
        val previous=MultiApiSearchEngine.sharedHttpClient
        val requested=mutableListOf<String>()
        try {
            MultiApiSearchEngine.sharedHttpClient=OkHttpClient.Builder().addInterceptor {chain ->
                requested+=chain.request().url.queryParameter("q").orEmpty()
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                    .body("[[[\"Uma história em português para todos os leitores.\",\"original\"]]]".toResponseBody()).build()
            }.build()
            val translated=PortugueseComicTranslator.translateSynopsisToPtBr(source,"Obra exclusiva de teste","","",2026,160,allowNetworkTranslation=true)
            assertTrue(translated.isNotBlank());assertEquals(chunks,requested)
            assertTrue(requested.last().endsWith("FINALEDELLASTORIA"))
        } finally {MultiApiSearchEngine.sharedHttpClient=previous}
    }
}
