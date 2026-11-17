package com.example.macrobenchmark

import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiObject2

internal const val TARGET_PACKAGE = "com.aistudio.quadrinheiro.lqglas"

private fun UiDevice.target(selector: BySelector, timeout: Long = 10_000): UiObject2 =
    checkNotNull(wait(Until.findObject(selector), timeout)) { "Missing journey target: $selector" }

private fun UiDevice.clickTarget(selector: BySelector) {
    target(selector).click()
    waitForIdle()
}

private fun UiDevice.finishWelcome() {
    // A fresh install may show two welcome pages, followed by authentication.
    wait(Until.hasObject(By.pkg(TARGET_PACKAGE).depth(0)), 10_000)
    for (tag in listOf("welcome_start_button", "welcome_primary_button", "auth_guest_button")) {
        if (wait(Until.hasObject(By.res(tag)), 1500)) clickTarget(By.res(tag))
    }
}

private fun UiDevice.openLibraryFromProfile() {
    clickTarget(By.desc("Perfil"))
    val profile = target(By.res("reader_profile"))
    repeat(6) {
        if (hasObject(By.text("Biblioteca"))) {
            clickTarget(By.text("Biblioteca"))
            return
        }
        profile.swipe(Direction.UP, .65f)
        waitForIdle()
    }
    clickTarget(By.text("Biblioteca"))
}

internal fun UiDevice.enterGuestLibrary() {
    finishWelcome()
    openLibraryFromProfile()
    if (!hasObject(By.res("shelf_list"))) clickTarget(By.text("Estante"))
    target(By.res("shelf_list"))
}

internal fun UiDevice.exerciseLibraryAndSettings() {
    val shelf = findObject(By.res("shelf_list")) ?: error("Library is not visible")
    shelf.setGestureMargin(displayWidth / 5)
    repeat(3) { shelf.swipe(Direction.UP, 0.65f); shelf.swipe(Direction.DOWN, 0.65f) }
    pressBack()
    clickTarget(By.res("profile_settings"))
    val settings = target(By.res("settings_list"))
    settings.setGestureMargin(displayWidth / 5)
    repeat(2) { settings.swipe(Direction.UP, 0.65f); settings.swipe(Direction.DOWN, 0.65f) }
    pressBack()
    openLibraryFromProfile()
}

internal fun UiDevice.openListForBenchmark(name: String) {
    enterGuestLibrary()
    clickTarget(By.text("Listas"))
    val lists = target(By.res("lists_page"))
    repeat(6) {
        if (hasObject(By.text(name))) return@repeat
        lists.swipe(Direction.UP, .65f)
        waitForIdle()
    }
    clickTarget(By.text(name))
    target(By.desc("Opções da lista"))
}

internal fun UiDevice.exerciseListMenu() {
    clickTarget(By.desc("Opções da lista"))
    target(By.text("Destacar na Biblioteca"))
    pressBack()
    target(By.desc("Opções da lista"))
}

internal fun UiDevice.enterExplore() {
    finishWelcome()
    clickTarget(By.desc("Explorar"))
    if (!hasObject(By.res("explore_page"))) clickTarget(By.text("Descobrir"))
    val page = target(By.res("explore_page"))
    repeat(5) {
        if (wait(Until.hasObject(By.res("explore_poster")), 2500)) return
        page.swipe(Direction.UP, .55f)
        waitForIdle()
    }
    target(By.res("explore_poster"), timeout = 20_000)
}

internal fun UiDevice.exerciseExploreDetailReturn() {
    val poster = target(By.res("explore_poster"))
    val description = checkNotNull(poster.contentDescription)
    val top = poster.visibleBounds.top
    poster.click()
    clickTarget(By.res("open_comic_page"))
    target(By.desc("Voltar"))
    pressBack()
    clickTarget(By.res("synopsis_close_button"))
    val returned = target(By.res("explore_poster").desc(description))
    check(kotlin.math.abs(returned.visibleBounds.top - top) <= 2) { "Explore scroll position changed after returning" }
}
