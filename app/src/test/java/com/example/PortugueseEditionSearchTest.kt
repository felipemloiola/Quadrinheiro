package com.example

import com.example.data.PortugueseSearchPolicy
import com.example.model.ComicEntry
import org.junit.Assert.*
import org.junit.Test

class PortugueseEditionSearchTest {
    private val book=ComicEntry(id="ab-BR-1",externalId="ab-BR-1",authors="",title="Batman",publisher="Panini",isOnlineResult=true)

    @Test fun storefrontPublisherAndTranslatedTitleDoNotOverrideForeignEditionLanguage() {
        for(language in listOf("en","es","de","it","fr")) {
            assertFalse(language,PortugueseSearchPolicy.accepts(book.copy(editionLanguage=language,titlePtBr="Batman: Ano Um")))
        }
        assertTrue(PortugueseSearchPolicy.accepts(book.copy(editionLanguage="pt-BR")))
        assertTrue(PortugueseSearchPolicy.accepts(book.copy(editionLanguage="pt_PT")))
    }
    @Test fun unknownOnlineLanguageNeedsPortugueseSourceEvidence() {
        assertFalse(PortugueseSearchPolicy.accepts(book.copy(title="Batman: El Hombre que Dominó el Cero Absoluto")))
        assertFalse(PortugueseSearchPolicy.accepts(book.copy(title="Superman & Batman: Generations III",synopsis="The story of the world's greatest heroes.")))
        assertFalse(PortugueseSearchPolicy.accepts(book.copy(synopsisPtBr="Uma história em português para todos os leitores.")))
        assertTrue(PortugueseSearchPolicy.accepts(book.copy(synopsis="Uma história em português para todos os leitores. Seus heróis também estão nesta edição.")))
    }
    @Test fun verifiedCuratedAndLocalEntriesArePreserved() {
        assertTrue(PortugueseSearchPolicy.accepts(book.copy(id="cat-batman",externalId="",isOnlineResult=false)))
    }
    @Test fun specialistComicCatalogsAcceptPtAndEnWhileBlockingThirdLanguages() {
        val metronEn = ComicEntry(id="mt-101", externalId="mt-101", title="Batman: Year One", authors="Frank Miller", publisher="DC Comics", editionLanguage="en", sourceLabel="Metron", isOnlineResult=true)
        val cvUnknown = ComicEntry(id="cv-volume-796", externalId="cv-volume-796", title="Daredevil: Born Again", authors="Frank Miller", publisher="Marvel", editionLanguage="", sourceLabel="Comic Vine", isOnlineResult=true)
        val mdPt = ComicEntry(id="md-8f3e1818-a015-491d-bd81-3addc4d7d56a", externalId="md-8f3e1818-a015-491d-bd81-3addc4d7d56a", title="Berserk", authors="Kentaro Miura", publisher="", genre="Mangá", format="Série de mangá", editionLanguage="pt", isOnlineResult=true)
        assertTrue(PortugueseSearchPolicy.accepts(metronEn))
        assertTrue(PortugueseSearchPolicy.accepts(cvUnknown))
        assertTrue(PortugueseSearchPolicy.accepts(mdPt))

        assertFalse(PortugueseSearchPolicy.accepts(metronEn.copy(editionLanguage="de", title="Batman: Die Rückkehr")))
        assertFalse(PortugueseSearchPolicy.accepts(cvUnknown.copy(title="Lanterna Verde - La notte più profonda")))
        assertFalse(PortugueseSearchPolicy.accepts(cvUnknown.copy(title="雲海爭奇錄")))
    }
}
