package com.example.data

import android.content.Context
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

/** Validate in small batches until the minimum selection is available; never on composition. */
internal object RecommendationCoverCheck {
    suspend fun filter(context: Context, items: List<RecommendedComic>): List<RecommendedComic> = coroutineScope {
        val accepted = mutableListOf<RecommendedComic>()
        for (batch in items.take(24).chunked(3)) {
            accepted += batch.map { item -> async {
                withTimeoutOrNull(3500) {
                    CoverImagePolicy.candidates(item.comic.coverUrl).firstOrNull { url ->
                        Coil.imageLoader(context).execute(ImageRequest.Builder(context).data(url).size(256,384).build()) is SuccessResult
                    }?.let { url -> item.copy(comic = item.comic.copy(coverUrl = url)) }
                }
            } }.awaitAll().filterNotNull()
            if (accepted.size >= 6) break
        }
        accepted
    }
}
