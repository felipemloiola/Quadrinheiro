package com.example

import android.graphics.Bitmap
import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.SafeStorage
import com.example.model.AppSettings
import com.example.updates.InAppUpdateDownload
import com.example.viewmodel.QuadrinheiroViewModel
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.ExternalResource
import java.io.File
import kotlin.math.abs

class SettingsAppearanceTest {
    @get:Rule(order = 0) val preset = object : ExternalResource() {
        override fun before() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            InAppUpdateDownload.load(context)?.let { InAppUpdateDownload.cancel(context, it) }
            SafeStorage(context).apply { setCompletedWelcomeIntro(true); setGuestMode(true) }
        }
    }
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var vm: QuadrinheiroViewModel
    @Before fun settingsRoot() {
        vm = ViewModelProvider(compose.activity)[QuadrinheiroViewModel::class.java]
        compose.runOnIdle { vm.updateSettings { AppSettings(reduceMotion = true) }; vm.switchMainTab("settings") }
    }
    private fun capture(name: String) {
        val bmp = compose.onRoot().captureToImage().asAndroidBitmap()
        File(compose.activity.getExternalFilesDir(null), name).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun settingsNamesFitOneLine() {
        val list = compose.onNodeWithTag("settings_list", useUnmergedTree = true)
        listOf("Acessibilidade", "Biblioteca", "Armazenamento").forEach { title ->
            list.performScrollToNode(hasText(title))
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNode(hasText(title) and hasAnyAncestor(hasTestTag("settings_list")), useUnmergedTree = true)
                .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertEquals(1, layouts.single().lineCount)
            assertFalse("Title is truncated: $title", layouts.single().isLineEllipsized(0))
        }
        capture("revision46-settings.png")
    }
    @Test fun brandKeepsSmallFrameAndUpdatesFollowItsDescription() {
        compose.onNodeWithTag("settings_list", true).performScrollToNode(hasTestTag("settings_update_button"))
        val frame = compose.onNodeWithTag("settings_footer_icon", true).getUnclippedBoundsInRoot()
        assertTrue(abs((frame.right - frame.left).value - 42f) < 1f)
        val mark = compose.onNodeWithTag("settings_footer_mark", true).getUnclippedBoundsInRoot()
        assertTrue((mark.right - mark.left).value < 35f)
        val description = compose.onNodeWithText("Catálogo, Diário Cronológico & Guia de Leitura").getUnclippedBoundsInRoot()
        val update = compose.onNodeWithTag("settings_update_button").getUnclippedBoundsInRoot()
        assertTrue(update.top > description.bottom)
        capture("revision46-footer.png")
    }
    @Test fun backButtonKeepsItsShapeAndRemainsAvailableDuringScroll() {
        compose.runOnIdle { vm.openSettingsCategory("appearance") }
        val back = compose.onNodeWithTag("settings_back_button")
        val before = back.getUnclippedBoundsInRoot()
        val list = compose.onNodeWithTag("settings_list", true)
        list.performTouchInput { swipeUp() }
        back.assertIsDisplayed().assertHasClickAction()
        assertEquals(before, back.getUnclippedBoundsInRoot())
        back.performClick()
        compose.onNodeWithTag("settings_back_button").assertDoesNotExist()
    }
    @Test fun glassSliderChangesThePreviewAndTheStoredLevel() {
        compose.runOnIdle { vm.updateSettings { it.copy(glassLevel = 0) }; vm.openSettingsCategory("liquidGlass", "appearance") }
        val preview = compose.onNodeWithTag("glass_level_preview", true)
        val before = preview.captureToImage().asAndroidBitmap()
        compose.onNodeWithTag("glass_level_slider", true).performTouchInput { click(Offset(width - 2f, centerY)) }
        compose.runOnIdle { assertTrue(vm.appSettings.value.glassLevel >= 95) }
        assertFalse(before.sameAs(preview.captureToImage().asAndroidBitmap()))
        capture("revision46-glass.png")
    }
    @Test fun iconChoiceChangesOnlyTheLauncherAliasWithoutClosingTheApp() {
        compose.runOnIdle { vm.openSettingsCategory("appearance") }
        compose.onNodeWithTag("settings_list", true).performScrollToNode(hasTestTag("launcher-icon-orange"))
        compose.onNodeWithTag("launcher-icon-orange").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick) { it() }
        compose.waitUntil(5000) { LauncherAppearance.selected(compose.activity) == LauncherIconStyle.ORANGE &&
            compose.activity.packageManager.getComponentEnabledSetting(ComponentName(compose.activity.packageName, LauncherIconStyle.ORANGE.alias)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED }
        compose.onNodeWithTag("launcher-icon-orange").assertIsSelected()
        capture("revision46-icons.png")
        LauncherAppearance.select(compose.activity, LauncherIconStyle.DEFAULT)
    }
}
