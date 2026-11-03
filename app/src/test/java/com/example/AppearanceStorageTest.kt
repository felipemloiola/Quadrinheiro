package com.example

import android.content.Context
import android.content.pm.PackageManager
import android.content.ComponentName
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.model.AppSettings
import com.example.ui.theme.withGlassAppearance
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppearanceStorageTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun glassLevelChangesRealMaterialParametersInBothStyles() {
        for (style in listOf("regular", "frosted")) {
            val low = AppSettings(glassLevel = 0, glassVariant = style).withGlassAppearance()
            val high = AppSettings(glassLevel = 100, glassVariant = style).withGlassAppearance()
            assertTrue(high.glassOpacity > low.glassOpacity)
            assertTrue(high.glassBlurIntensity > low.glassBlurIntensity)
            assertTrue(high.glassIntensity < low.glassIntensity)
        }
        assertEquals(AppSettings(), AppSettings().withGlassAppearance())
    }

    @Test fun eachLauncherChoiceEnablesExactlyOneAliasAndDefaultTracksNight() {
        val names = listOf(LauncherAppearance.DAY, LauncherAppearance.NIGHT) + LauncherIconStyle.entries.filter { it != LauncherIconStyle.DEFAULT }.map { it.alias }
        LauncherIconStyle.entries.forEach { style ->
            LauncherAppearance.select(context, style)
            assertEquals(style, LauncherAppearance.selected(context))
            val active = names.filter { context.packageManager.getComponentEnabledSetting(ComponentName(context.packageName, it)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED }
            assertEquals(1, active.size)
            if (style != LauncherIconStyle.DEFAULT) assertEquals(style.alias, active.single())
        }
        LauncherAppearance.select(context, LauncherIconStyle.DEFAULT)
        val night = context.createConfigurationContext(Configuration(context.resources.configuration).apply { uiMode = Configuration.UI_MODE_NIGHT_YES })
        LauncherAppearance.sync(night)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, context.packageManager.getComponentEnabledSetting(ComponentName(context.packageName, LauncherAppearance.NIGHT)))
    }

    @Test fun recoverableCleanupPreservesLibraryAndAccountData() {
        val storage = SafeStorage(context)
        val library = "{\"comics\":[{\"id\":\"canary\"}]}"
        storage.setItem(SafeStorage.GUEST_STATE_KEY, library)
        storage.setItem(SafeStorage.ACCOUNT_STATE_PREFIX + "canary", library)
        storage.setItem(SafeStorage.TRANSLATION_CACHE_KEY, "{\"temporary\":{\"titlePtBr\":\"Cópia\"}}")
        val backup = File(context.filesDir, "backup-canary.json").apply { writeText(library) }
        StorageMaintenance.clearRecoverable(context)
        assertNull(storage.getItem(SafeStorage.TRANSLATION_CACHE_KEY))
        assertEquals(library, storage.getItem(SafeStorage.GUEST_STATE_KEY))
        assertEquals(library, storage.getItem(SafeStorage.ACCOUNT_STATE_PREFIX + "canary"))
        assertEquals(library, backup.readText())
    }

    @Test fun storageMeasurementCountsNestedFilesOnce() {
        val root = File(context.cacheDir, "measure-fixture").apply { mkdirs() }
        root.resolve("one").writeBytes(ByteArray(1024))
        root.resolve("nested").mkdirs()
        root.resolve("nested/two").writeBytes(ByteArray(2048))
        assertEquals(3072L, StorageMaintenance.bytes(root))
    }
}
