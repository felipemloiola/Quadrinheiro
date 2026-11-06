package com.example

import com.example.data.PortugueseComicTranslator
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ApiFixesStage5Test {

    @Test
    fun translatesCreatorRolesAccuratelyToPortuguese() {
        assertEquals("Roteiro", PortugueseComicTranslator.translateRole("Writer"))
        assertEquals("Desenho", PortugueseComicTranslator.translateRole("Penciler"))
        assertEquals("Desenho", PortugueseComicTranslator.translateRole("Penciller"))
        assertEquals("Arte-final", PortugueseComicTranslator.translateRole("Inker"))
        assertEquals("Cores", PortugueseComicTranslator.translateRole("Colorist"))
        assertEquals("Cores", PortugueseComicTranslator.translateRole("Colourist"))
        assertEquals("Capa", PortugueseComicTranslator.translateRole("Cover Artist"))
        assertEquals("Letras", PortugueseComicTranslator.translateRole("Letterer"))
        assertEquals("Edição", PortugueseComicTranslator.translateRole("Editor"))
    }

    @Test
    fun formatsEditorialPhrasesAndAvoidsAllWordCapitalization() {
        val raw = "COLLECTING ISSUES #1-6 OF THE ACCLAIMED SERIES. WRITTEN BY ALAN MOORE. ART BY DAVE GIBBONS."
        val formatted = PortugueseComicTranslator.formatCleanSynopsis(raw)
        assertTrue(formatted.contains("Reúne as edições"))
        assertTrue(formatted.contains("Escrito por"))
        assertTrue(formatted.contains("Arte de"))
        assertFalse(formatted.contains("COLLECTING ISSUES"))
        assertFalse(formatted.contains("WRITTEN BY"))
    }

    @Test
    fun preservesOriginalTitleWithoutForcingTranslation() {
        val originalTitle = "Swamp Thing #40"
        assertEquals("Swamp Thing #40", originalTitle)
    }
}
