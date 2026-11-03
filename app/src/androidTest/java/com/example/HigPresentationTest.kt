package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.screens.SearchResultComicCard
import com.example.ui.theme.*
import com.example.updates.*
import com.kyant.backdrop.backdrops.emptyBackdrop
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HigPresentationTest {
    @get:Rule val compose = createComposeRule()
    private fun snapshot(name: String) {
        compose.waitForIdle()
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        java.io.File(context.getExternalFilesDir(null), name).outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun narrowCatalogWithLargeTextKeepsAllActionsReachable() {
        var selected: ReadingStatus? = null
        compose.setContent {
            QuadrinheiroTheme(AppSettings(colorScheme = "dark")) {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
                    Box(Modifier.width(300.dp)) {
                        SearchResultComicCard(ComicEntry("fixture", "", "Fixture", authors = "Autor", publisher = "Editora"), null,
                            AppSettings(), {}, {}, { _, status -> selected = status })
                    }
                }
            }
        }
        compose.onNodeWithText("+ Lido").assertIsDisplayed()
        compose.onNodeWithText("Lendo").assertIsDisplayed()
        compose.onNodeWithText("Quero Ler").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(ReadingStatus.WANT_TO_READ, selected) }
    }

    @Test fun updateDialogKeepsDownloadAndDismissActionsAccessible() {
        var downloads = 0
        var dismissed = false
        compose.setContent {
            QuadrinheiroTheme(AppSettings(colorScheme = "light")) {
                AppUpdateDialog({}, LocalIOS27Theme.current.bgSecondary,
                    title = { Text("Atualização disponível") },
                    text = { Text("A nova versão está pronta para baixar no aplicativo.") },
                    confirmButton = { UpdateDialogAction({ downloads++ }) { Text("Baixar atualização") } },
                    dismissButton = { UpdateDialogAction({ dismissed = true }) { Text("Agora não") } })
            }
        }
        compose.onNodeWithText("Baixar atualização").assertIsDisplayed().performClick()
        snapshot("hig-updater-light.png")
        compose.onNodeWithText("Agora não").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, downloads); assertTrue(dismissed) }
    }

    @Test fun actualPickerCanOpenChooseAndReopenWithoutStaleSelection() {
        val host = LiquidGlassMenuHostState()
        val selected = mutableStateOf(0)
        compose.setContent {
            QuadrinheiroTheme(AppSettings(colorScheme = "dark")) {
                CompositionLocalProvider(LocalLiquidGlassMenuHost provides host) {
                    Box(Modifier.fillMaxSize().padding(20.dp)) {
                        AppleMenuPicker(listOf(SegmentOption(0, "Mais Recentes"), SegmentOption(1, "Título")), selected.value, { selected.value = it })
                        LiquidGlassMenuOverlayHost(host, emptyBackdrop())
                    }
                }
            }
        }
        compose.onNodeWithText("Mais Recentes").performClick()
        compose.onNodeWithText("Título").performClick()
        compose.runOnIdle { assertEquals(1, selected.value) }
        compose.waitForIdle()
        compose.onNodeWithText("Título").performClick()
        compose.onAllNodesWithText("Mais Recentes").onLast().assertIsDisplayed()
    }
}
