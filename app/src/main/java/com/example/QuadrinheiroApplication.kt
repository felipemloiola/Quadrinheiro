package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.data.MultiApiSearchEngine
import com.example.data.RejectHeifAvifDecoderFactory
import com.example.data.ImageMemoryPolicy

/** One lazy loader/cache for activities and widgets, including a cold widget process. */
class QuadrinheiroApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        if(com.example.data.CloudCacheRecovery.needsReview(this))return
        // Filesystem migrations run on CacheMaintenance's IO scope, never on first draw.
        com.example.data.CacheMaintenance.schedule(this)
        runCatching { LauncherAppearance.schedule(this) }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        runCatching { LauncherAppearance.sync(this); com.example.widget.QuadrinheiroWidgetsSync.refreshAllWidgetsAsync(this) }
    }

    companion object {
        const val COIL_DISK_CACHE_BUDGET_BYTES = 16L * 1024L * 1024L
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components {
                add(RejectHeifAvifDecoderFactory())
            }
            .okHttpClient {
                MultiApiSearchEngine.initHttpCache(this)
                // Evita duplicar binários de imagem no cache HTTP do OkHttp, mantendo-os apenas no DiskCache do Coil
                MultiApiSearchEngine.sharedHttpClient.newBuilder().cache(null).build()
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizeBytes(ImageMemoryPolicy.budgetBytes(
                        Runtime.getRuntime().maxMemory(),
                        getSystemService(android.app.ActivityManager::class.java)?.isLowRamDevice == true
                    ))
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("quadrinheiro_covers_disk_cache"))
                    .maxSizeBytes(COIL_DISK_CACHE_BUDGET_BYTES)
                    .build()
            }
            .crossfade(50)
            .allowHardware(true)
            .allowRgb565(true)
            .respectCacheHeaders(false)
            .build()
}
