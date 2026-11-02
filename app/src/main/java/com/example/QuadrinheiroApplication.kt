package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.data.MultiApiSearchEngine

/** One lazy loader/cache for activities and widgets, including a cold widget process. */
class QuadrinheiroApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader =
                ImageLoader.Builder(this)
                    .okHttpClient {
                        MultiApiSearchEngine.initHttpCache(this)
                        MultiApiSearchEngine.sharedHttpClient
                    }
                    .memoryCache {
                        MemoryCache.Builder(this)
                            .maxSizePercent(0.18)
                            .strongReferencesEnabled(false)
                            .build()
                    }
                    .diskCache {
                        DiskCache.Builder()
                            .directory(this.cacheDir.resolve("quadrinheiro_covers_disk_cache"))
                            .maxSizeBytes(250L * 1024L * 1024L)
                            .build()
                    }
                    .crossfade(50)
                    .allowHardware(true)
                    .allowRgb565(true)
                    .respectCacheHeaders(false)
                    .build()
}
