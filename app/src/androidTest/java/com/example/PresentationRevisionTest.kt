package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.model.AppSettings
import com.example.model.ComicEntry
import com.example.model.ReadingStatus
import com.example.ui.screens.DiaryAnalyticsSummaryCard
import com.example.ui.components.AppPage
import com.example.ui.components.AppPageTransition
import com.example.ui.screens.WelcomeIntroScreen
import com.example.ui.theme.QuadrinheiroTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PresentationRevisionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun diarySummaryShowsReadingHistoryWithoutGoalOrShareActions() {
        compose.setContent {
            QuadrinheiroTheme(AppSettings()) {
                DiaryAnalyticsSummaryCard(listOf(
                    ComicEntry("read", "", "Lido", authors = "", publisher = "", pageCount = 120, status = ReadingStatus.READ, rating = 4f),
                    ComicEntry("reading", "", "Lendo", authors = "", publisher = "", pageCount = 80, currentPage = 20, status = ReadingStatus.READING)
                ))
            }
        }
        compose.onNodeWithText("Concluídas").assertIsDisplayed()
        compose.onNodeWithText("140").assertIsDisplayed()
        compose.onNodeWithText("Nota média").assertIsDisplayed()
        compose.onNode(hasClickAction()).assertDoesNotExist()
    }

    @Test fun welcomeSummaryFitsCompactHeightWithLargerTextWithoutScrolling() {
        var continued = false
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                QuadrinheiroTheme(AppSettings(reduceMotion = true)) {
                    Box(Modifier.fillMaxWidth().height(640.dp).testTag("compact-host")) {
                        WelcomeIntroScreen { continued = true }
                    }
                }
            }
        }
        compose.onNodeWithTag("welcome_start_button").performClick()
        compose.onNodeWithText("Histórico de leitura").assertIsDisplayed()
        val host = compose.onNodeWithTag("compact-host").getUnclippedBoundsInRoot()
        val button = compose.onNodeWithTag("welcome_primary_button").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("CTA must fit fully inside the page", button.bottom <= host.bottom)
        compose.onNodeWithTag("welcome_primary_button").performClick()
        compose.runOnIdle { assertTrue(continued) }
    }

    @Test fun hierarchicalPagesEnterAndReturnWithoutLosingDestination() {
        val page = mutableStateOf(AppPage("settings", null, null, false))
        compose.setContent {
            QuadrinheiroTheme(AppSettings()) {
                AppPageTransition(page.value) { destination -> Text(destination.settings ?: "Ajustes raiz") }
            }
        }
        compose.onNodeWithText("Ajustes raiz").assertIsDisplayed()
        compose.runOnIdle { page.value = page.value.copy(settings = "Aparência") }
        compose.onNodeWithText("Aparência").assertIsDisplayed()
        compose.onNodeWithText("Ajustes raiz").assertDoesNotExist()
        compose.runOnIdle { page.value = page.value.copy(settings = null) }
        compose.onNodeWithText("Ajustes raiz").assertIsDisplayed()
        compose.onNodeWithText("Aparência").assertDoesNotExist()
    }
}
