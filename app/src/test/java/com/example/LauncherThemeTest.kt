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
