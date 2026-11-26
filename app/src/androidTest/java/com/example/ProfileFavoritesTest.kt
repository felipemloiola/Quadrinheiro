package com.example

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.CharacterResult
import com.example.data.ProfileShowcase
import com.example.model.AppSettings
import com.example.model.ComicEntry
import com.example.model.ReaderProfile
import com.example.ui.screens.FavoriteChoice
import com.example.ui.screens.FavoriteSelectionSheet
import com.example.ui.screens.ReaderProfileScreen
import com.example.ui.theme.QuadrinheiroTheme
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileFavoritesTest {
    @get:Rule val compose = createComposeRule()

    @Test fun profileAndEditorConfirmFavoritesIndependentlyOfOpeningTheSheet() {
        val comic = ComicEntry("curation-test", "", "Uma HQ para destacar", authors = "Autora", publisher = "Editora")
        val settings = mutableStateOf(AppSettings(reduceMotion = true, reduceTransparency = true))
        compose.setContent {
            QuadrinheiroTheme(settings.value) {
                ReaderProfileScreen(
                    comics = listOf(comic),
                    profile = ReaderProfile(name = "Leitora"),
                    settings = settings.value,
                    listState = rememberLazyListState(),
                    onSettings = {},
                    onLibrary = {},
                    onEditProfile = {},
                    onUpdateShowcase = { ids, characters, details ->
                        settings.value = settings.value.copy(
                            profileFavoriteComicIds = ids,
                            profileFavoriteCharacters = characters,
                            profileShowcaseDetails = details
                        )
                    },
                    onComic = {},
                    onStatistics = {}
                )
            }
        }

        val profile = compose.onNodeWithTag("reader_profile")
        profile.performScrollToNode(hasTestTag("favorite-comic-0"))
        val firstSlot = compose.onNodeWithContentDescription("Escolher HQ favorita 1")
        firstSlot.performClick()
        val option = compose.onNodeWithTag("favorite_comic_option_curation-test")
        val confirm = compose.onNodeWithTag("favorite_selection_confirm")
        confirm.assertIsNotEnabled()
        option.assertIsNotSelected().performClick().assertIsSelected()
        compose.runOnIdle { assertTrue(settings.value.profileFavoriteComicIds.isEmpty()) }
        compose.onNodeWithTag("favorite_selection_close").performClick()
        compose.onNodeWithTag("favorite_selection_sheet").assertDoesNotExist()
        profile.assertExists()

        firstSlot.performClick()
        option.assertIsNotSelected().performClick()
        confirm.assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals(listOf(comic.id), settings.value.profileFavoriteComicIds)
            assertEquals(comic.displayTitle, ProfileShowcase.restoreComic(settings.value.profileShowcaseDetails.getValue("comic:${comic.id}"))?.displayTitle)
        }

        profile.performScrollToNode(hasText("Editar perfil"))
        compose.onNodeWithText("Editar perfil").performClick()
        val editor = compose.onNodeWithTag("profile_editor")
        editor.performScrollToNode(hasTestTag("profile_editor_favorite_comic_0"))
        val editorSlot = compose.onNodeWithTag("profile_editor_favorite_comic_0")
        editorSlot.performClick()
        val artwork = compose.onNodeWithTag("favorite_artwork_sheet")
        val remove = compose.onNodeWithTag("favorite_artwork_remove")
        val confirmRemoval = compose.onNodeWithTag("favorite_artwork_confirm")
        remove.performScrollTo().performClick()
        confirmRemoval.performScrollTo().assertTextEquals("Confirmar remoção")
        compose.runOnIdle { assertEquals(listOf(comic.id), settings.value.profileFavoriteComicIds) }
        compose.onNodeWithTag("favorite_artwork_close").performScrollTo().performClick()
        artwork.assertDoesNotExist()
        editorSlot.performClick()
        remove.performScrollTo().performClick()
        confirmRemoval.performScrollTo().performClick()
        artwork.assertDoesNotExist()
        editor.assertExists()
        compose.runOnIdle {
            assertTrue(settings.value.profileFavoriteComicIds.isEmpty())
            assertTrue(settings.value.profileShowcaseDetails.isEmpty())
        }
    }

    @Test fun characterDraftRetainsTheSelectedResultWhenSearchChanges() {
        val current = CharacterResult("batman", "Batman", "Batman", "", emptyList())
        val candidate = CharacterResult(
            "robin", "Robin (Dick Grayson)", "Robin", "https://example.test/robin.png", emptyList()
        )
        val query = mutableStateOf("")
        var recorded: FavoriteChoice? = null
        compose.setContent {
            QuadrinheiroTheme(AppSettings(reduceMotion = true, reduceTransparency = true)) {
                FavoriteSelectionSheet(
                    characters = true,
                    query = query.value,
                    onQueryChange = { query.value = it },
                    currentSelection = FavoriteChoice(current.name, current.name),
                    characterLoading = false,
                    characterError = false,
                    characterResults = if (query.value.isEmpty()) listOf(current, candidate) else emptyList(),
                    comicLoading = false,
                    shownComics = emptyList(),
                    onConfirm = { recorded = it },
                    onClose = {}
                )
            }
        }

        compose.onNodeWithTag("favorite_character_option_batman").assertIsSelected()
        compose.onNodeWithTag("favorite_character_option_robin").assertIsNotSelected().performClick()
        compose.onNode(hasSetTextAction()).performTextInput("outro nome")
        compose.onNodeWithTag("favorite_character_option_robin").assertDoesNotExist()
        compose.onNodeWithTag("favorite_selection_status").assertTextEquals("Seleção: ${candidate.name}")
        compose.runOnIdle { assertNull(recorded) }
        compose.onNodeWithTag("favorite_selection_confirm").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(candidate.name, recorded?.value)
            val saved = JSONObject(checkNotNull(recorded?.snapshot))
            assertEquals(candidate.id, saved.getString("id"))
            assertEquals(candidate.image, saved.getString("image"))
        }
    }
}
