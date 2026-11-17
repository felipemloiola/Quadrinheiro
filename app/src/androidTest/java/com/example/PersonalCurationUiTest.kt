package com.example

import androidx.compose.runtime.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.*
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.QuadrinheiroTheme
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersonalCurationUiTest {
    @get:Rule val compose=createComposeRule()
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private lateinit var storage:CurationStorage
    private lateinit var original:CurationSnapshot
    private var originalDrafts:List<ComicList> = emptyList()
    private lateinit var controller:CurationController
    private val library=listOf("Batman: Ano Um","Superman: Quatro Estações").mapIndexed { index,title ->
        ComicEntry("curation-fixture-$index","",title,authors="Autor de teste",publisher="DC",pageCount=100,editionLanguage="pt")
    }
    @Before fun setup() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        storage=CurationStorage(context);original=storage.load("guest_local");originalDrafts=storage.drafts("guest_local")
        storage.save("guest_local",CurationSnapshot());storage.clearDraft("guest_local")
        controller=CurationController(context,scope,{"guest_local"},{})
        compose.runOnIdle {controller.load()}
        compose.waitUntil(10000) {controller.state.value.owner=="guest_local" && !controller.state.value.loading}
    }
    @After fun cleanup() {
        scope.cancel();storage.save("guest_local",original);storage.clearDraft("guest_local")
        originalDrafts.forEach {storage.saveDraft("guest_local",it)}
    }
    @Test fun listEditorSavesRankingNotesAndOrderWithoutChangingTheLibrary() {
        var overview by mutableStateOf(false)
        val unrelated=ComicList(name="Outro rascunho preservado")
        storage.saveDraft("guest_local",unrelated)
        val list=ComicList(name="Meu ranking",ranked=true,items=library.map(ListComic::from))
        compose.setContent {
            QuadrinheiroTheme(settings=AppSettings(colorScheme="dark",reduceMotion=true,reduceTransparency=true)) {
                Box(Modifier.fillMaxSize().background(com.example.ui.theme.LocalIOS27Theme.current.bgPrimary)) {
                if(overview) PersonalListsScreen(library, controller, {}, {}, {})
                else ListEditor(list,library,controller,null,{}) {overview=true}
                }
            }
        }
        val firstOptions="list_item_options_${list.items.first().key}"
        compose.onNodeWithTag("list_editor_items").performScrollToNode(hasTestTag(firstOptions))
        compose.onNodeWithTag(firstOptions).performClick()
        compose.onNodeWithText("Editar nota").performClick()
        compose.onNode(hasSetTextAction() and hasText("Sua nota")).performTextInput("Minha primeira escolha")
        compose.onNodeWithText("Concluir").performClick()
        compose.onNodeWithTag(firstOptions).performClick()
        compose.onNodeWithText("Mover").performClick()
        compose.onNode(hasSetTextAction() and hasText("Posição de 1 a 2")).performTextReplacement("2")
        compose.onAllNodesWithText("Mover").onLast().performClick()
        compose.onNodeWithText("Salvar lista").performClick()
        compose.waitUntil(10000) {controller.state.value.lists.any {it.id==list.id}}
        val saved=storage.load("guest_local").lists.single()
        assertEquals(library.last().displayTitle,saved.items.first().title)
        assertEquals("Minha primeira escolha",saved.items.last().note)
        assertTrue(storage.drafts("guest_local").any {it.id==unrelated.id})
        assertTrue(saved.ranked);assertTrue(library.all {it.currentPage==0 && !it.favorite})
        compose.onNodeWithContentDescription("Explorar listas públicas").assertDoesNotExist()
        compose.onNodeWithText("Meu ranking").performClick()
        compose.onNodeWithText("Minha primeira escolha").assertIsDisplayed()
        val bitmap=checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,"curation-list.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
        }
        compose.onNodeWithText(library.last().displayTitle).performClick()
        compose.onNodeWithTag("comic_detail_page").assertIsDisplayed()
        compose.onNode(hasContentDescription("Voltar") and hasText("Voltar")).performClick()
        compose.onNodeWithText("Minha primeira escolha").assertIsDisplayed()
    }
    @Test fun explanationsUseSavedEvidenceAndFeedbackCanBeUndone() {
        val c=library.first()
        val rec=RecommendedComic(c,"Com seu favorito: Batman",110,"FAVORITE_CHARACTER",
            RecommendationReason("FAVORITE_CHARACTER","Batman","Os metadados desta HQ mencionam Batman, um dos seus personagens favoritos."))
        compose.setContent {QuadrinheiroTheme(settings=AppSettings(reduceMotion=true,reduceTransparency=true,dynamicTypeScale=115)) {
            RecommendationExplanationAction(rec,controller,{})
        }}
        compose.onNodeWithText("Por que esta HQ?").performClick()
        compose.onNodeWithText(rec.explanation!!.detail).assertIsDisplayed()
        compose.onNodeWithText("Não tenho interesse").performClick()
        compose.waitUntil(10000) {controller.state.value.feedback.hidden.size==1}
        var hydrated=false
        compose.runOnIdle {scope.launch {
            val cold=CurationController(InstrumentationRegistry.getInstrumentation().targetContext,scope,{"guest_local"},{})
            cold.awaitLocal()
            assertTrue(cold.isReady("guest_local"));assertTrue(cold.state.value.feedback.hides(c));hydrated=true
        }}
        compose.waitUntil(10000) {hydrated}
        compose.onNodeWithText("Desfazer preferência").performClick()
        compose.waitUntil(10000) {controller.state.value.feedback.hidden.isEmpty() && !controller.state.value.saving}
        assertTrue(storage.load("guest_local").feedback.hidden.isEmpty())
        assertFalse(c.favorite)
    }
    @Test fun longPressDragReordersAndAccountSwitchRejectsLateSave() {
        val list=ComicList(name="Arraste de teste",items=library.map(ListComic::from))
        compose.setContent {QuadrinheiroTheme(settings=AppSettings(reduceMotion=true,reduceTransparency=true)) {
            ListEditor(list,library,controller,null,{}) {}
        }}
        val description="Reordenar ${library.first().displayTitle}"
        compose.onNodeWithTag("list_editor_items").performScrollToNode(hasContentDescription(description))
        compose.onNodeWithContentDescription(description).performTouchInput {
            down(center);advanceEventTime(700);moveBy(androidx.compose.ui.geometry.Offset(0f,650f));up()
        }
        compose.onNodeWithText("Salvar lista").performClick()
        compose.waitUntil(10000) {controller.state.value.lists.any {it.id==list.id}}
        assertEquals(library.last().displayTitle,controller.state.value.lists.single().items.first().title)
        var owner="guest_local"
        val isolated=CurationController(InstrumentationRegistry.getInstrumentation().targetContext,scope,{owner},{})
        compose.runOnIdle {isolated.load()}
        compose.waitUntil(10000) {!isolated.state.value.loading && isolated.state.value.lists.isNotEmpty()}
        compose.runOnIdle {owner="other-account";isolated.activate()}
        var rejected=false
        compose.runOnIdle {scope.launch {rejected=!isolated.save(list,"guest_local")}}
        compose.waitUntil(5000) {rejected}
        assertTrue(isolated.state.value.lists.isEmpty())
        assertTrue(storage.load("other-account").lists.isEmpty())
    }
    @Test fun catalogItemCanJoinAnUnsavedListWithoutRegisteringAReading() {
        val pending=ComicList(name="Lista do catálogo",items=listOf(ListComic.from(library.first())))
        storage.saveDraft("guest_local",pending)
        compose.setContent {QuadrinheiroTheme(settings=AppSettings(reduceMotion=true,reduceTransparency=true)) {
            AddToListAction(library.last(),controller)
        }}
        compose.onNodeWithText("Adicionar a uma lista").performClick()
        compose.waitUntil(10000) {compose.onAllNodesWithText("Continuar rascunho: Lista do catálogo").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Continuar rascunho: Lista do catálogo").performClick()
        compose.onNodeWithText("Salvar lista").performClick()
        compose.waitUntil(10000) {controller.state.value.lists.any {it.id==pending.id}}
        assertEquals(2,storage.load("guest_local").lists.single().items.size)
        assertTrue(library.all {it.currentPage==0 && !it.favorite})
    }
    @Test fun selectingSeveralLibraryComicsIsAppliedOnlyOnConfirmation() {
        val list=ComicList(name="Seleção em lote")
        compose.setContent {QuadrinheiroTheme(settings=AppSettings(reduceMotion=true,reduceTransparency=true)) {
            ListEditor(list,library,controller,null,{}) {}
        }}
        compose.onNodeWithTag("list_editor_items").performScrollToNode(hasText("Adicionar HQs"))
        compose.onNodeWithText("Adicionar HQs").performClick()
        compose.onNodeWithText(library.first().displayTitle).performClick()
        compose.onNodeWithText(library.last().displayTitle).performClick()
        assertTrue(controller.state.value.lists.isEmpty())
        compose.onNodeWithText("Adicionar (2)").performClick()
        compose.onNodeWithText("Salvar lista").performClick()
        compose.waitUntil(10000) {controller.state.value.lists.any {it.id==list.id}}
        assertEquals(2,storage.load("guest_local").lists.single().items.size)
        assertTrue(library.all {it.currentPage==0})
    }
    @Test fun libraryAndCatalogSelectionsReturnToTheSameListDraft() {
        val catalogComic = library.last().copy(id = "catalog-only", title = "HQ do catálogo", isOnlineResult = true)
        val list = ComicList(name = "Estante e catálogo", description = "Descrição do rascunho")
        compose.runOnIdle { scope.launch { controller.save(list) } }
        compose.waitUntil(10000) { controller.state.value.lists.any { it.id == list.id } }
        compose.setContent {
            QuadrinheiroTheme(settings = AppSettings(reduceMotion = true, reduceTransparency = true)) {
                PersonalListsScreen(library, controller, {}, {}, {}, searchCatalog = { query ->
                    kotlinx.coroutines.flow.flowOf(if (query == "catálogo") listOf(catalogComic) else emptyList())
                })
            }
        }
        compose.onNode(hasClickAction() and hasText(list.name)).performClick()
        compose.onNodeWithText("Adicionar HQs").performClick()
        compose.onNodeWithText(library.first().displayTitle).performClick()
        compose.onNodeWithText("Catálogo").performClick()
        compose.onNode(hasSetTextAction() and hasContentDescription("Buscar no catálogo")).performTextInput("catálogo")
        compose.waitUntil(5000) { compose.onAllNodesWithText(catalogComic.displayTitle).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(catalogComic.displayTitle).performClick()
        compose.onNodeWithText("Minha estante").performClick()
        compose.onNodeWithText("Adicionar (2)").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("Minha estante").fetchSemanticsNodes().isEmpty() }
        assertTrue(storage.load("guest_local").lists.single().items.isEmpty())
        compose.onNodeWithText("Salvar lista").performClick()
        compose.waitUntil(10000) { controller.state.value.lists.single().items.size == 2 }
        val saved = storage.load("guest_local").lists.single()
        assertEquals(list.description, saved.description)
        assertEquals(listOf(ListComic.from(library.first()).key, ListComic.from(catalogComic).key), saved.items.map { it.key })
        compose.waitUntil(5000) { compose.onAllNodesWithText("Salvar lista").fetchSemanticsNodes().isEmpty() }
        compose.onNode(hasClickAction() and hasText(catalogComic.displayTitle)).assertIsDisplayed()
    }
    @Test fun focusEditorKeepsArtworkChangesInTheDraftUntilConfirmed() {
        val items = library.map(ListComic::from).map {
            it.copy(cover = "android.resource://android/${android.R.drawable.ic_menu_gallery}")
        }
        val withoutCover = ListComic.from(library.first().copy(id = "without-cover"))
        val list = ComicList(name = "Capa ajustável", items = items + withoutCover)
        compose.runOnIdle { scope.launch { controller.save(list) } }
        compose.waitUntil(10000) { controller.state.value.lists.any { it.id == list.id } }
        compose.setContent {QuadrinheiroTheme(settings=AppSettings(reduceMotion=true,reduceTransparency=true)) {
            PersonalListsScreen(library, controller, {}, {}, {})
        }}
        compose.onNode(hasClickAction() and hasText(list.name)).performClick()
        compose.onNodeWithContentDescription("Opções da lista").performClick()
        compose.onNodeWithText("Escolher capa").performClick()
        compose.onNodeWithTag("list_cover_choice_${withoutCover.key}").assertDoesNotExist()
        compose.onNodeWithTag("list_cover_choice_${items.last().key}").performClick()
        compose.onNodeWithText("Ajustar capa").assertIsDisplayed()
        compose.onNodeWithContentDescription("Enquadramento da capa.",substring=true).performTouchInput {
            down(0,androidx.compose.ui.geometry.Offset(centerX-25,centerY))
            down(1,androidx.compose.ui.geometry.Offset(centerX+25,centerY))
            moveTo(0,androidx.compose.ui.geometry.Offset(centerX-70,centerY-20),delayMillis=120)
            moveTo(1,androidx.compose.ui.geometry.Offset(centerX+70,centerY-20),delayMillis=120)
            up(0);up(1)
        }
        assertEquals("", storage.load("guest_local").lists.single().coverKey)
        compose.onNodeWithText("Usar capa").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("Ajustar capa").fetchSemanticsNodes().isEmpty() }
        assertEquals("", storage.load("guest_local").lists.single().coverKey)
        compose.onNodeWithText("Salvar lista").performClick()
        compose.waitUntil(10000) { controller.state.value.lists.single().coverKey == items.last().key }
        val saved = storage.load("guest_local").lists.single()
        assertEquals(items.last().key, saved.coverKey)
        assertTrue(saved.coverZoom > 1f)
        assertEquals(items + withoutCover, saved.items)
    }
    @Test fun singleBookFeaturedListStaysCompact() {
        val list=ComicList(name="Uma HQ",items=listOf(ListComic.from(library.first())))
        compose.setContent {QuadrinheiroTheme(settings=AppSettings(reduceMotion=true,reduceTransparency=true)) {
            FeaturedListCard(list,{})
        }}
        val bounds=compose.onNodeWithText("Uma HQ").fetchSemanticsNode().boundsInRoot
        val density=InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
        assertTrue("Card de uma HQ não pode ocupar toda a tela",bounds.height/density<400f)
    }
    @Test fun unavailableAccountCannotOfferAnApparentlySuccessfulSave() {
        val unavailable=CurationController(InstrumentationRegistry.getInstrumentation().targetContext,scope,{"local_default"},{})
        compose.runOnIdle {unavailable.load()}
        compose.setContent {QuadrinheiroTheme(settings=AppSettings(colorScheme="light",reduceMotion=true,reduceTransparency=true)) {
            ListEditor(ComicList(name="Rascunho preservado"),library,unavailable,null,{}) {}
        }}
        compose.onNodeWithText("Salvar lista").assertIsNotEnabled()
        compose.onNodeWithText("Recarregar versão salva · preservar rascunho").assertDoesNotExist()
    }
    @Test fun localEmailAccountSavesReloadsAndNeverLeaksIntoAnotherAccount() {
        val uid="email-curation-fixture"
        val before=storage.load(uid)
        val beforeDrafts=storage.drafts(uid)
        var owner=uid
        val local=CurationController(InstrumentationRegistry.getInstrumentation().targetContext,scope,{owner},{},isLocalOwner={it==uid})
        try {
            compose.runOnIdle {local.load()}
            compose.waitUntil(10000) {local.state.value.ready}
            assertTrue(local.state.value.localOnly)
            var saved=false
            val list=ComicList(name="Lista da conta por e-mail",items=library.map(ListComic::from))
            compose.runOnIdle {scope.launch {saved=local.save(list)}}
            compose.waitUntil(10000) {saved}
            val cold=CurationController(InstrumentationRegistry.getInstrumentation().targetContext,scope,{owner},{},isLocalOwner={it==uid})
            compose.runOnIdle {cold.load()}
            compose.waitUntil(10000) {cold.state.value.ready}
            assertTrue(cold.state.value.lists.any {it.id==list.id && it.items.size==2})
            compose.runOnIdle {owner="other-account";local.activate()}
            var rejected=false
            compose.runOnIdle {scope.launch {rejected=!local.save(list,uid)}}
            compose.waitUntil(10000) {rejected}
            assertFalse(local.state.value.localOnly)
            assertFalse(local.state.value.lists.any {it.id==list.id})
        } finally {
            storage.save(uid,before);storage.clearDraft(uid);beforeDrafts.forEach {storage.saveDraft(uid,it)}
        }
    }
    @Test fun deletedListsDoNotConsumeTheActiveListQuota() {
        storage.save("guest_local",CurationSnapshot(List(101) {ComicList(name="Lista excluída",deleted=true,revision=1)}))
        compose.runOnIdle {controller.load(true)}
        compose.waitUntil(10000) {controller.state.value.lists.size==101 && !controller.state.value.loading}
        var saved=false
        compose.runOnIdle {scope.launch {saved=controller.save(ComicList(name="Nova lista"))}}
        compose.waitUntil(10000) {saved}
        assertEquals(1,storage.load("guest_local").lists.count {!it.deleted})
    }
    @Test fun saveFinishesIfTheEditorCoroutineIsCancelled() {
        val list=ComicList(name="Salvar mesmo ao fechar a tela")
        compose.runOnIdle {
            val editor=scope.launch {controller.save(list)}
            editor.cancel()
        }
        compose.waitUntil(10000) {controller.state.value.lists.any {it.id==list.id} && !controller.state.value.saving}
        assertTrue(storage.load("guest_local").lists.any {it.id==list.id})
    }
}
