package com.example

import com.example.benchmark.BaselineProfileSpec
import com.kyant.backdrop.LiquidGlassShaderPrewarmer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PerformanceRenderingOptimizationTest {

    @Test
    fun `baseline profile spec generates AOT rules for cold start, welcome intro and liquid glass`() {
        val rules = BaselineProfileSpec.generateBaselineProfileRules()
        assertFalse("Baseline profile rules should not be empty", rules.isEmpty())
        assertTrue(
            "Should include Cold Start MainActivity rules",
            rules.any { it.contains("Lcom/example/MainActivity;") }
        )
        assertTrue(
            "Should include WelcomeIntroScreen transition rules",
            rules.any { it.contains("Lcom/example/ui/screens/WelcomeIntroScreenKt;") }
        )
        assertTrue(
            "Should include LiquidGlass shader and backdrop pipeline rules",
            rules.any { it.contains("Lcom/kyant/backdrop/DrawBackdropModifierKt;") } &&
                rules.any { it.contains("Lcom/example/ui/components/SdfShader;") }
        )
    }

    @Test
    fun `baseline-prof txt exists in main source set and contains critical AOT rules`() {
        val candidatePaths = listOf(
            File("src/main/baseline-prof.txt"),
            File("app/src/main/baseline-prof.txt")
        )
        val profileFile = candidatePaths.firstOrNull { it.exists() }
        assertTrue("baseline-prof.txt must exist in app/src/main", profileFile != null)

        val content = profileFile!!.readText()
        assertTrue(content.contains("HSPLcom/example/MainActivity;"))
        assertTrue(content.contains("WelcomeIntroScreen"))
        assertTrue(content.contains("LiquidGlassShaderPrewarmer"))
        assertTrue(content.contains("LayerBackdropNode"))
    }

    @Test
    fun `shader prewarmer executes safely off-frame without throwing`() {
        LiquidGlassShaderPrewarmer.prewarmSynchronously()
        assertTrue("Shader prewarmer should mark completion", LiquidGlassShaderPrewarmer.isReady)
    }

    @Test
    fun `reading statistics computes 12 monthly buckets, formats, publishers and author rankings`() {
        val sampleComics = listOf(
            com.example.model.ComicEntry(
                id = "1",
                externalId = "ext-1",
                title = "Sandman: Prelúdios & Noturnos",
                authors = "Neil Gaiman, Sam Kieth",
                publisher = "DC / Vertigo",
                genre = "DC / Vertigo",
                format = "Encadernado (TPB)",
                pageCount = 240,
                currentPage = 240,
                status = com.example.model.ReadingStatus.READ,
                rating = 5f,
                readDate = "2026-10-12"
            ),
            com.example.model.ComicEntry(
                id = "2",
                externalId = "ext-2",
                title = "Sandman: Casa de Bonecas",
                authors = "Neil Gaiman",
                publisher = "DC / Vertigo",
                genre = "DC / Vertigo",
                format = "Encadernado (TPB)",
                pageCount = 256,
                currentPage = 256,
                status = com.example.model.ReadingStatus.READ,
                rating = 4.8f,
                readDate = "2026-10-20"
            ),
            com.example.model.ComicEntry(
                id = "3",
                externalId = "ext-3",
                title = "Batman: Ano Um",
                authors = "Frank Miller, David Mazzucchelli",
                publisher = "Panini",
                genre = "DC / Vertigo",
                format = "Edição Especial / Capa Dura",
                pageCount = 144,
                currentPage = 72,
                status = com.example.model.ReadingStatus.READING,
                readDate = "2026-10-28"
            )
        )
        val profile = com.example.model.ReaderProfile(yearlyGoal = 24, dailyPagesGoal = 30)
        val summary = com.example.ui.screens.computeReadingStatistics(
            comics = sampleComics,
            profile = profile,
            accentColor = androidx.compose.ui.graphics.Color.Cyan,
            secondaryColor = androidx.compose.ui.graphics.Color.Magenta
        )

        org.junit.Assert.assertEquals(12, summary.monthlyBuckets.size)
        org.junit.Assert.assertEquals(2, summary.readComicsCount)
        org.junit.Assert.assertEquals(1, summary.readingComicsCount)
        org.junit.Assert.assertEquals(568, summary.totalPagesRead)
        assertTrue(summary.formatBreakdown.any { it.label.contains("Encadernados") })
        assertTrue(summary.publisherBreakdown.any { it.label.contains("DC / Vertigo") })
        org.junit.Assert.assertEquals("Neil Gaiman", summary.topAuthors.first().name)
        org.junit.Assert.assertEquals(2, summary.topAuthors.first().worksCount)
    }

    @Test
    fun `glance widget visual renderer adapts palette to selected AppTheme`() {
        val bluePalette = com.example.widget.WidgetVisualRenderer.resolvePaletteForThemeId("BLUE")
        val redPalette = com.example.widget.WidgetVisualRenderer.resolvePaletteForThemeId("RED")
        val monoPalette = com.example.widget.WidgetVisualRenderer.resolvePaletteForThemeId("SYSTEM_MONO")

        org.junit.Assert.assertNotEquals(bluePalette.outerRingColor, redPalette.outerRingColor)
        org.junit.Assert.assertNotEquals(bluePalette.outerRingColor, monoPalette.outerRingColor)
        org.junit.Assert.assertEquals("widget_comic_id", com.example.widget.UpdatePageAction.PARAM_COMIC_ID.name)
        org.junit.Assert.assertEquals("widget_page_delta", com.example.widget.UpdatePageAction.PARAM_PAGE_DELTA.name)
    }

    @Test
    fun `social share cards format completion dates, aura colors and HD aspect ratios`() {
        val formattedDate = com.example.ui.components.formatPortugueseCompletionDate("2026-10-30")
        org.junit.Assert.assertEquals("Concluído em 30 de Outubro de 2026", formattedDate)

        val batmanComic = com.example.model.ComicEntry(
            id = "b1",
            externalId = "ext-b1",
            title = "Batman: Ano Um",
            authors = "Frank Miller, David Mazzucchelli",
            publisher = "DC Comics",
            genre = "DC / Vertigo",
            format = "Edição Especial / Capa Dura",
            releaseYear = 1987,
            pageCount = 144,
            currentPage = 144,
            status = com.example.model.ReadingStatus.READ,
            rating = 5.0f,
            readDate = "2026-10-30"
        )
        val auraColor = com.example.ui.components.resolveAtmosphericAuraForComic(
            comic = batmanComic,
            sampledColor = null,
            fallbackAccent = androidx.compose.ui.graphics.Color(0xFF0A84FF)
        )
        org.junit.Assert.assertEquals(androidx.compose.ui.graphics.Color(0xFFE11D48), auraColor)

        val stories = com.example.ui.components.SocialShareAspectRatio.STORIES_9_16
        val feed = com.example.ui.components.SocialShareAspectRatio.FEED_1_1
        org.junit.Assert.assertEquals(1080, stories.widthPx)
        org.junit.Assert.assertEquals(1920, stories.heightPx)
        org.junit.Assert.assertEquals(1080, feed.widthPx)
        org.junit.Assert.assertEquals(1080, feed.heightPx)
        org.junit.Assert.assertEquals(3, com.example.ui.components.SocialShareCardType.entries.size)
    }
}

