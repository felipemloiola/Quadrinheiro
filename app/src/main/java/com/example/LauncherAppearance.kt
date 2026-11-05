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
    fun selected(context: Context): LauncherIconStyle {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return if (followsApp(context)) LauncherIconStyle.fromId(prefs.getString("accent", "black")) else LauncherIconStyle.DEFAULT
    }
    fun setFollowsApp(context: Context, follow: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("follow_app", follow).apply()
    }
    fun rememberAccent(context: Context, themeId: String) {
        val id = if (themeId == "SYSTEM_MONO") "black" else com.example.ui.theme.AppTheme.fromId(themeId).name.lowercase()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString("accent", null) != id) prefs.edit().putString("accent", id).apply()
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
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("follow_app", style != LauncherIconStyle.DEFAULT).putString("accent", style.id).apply()
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
