package com.example.data

import android.content.Context
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Two-stage progressive cover verification:
 * Stage 1 emits the first [INITIAL_FAST_BATCH_TARGET] verified items immediately so the carousel paints fast;
 * Stage 2 continues validating up to [maxItems] items for the expanded "Para você" shelf.
 */
internal object RecommendationCoverCheck {
    const val INITIAL_FAST_BATCH_TARGET = 8

    suspend fun filter(
        context: Context,
        items: List<RecommendedComic>,
        maxItems: Int = PersonalizedDiscovery.MAX_DURABLE_RECOMMENDATIONS,
        onInitialBatchReady: (suspend (List<RecommendedComic>) -> Unit)? = null
    ): List<RecommendedComic> = coroutineScope {
        val accepted = mutableListOf<RecommendedComic>()
        var emittedInitial = false
        val permits = Semaphore(4)
        for (batch in items.take(42).chunked(4)) {
            accepted += batch.map { item -> async {
                permits.withPermit {
                    withTimeoutOrNull(3500) {
                        CoverImagePolicy.candidates(item.comic.coverUrl).firstOrNull { url ->
                            Coil.imageLoader(context).execute(
                                ImageRequest.Builder(context).data(url).size(256, 384).build()
                            ) is SuccessResult
                        }?.let { url -> item.copy(comic = item.comic.copy(coverUrl = url)) }
                    }
                }
            } }.awaitAll().filterNotNull()
            if (!emittedInitial && accepted.size >= INITIAL_FAST_BATCH_TARGET) {
                emittedInitial = true
                onInitialBatchReady?.invoke(accepted.take(maxItems))
            }
            if (accepted.size >= maxItems) break
        }
        accepted.take(maxItems)
    }
}
