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
}
