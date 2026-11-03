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

    @Synchronized
    fun sync(context: Context) {
        val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val pm = context.packageManager
        val enabled = ComponentName(context.packageName, if (night) NIGHT else DAY)
        val disabled = ComponentName(context.packageName, if (night) DAY else NIGHT)
        if (pm.getComponentEnabledSetting(enabled) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED &&
            pm.getComponentEnabledSetting(disabled) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) return
        if (Build.VERSION.SDK_INT >= 33) pm.setComponentEnabledSettings(listOf(
            PackageManager.ComponentEnabledSetting(enabled, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP),
            PackageManager.ComponentEnabledSetting(disabled, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        )) else {
            pm.setComponentEnabledSetting(enabled, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            pm.setComponentEnabledSetting(disabled, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        }
    }

    fun schedule(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("system-launcher-appearance", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<LauncherAppearanceWorker>(15, TimeUnit.MINUTES).build())
    }
}

class LauncherAppearanceWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = runCatching { LauncherAppearance.sync(applicationContext); Result.success() }.getOrElse { Result.retry() }
}
