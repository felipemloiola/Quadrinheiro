package com.example

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.work.*
import java.util.concurrent.TimeUnit

object LauncherAppearance {
    @Volatile var foreground: Boolean = false
    internal const val DAY = "com.example.LauncherDay"
    internal const val NIGHT = "com.example.LauncherNight"
    private const val PREFS = "launcher_appearance"
    fun followsApp(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("follow_app", false)
    fun selectedMode(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("launcher_mode", if (followsApp(context)) "theme" else "original") ?: "original"

    fun selected(context: Context): LauncherIconStyle {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val mode = prefs.getString("launcher_mode", if (prefs.getBoolean("follow_app", false)) "theme" else "original") ?: "original"
        return when (mode) {
            "original", "default" -> LauncherIconStyle.DEFAULT
            "theme" -> {
                val themeId = prefs.getString("theme_id", "SYSTEM_MONO")
                LauncherIconStyle.fromThemeId(themeId)
            }
            else -> LauncherIconStyle.fromId(mode)
        }
    }

    fun setFollowsApp(context: Context, follow: Boolean) {
        val mode = if (follow) "theme" else "original"
        setLauncherMode(context, mode)
    }

    fun setLauncherMode(context: Context, mode: String, themeId: String? = null) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val editor = prefs.edit()
            .putString("launcher_mode", mode)
            .putBoolean("follow_app", mode != "original" && mode != "default")
        if (!themeId.isNullOrBlank()) {
            editor.putString("theme_id", themeId)
            val mappedStyle = LauncherIconStyle.fromThemeId(themeId)
            editor.putString("accent", mappedStyle.id)
        } else if (mode != "theme" && mode != "original" && mode != "default") {
            editor.putString("accent", mode)
        }
        editor.apply()
        scheduleBackgroundSync(context)
    }

    fun rememberAccent(context: Context, themeId: String) {
        val mappedStyle = LauncherIconStyle.fromThemeId(themeId)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("theme_id", themeId)
            .putString("accent", mappedStyle.id)
            .apply()
        if (selectedMode(context) == "theme") {
            scheduleBackgroundSync(context)
        }
    }

    fun applyWhenBackground(context: Context) {
        if (!foreground) runCatching { sync(context.applicationContext) }
    }
    fun scheduleBackgroundSync(context: Context) {
        runCatching { WorkManager.getInstance(context).enqueueUniqueWork("pending-launcher-appearance",
            ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<LauncherAppearanceWorker>()
                .setInitialDelay(5, TimeUnit.SECONDS).build()) }
    }
    fun select(context: Context, style: LauncherIconStyle) {
        val mode = if (style == LauncherIconStyle.DEFAULT) "original" else style.id
        setLauncherMode(context, mode)
    }
    internal fun aliasFor(style: LauncherIconStyle, night: Boolean) = if (style == LauncherIconStyle.DEFAULT) { if (night) NIGHT else DAY } else style.alias + if (night) "Night" else "Day"
    @Synchronized
    fun sync(context: Context) {
        if (foreground) return
        val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val pm = context.packageManager
        val desired = aliasFor(selected(context), night)
        val names = listOf(desired) + (listOf(DAY, NIGHT) + LauncherIconStyle.entries.filter { it != LauncherIconStyle.DEFAULT }.flatMap { listOf(it.alias, it.alias + "Day", it.alias + "Night") }).filter { it != desired }
        val changes = names.mapNotNull { name ->
            val component = ComponentName(context.packageName, name)
            val state = if (name == desired) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            if (pm.getComponentEnabledSetting(component) == state) null else component to state
        }
        if (Build.VERSION.SDK_INT >= 33) {
            if (changes.isNotEmpty()) pm.setComponentEnabledSettings(changes.map { (component, state) -> PackageManager.ComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP) })
        } else changes.forEach { (component, state) -> pm.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP) }
    }
    fun schedule(context: Context) {
        val manager = WorkManager.getInstance(context)
        manager.enqueueUniquePeriodicWork("system-launcher-appearance", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<LauncherAppearanceWorker>(15, TimeUnit.MINUTES).setInitialDelay(15, TimeUnit.MINUTES).build())
    }
}
class LauncherAppearanceWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = runCatching { LauncherAppearance.sync(applicationContext); Result.success() }.getOrElse { Result.retry() }
}
