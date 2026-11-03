package com.example

import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.SafeStorage
import com.example.model.*
import com.example.viewmodel.QuadrinheiroViewModel
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RapidAppStressTest {
    @Test fun actualAppSurvivesRapidTabsAndNestedSettings() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val storage = SafeStorage(instrumentation.targetContext)
        storage.setCompletedWelcomeIntro(true)
        storage.setGuestMode(true)
        storage.saveState((1..12).map { ComicEntry("stress-$it", "", "História de teste $it", authors = "Autor", publisher = "Editora", status = if (it == 1) ReadingStatus.READING else ReadingStatus.READ) }, ReaderProfile(), AppSettings(), ownerUid = "guest_local")
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                SystemClock.sleep(800)
                repeat(64) { index ->
                    scenario.onActivity { activity ->
                        val vm = ViewModelProvider(activity)[QuadrinheiroViewModel::class.java]
                        val tab = listOf("home", "explore", "profile", "shelf", "search", "settings")[index % 6]
                        vm.switchMainTab(tab)
                        if (tab == "settings") vm.openSettingsCategory(listOf("appearance", "liquidGlass", "accessibility")[index % 3])
                    }
                    SystemClock.sleep(40)
                }
                instrumentation.waitForIdleSync()
                assertEquals(Lifecycle.State.RESUMED, scenario.state)
            }
        } finally { instrumentation.runOnMainSync { com.kyant.backdrop.BackdropFirstFrameGate.firstFrameReady = true } }
    }
}
