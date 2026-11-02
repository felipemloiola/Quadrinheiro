package com.example.macrobenchmark

import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

internal const val TARGET_PACKAGE = "com.aistudio.quadrinheiro.lqglas"

private fun UiDevice.clickTag(tag: String, timeout: Long = 10_000) {
    check(wait(Until.hasObject(By.res(tag)), timeout)) { "Missing journey target: $tag" }
    findObject(By.res(tag)).click()
    waitForIdle()
}

internal fun UiDevice.enterGuestLibrary() {
    // A fresh install may show two welcome pages, followed by authentication.
    wait(Until.hasObject(By.pkg(TARGET_PACKAGE).depth(0)), 10_000)
    for (tag in listOf("welcome_start_button", "welcome_primary_button", "auth_guest_button")) {
        if (wait(Until.hasObject(By.res(tag)), 1500)) clickTag(tag)
    }
    clickTag("nav_shelf")
    check(wait(Until.hasObject(By.res("shelf_list")), 10_000))
}

internal fun UiDevice.exerciseLibraryAndSettings() {
    val shelf = findObject(By.res("shelf_list")) ?: error("Library is not visible")
    shelf.setGestureMargin(displayWidth / 5)
    repeat(3) { shelf.swipe(Direction.UP, 0.65f); shelf.swipe(Direction.DOWN, 0.65f) }
    clickTag("nav_settings")
    check(wait(Until.hasObject(By.res("settings_list")), 10_000))
    val settings = findObject(By.res("settings_list"))
    settings.setGestureMargin(displayWidth / 5)
    repeat(2) { settings.swipe(Direction.UP, 0.65f); settings.swipe(Direction.DOWN, 0.65f) }
    clickTag("nav_shelf")
}
