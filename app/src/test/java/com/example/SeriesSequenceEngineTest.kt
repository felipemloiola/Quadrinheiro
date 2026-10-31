package com.example

import com.example.data.CuratedCatalog
import com.example.data.SeriesSequenceEngine
import com.example.model.ComicEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeriesSequenceEngineTest {

    @Test
    fun testStandaloneGraphicNovelWatchmenEntersRecommendationMode() {
        val watchmen = CuratedCatalog.curatedComicsCatalog.first { it.title.contains("Watchmen", ignoreCase = true) }
        val seqGroup = SeriesSequenceEngine.findSequenceForComic(
            target = watchmen,
            userComics = listOf(watchmen),
            catalogComics = CuratedCatalog.curatedComicsCatalog
        )

        assertTrue("Watchmen deve entrar em modo de recomendação/títulos relacionados", seqGroup.isRecommendationMode)
        assertTrue("Watchmen deve conter recomendações ricas", seqGroup.recommendations.isNotEmpty())
        assertEquals("Quem leu esta Graphic Novel também curtiu", seqGroup.recommendationSubtitle)

        // Verifica se recomenda outras obras clássicas ou do Alan Moore (ex: V de Vingança, Monstro do Pântano, etc.)
        val recommendedTitles = seqGroup.recommendations.map { it.comic.title.lowercase() }
        val hasRelevantRecommendation = recommendedTitles.any {
            it.contains("vingança") || it.contains("vinganca") || it.contains("monstro") || it.contains("cavaleiro") || it.contains("piada")
        }
        assertTrue("Deve recomendar títulos afins a Watchmen", hasRelevantRecommendation)
    }

    @Test
    fun testStandaloneGraphicNovelMausEntersRecommendationMode() {
        val maus = CuratedCatalog.curatedComicsCatalog.first { it.title.contains("Maus", ignoreCase = true) }
        val seqGroup = SeriesSequenceEngine.findSequenceForComic(
            target = maus,
            userComics = listOf(maus),
            catalogComics = CuratedCatalog.curatedComicsCatalog
        )

        assertTrue("Maus deve entrar em modo de recomendação/títulos relacionados", seqGroup.isRecommendationMode)
        assertTrue("Maus deve conter recomendações ricas", seqGroup.recommendations.isNotEmpty())
        val recommendedTitles = seqGroup.recommendations.map { it.comic.title.lowercase() }
        val hasRelevantRecommendation = recommendedTitles.any {
            it.contains("persépolis") || it.contains("persepolis") || it.contains("daytripper") || it.contains("watchmen")
        }
        assertTrue("Deve recomendar títulos afins a Maus", hasRelevantRecommendation)
    }

    @Test
    fun testSerializedSeriesDoesNotEnterRecommendationMode() {
        val batman1 = CuratedCatalog.curatedComicsCatalog.first { it.title == "Batman #1" }
        val batmanComics = CuratedCatalog.curatedComicsCatalog.filter { it.title.startsWith("Batman #") }
        val seqGroup = SeriesSequenceEngine.findSequenceForComic(
            target = batman1,
            userComics = batmanComics,
            catalogComics = CuratedCatalog.curatedComicsCatalog
        )

        assertFalse("Série serializada como Batman #1..#5 NÃO deve entrar em modo de recomendação", seqGroup.isRecommendationMode)
        assertTrue("Série serializada deve manter edições sequenciais", seqGroup.issues.size >= 2)
    }
}
