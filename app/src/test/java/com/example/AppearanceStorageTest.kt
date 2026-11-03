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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.Before
import org.junit.After

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AppearanceStorageTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    @Before fun installMainDispatcher() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun restoreMainDispatcher() { Dispatchers.resetMain() }

    @Test fun foregroundIconChangesAreDeferredUntilTheActivityLeaves() {
        LauncherAppearance.foreground = false
        LauncherAppearance.select(context, LauncherIconStyle.DEFAULT)
        LauncherAppearance.sync(context)
        val day = ComponentName(context.packageName, LauncherAppearance.DAY)
        val before = context.packageManager.getComponentEnabledSetting(day)
        try {
            LauncherAppearance.foreground = true
            LauncherAppearance.rememberAccent(context, "ORANGE")
            LauncherAppearance.setFollowsApp(context, true)
            LauncherAppearance.sync(context)
            assertEquals(before, context.packageManager.getComponentEnabledSetting(day))
            LauncherAppearance.foreground = false
            LauncherAppearance.applyWhenBackground(context)
            assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                context.packageManager.getComponentEnabledSetting(ComponentName(context.packageName,
                    LauncherAppearance.aliasFor(LauncherIconStyle.ORANGE, false))))
        } finally { LauncherAppearance.foreground = false }
    }

    @Test fun favoritesPersistInsideTheirAccountAndRoundTripCloudPreferences() {
        val settings = AppSettings(profileFavoriteComicIds = listOf("a", "b", "c", "d"),
            profileFavoriteCharacters = listOf("Batman", "Magali", "Luffy", "Tempestade"))
        val storage = SafeStorage(context)
        storage.saveState(emptyList(), com.example.model.ReaderProfile(), settings, ownerUid = "alice-showcase")
        storage.saveState(emptyList(), com.example.model.ReaderProfile(), AppSettings(), ownerUid = "bob-showcase")
        assertEquals(settings.profileFavoriteComicIds, storage.loadAccountState("alice-showcase")!!.settings.profileFavoriteComicIds)
        assertTrue(storage.loadAccountState("bob-showcase")!!.settings.profileFavoriteCharacters.isEmpty())
        assertEquals(settings, CloudUserPreferences.fromAppSettings(settings).toAppSettings())
    }

    @Test fun cloudShowcaseIsBoundedAndDeduplicated() {
        val cloud = CloudUserPreferences.fromAppSettings(AppSettings(
            profileFavoriteComicIds = listOf("a", "a", "b", "c", "d", "e"),
            profileFavoriteCharacters = listOf("", "Batman", "Batman", "X".repeat(100), "Magali", "Luffy", "Goku")))
        assertEquals(listOf("a", "b", "c", "d"), cloud.profileFavoriteComicIds)
        assertEquals(4, cloud.profileFavoriteCharacters.size)
        assertTrue(cloud.profileFavoriteCharacters.all { it.length in 1..60 })
        assertTrue(cloud.javaClass.declaredFields.count { !it.isSynthetic && it.name != "Companion" } <= 45)
    }

    @Test fun accentChoiceDoesNotTintTheSystemBackground() {
        for (dark in listOf(false, true)) {
            val colors = com.example.ui.theme.AppTheme.entries.map { com.example.ui.theme.getIOS27ThemeColors(it, dark) }
            assertEquals(1, colors.map { it.bgPrimary }.distinct().size)
            assertEquals(1, colors.map { it.bgSecondary }.distinct().size)
            assertTrue(colors.map { it.accent }.distinct().size > 1)
        }
    }

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
        LauncherAppearance.foreground = false
        val names = listOf(LauncherAppearance.DAY, LauncherAppearance.NIGHT) + LauncherIconStyle.entries.filter { it != LauncherIconStyle.DEFAULT }.flatMap { listOf(it.alias, it.alias + "Day", it.alias + "Night") }
        LauncherIconStyle.entries.forEach { style ->
            LauncherAppearance.select(context, style)
            LauncherAppearance.sync(context)
            assertEquals(style, LauncherAppearance.selected(context))
            val active = names.filter { context.packageManager.getComponentEnabledSetting(ComponentName(context.packageName, it)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED }
            assertEquals(1, active.size)
            if (style != LauncherIconStyle.DEFAULT) assertTrue(active.single().startsWith(style.alias))
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
