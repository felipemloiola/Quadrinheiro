package com.example

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LauncherThemeTest {
    @Test fun switchingAppearanceEnablesOnlyTheMatchingLauncherAlias() {
        val app: Context = ApplicationProvider.getApplicationContext()
        for (mode in listOf(Configuration.UI_MODE_NIGHT_YES, Configuration.UI_MODE_NIGHT_NO)) {
            val config = Configuration(app.resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or mode
            }
            LauncherAppearance.sync(app.createConfigurationContext(config))
            val dark = mode == Configuration.UI_MODE_NIGHT_YES
            val pm = app.packageManager
            fun state(name: String) = pm.getComponentEnabledSetting(android.content.ComponentName(app.packageName, name))
            assertEquals(android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED, state(if (dark) LauncherAppearance.NIGHT else LauncherAppearance.DAY))
            assertEquals(android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED, state(if (dark) LauncherAppearance.DAY else LauncherAppearance.NIGHT))
        }
    }
    @Test fun launcherBackgroundFollowsSystemNightMode() {
        val app: Context = ApplicationProvider.getApplicationContext()
        fun color(mode: Int): Int {
            val config = Configuration(app.resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or mode
            }
            return app.createConfigurationContext(config).getColor(R.color.ic_launcher_bg_start)
        }
        assertEquals(0xFFF3F4F7.toInt(), color(Configuration.UI_MODE_NIGHT_NO))
        assertEquals(0xFF101116.toInt(), color(Configuration.UI_MODE_NIGHT_YES))
    }
}
