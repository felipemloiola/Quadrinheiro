package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.model.AppSettings
import com.example.ui.screens.CommunityCommentComposer
import com.example.ui.theme.QuadrinheiroTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class CommunityCommentComposerTest {
    @get:Rule val compose = createComposeRule()

    @Test fun failedSendLeavesTheDraftAvailableForRetry() {
        val submitted = mutableListOf<String>()
        var refreshes = 0
        val message = "Valeu pela indicação!"
        compose.setContent {
            QuadrinheiroTheme(AppSettings(reduceMotion = true, reduceTransparency = true)) {
                CommunityCommentComposer(enabled = true, onRefresh = { refreshes++ }) { text ->
                    submitted += text
                    if (submitted.size == 1) throw IOException("Test connection failure")
                }
            }
        }
        compose.onNode(hasSetTextAction() and hasContentDescription("Adicione um comentário")).performTextInput(message)
        compose.onNodeWithText("Enviar").performClick()
        compose.onNodeWithText("Não foi possível enviar. Seu texto foi mantido.").assertIsDisplayed()
        compose.onNode(hasSetTextAction() and hasText(message)).assertExists()
        compose.onNodeWithText("Tentar enviar novamente").performClick()
        compose.onNode(hasSetTextAction() and hasText(message)).assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(listOf(message, message), submitted)
            assertEquals(1, refreshes)
        }
    }
}
