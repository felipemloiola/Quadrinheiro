package com.example

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.work.*
import java.util.concurrent.TimeUnit

object LauncherAppearance {
    internal const val DAY = "com.example.LauncherDay"
    internal const val NIGHT = "com.example.LauncherNight"
    private const val PREFS = "launcher_appearance"
    fun selected(context: Context) = LauncherIconStyle.fromId(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("background", "default"))
    fun select(context: Context, style: LauncherIconStyle) {
        val previous = selected(context)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("background", style.id).apply()
        try { sync(context) } catch (e: Exception) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("background", previous.id).apply()
            throw e
        }
        runCatching { schedule(context) }
    }
    internal fun aliasFor(style: LauncherIconStyle, night: Boolean) = if (style == LauncherIconStyle.DEFAULT) { if (night) NIGHT else DAY } else style.alias
    @Synchronized
    fun sync(context: Context) {
        val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val pm = context.packageManager
        val desired = aliasFor(selected(context), night)
        val names = listOf(desired) + (listOf(DAY, NIGHT) + LauncherIconStyle.entries.filter { it != LauncherIconStyle.DEFAULT }.map { it.alias }).filter { it != desired }
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
        if (selected(context) != LauncherIconStyle.DEFAULT) manager.cancelUniqueWork("system-launcher-appearance")
        else manager.enqueueUniquePeriodicWork("system-launcher-appearance", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<LauncherAppearanceWorker>(15, TimeUnit.MINUTES).build())
    }
}
class LauncherAppearanceWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = runCatching { LauncherAppearance.sync(applicationContext); Result.success() }.getOrElse { Result.retry() }
}
