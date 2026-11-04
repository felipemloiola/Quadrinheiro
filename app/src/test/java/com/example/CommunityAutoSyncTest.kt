package com.example

import com.example.data.CommunityAutoSync
import com.example.data.CommunityAutoSync.PendingAction
import com.example.model.ComicEntry
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Regressão do retry infinito: um item pendente cujo quadrinho não existe mais precisa ser
 * reconhecido como retirada, senão `hasPending` fica sempre verdadeiro e o worker nunca termina.
 */
class CommunityAutoSyncTest {

    private fun comic(
        id: String = "hq-1",
        updatedAt: Long = 1_000L,
        rating: Float = 4f,
        review: String = "Boa leitura"
    ) = ComicEntry(
        id = id,
        externalId = id,
        title = "HQ",
        authors = "Autor",
        publisher = "Editora",
        rating = rating,
        review = review,
        updatedAt = updatedAt
    )

    @Test
    fun orphanPendingEntryIsWithdrawnInsteadOfSkipped() {
        assertEquals(
            PendingAction.WITHDRAW,
            CommunityAutoSync.pendingAction(deleted = false, comic = null, revision = 1_000L)
        )
    }

    @Test
    fun deletedPendingEntryIsWithdrawnEvenWhenComicStillExists() {
        assertEquals(
            PendingAction.WITHDRAW,
            CommunityAutoSync.pendingAction(deleted = true, comic = comic(), revision = 1_000L)
        )
    }

    @Test
    fun olderComicThanQueuedRevisionIsSkippedBecauseANewerEditOwnsTheQueue() {
        assertEquals(
            PendingAction.SKIP,
            CommunityAutoSync.pendingAction(
                deleted = false,
                comic = comic(updatedAt = 900L),
                revision = 1_000L
            )
        )
    }

    @Test
    fun sameRevisionIsStillPublished() {
        assertEquals(
            PendingAction.PUBLISH,
            CommunityAutoSync.pendingAction(
                deleted = false,
                comic = comic(updatedAt = 1_000L),
                revision = 1_000L
            )
        )
    }

    @Test
    fun clearedReviewIsWithdrawn() {
        assertEquals(
            PendingAction.WITHDRAW,
            CommunityAutoSync.pendingAction(
                deleted = false,
                comic = comic(rating = 0f, review = "   "),
                revision = 1_000L
            )
        )
    }

    @Test
    fun ratingWithoutReviewIsPublished() {
        assertEquals(
            PendingAction.PUBLISH,
            CommunityAutoSync.pendingAction(
                deleted = false,
                comic = comic(rating = 4f, review = ""),
                revision = 1_000L
            )
        )
    }
}
