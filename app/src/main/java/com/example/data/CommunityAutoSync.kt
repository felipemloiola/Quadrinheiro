package com.example.data

import android.content.Context
import com.example.model.ComicEntry
import com.example.model.ReaderProfile
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

/** Durable publication queue for new edits; also aggregates rated HQs for immediate Community feed display. */
object CommunityAutoSync {
    private val change = MutableStateFlow(0L)
    val revision = change.asStateFlow()
    private fun key(uid: String) = "community_pending_$uid"
    private const val SHARED_POSTS_KEY = "community_shared_posts_v1"
    private fun commentsKey(postId: String) = "community_comments_$postId"

    @Synchronized fun enqueue(storage: SafeStorage, uid: String, comic: ComicEntry, deleted: Boolean = false) {
        if (uid == "guest_local" || uid.isBlank()) return
        val queue = runCatching { JSONObject(storage.getItem(key(uid)) ?: "{}") }.getOrDefault(JSONObject())
        val comicKey = communityComicKey(comic)
        queue.put(comic.id, JSONObject().put("key", comicKey).put("rev", comic.updatedAt).put("deleted", deleted))
        storage.setItem(key(uid), queue.toString())
        val postId = "${uid}_$comicKey"
        val shared = runCatching { JSONObject(storage.getItem(SHARED_POSTS_KEY) ?: "{}") }.getOrDefault(JSONObject())
        if (deleted || (comic.rating <= 0f && comic.review.isBlank())) {
            shared.remove(postId)
        } else {
            val state = storage.loadAccountState(uid)
            val authorName = state?.profile?.name?.takeIf { it.isNotBlank() }
                ?: storage.loadAuthSession()?.takeIf { it.uid == uid }?.displayName?.takeIf { it.isNotBlank() }
                ?: "Leitor"
            val handle = state?.profile?.handle.orEmpty()
            val avatar = state?.profile?.avatarUrl.orEmpty()
            shared.put(
                postId,
                JSONObject()
                    .put("id", postId)
                    .put("ownerId", uid)
                    .put("author", authorName.take(80))
                    .put("handle", handle.take(60))
                    .put("avatar", avatar.take(2000))
                    .put("comicKey", comicKey)
                    .put("title", comic.displayTitle.take(300))
                    .put("cover", comic.coverUrl.take(2000))
                    .put("rating", comic.rating.toDouble())
                    .put("body", comic.review.trim().take(5000))
                    .put("updatedAt", comic.updatedAt.takeIf { it > 0L } ?: System.currentTimeMillis())
            )
        }
        storage.setItem(SHARED_POSTS_KEY, shared.toString())
        change.value = System.currentTimeMillis()
    }

    fun hasPending(storage: SafeStorage, uid: String) =
        runCatching { JSONObject(storage.getItem(key(uid)) ?: "{}").length() > 0 }.getOrDefault(false)

    @Synchronized private fun acknowledge(storage: SafeStorage, uid: String, id: String, revision: Long) {
        val queue = JSONObject(storage.getItem(key(uid)) ?: "{}")
        if (queue.optJSONObject(id)?.optLong("rev") == revision) {
            queue.remove(id)
            storage.setItem(key(uid), queue.toString())
        }
    }

    suspend fun flush(context: Context, uid: String, storage: SafeStorage): Boolean {
        val state = storage.loadAccountState(uid) ?: return false
        val repo = CommunityRepository(context)
        if (repo.currentUid() != uid) return false
        val queue = JSONObject(storage.getItem(key(uid)) ?: "{}")
        for (id in queue.keys().asSequence().take(20).toList()) {
            val item = queue.getJSONObject(id)
            val rev = item.optLong("rev")
            val comic = state.comics.firstOrNull { it.id == id }
            if (!item.optBoolean("deleted") && (comic == null || comic.updatedAt < rev)) continue
            if (item.optBoolean("deleted") || comic == null || (comic.rating <= 0f && comic.review.isBlank())) {
                repo.withdrawKey(uid, item.getString("key"))
            } else {
                repo.publish(uid, state.profile, comic, comic.review, comic.rating.toDouble())
            }
            acknowledge(storage, uid, id, rev)
            change.value = System.currentTimeMillis()
        }
        // Also ensure any existing rated/reviewed comics in the active user's library are published
        for (comic in state.comics.filter { it.rating > 0f || it.review.isNotBlank() }.take(20)) {
            runCatching {
                repo.publish(uid, state.profile, comic, comic.review, comic.rating.toDouble())
            }
        }
        return !hasPending(storage, uid)
    }

    fun localReviews(
        storage: SafeStorage,
        activeUid: String,
        currentComics: List<ComicEntry>,
        currentProfile: ReaderProfile,
        comicKeyFilter: String? = null
    ): List<CommunityReview> {
        val byId = LinkedHashMap<String, CommunityReview>()

        // 1. Read shared posts persisted across accounts on this device
        val shared = runCatching { JSONObject(storage.getItem(SHARED_POSTS_KEY) ?: "{}") }.getOrDefault(JSONObject())
        for (postId in shared.keys()) {
            val obj = shared.optJSONObject(postId) ?: continue
            val cKey = obj.optString("comicKey")
            if (comicKeyFilter != null && cKey != comicKeyFilter) continue
            val tsMillis = obj.optLong("updatedAt").takeIf { it > 0L } ?: System.currentTimeMillis()
            byId[postId] = CommunityReview(
                id = postId,
                ownerId = obj.optString("ownerId"),
                author = obj.optString("author").ifBlank { "Leitor" },
                handle = obj.optString("handle"),
                title = obj.optString("title").ifBlank { "HQ" },
                cover = obj.optString("cover"),
                rating = obj.optDouble("rating", 0.0),
                body = obj.optString("body"),
                createdAt = Timestamp(Date(tsMillis)),
                avatar = obj.optString("avatar")
            )
        }

        // 2. Include all rated/reviewed HQs from the active user's collection
        if (activeUid.isNotBlank() && activeUid != "guest_local") {
            val authorName = currentProfile.name.trim().ifBlank {
                storage.loadAuthSession()?.displayName?.takeIf { it.isNotBlank() } ?: "Leitor"
            }
            val updatedShared = runCatching { JSONObject(storage.getItem(SHARED_POSTS_KEY) ?: "{}") }.getOrDefault(JSONObject())
            var sharedChanged = false
            for (comic in currentComics) {
                if (comic.rating <= 0f && comic.review.isBlank()) continue
                val cKey = communityComicKey(comic)
                if (comicKeyFilter != null && cKey != comicKeyFilter) continue
                val postId = "${activeUid}_$cKey"
                val tsMillis = comic.updatedAt.takeIf { it > 0L } ?: System.currentTimeMillis()
                val review = CommunityReview(
                    id = postId,
                    ownerId = activeUid,
                    author = authorName,
                    handle = currentProfile.handle,
                    title = comic.displayTitle.ifBlank { comic.title }.ifBlank { "HQ" },
                    cover = comic.coverUrl,
                    rating = comic.rating.toDouble(),
                    body = comic.review.trim(),
                    createdAt = Timestamp(Date(tsMillis)),
                    avatar = currentProfile.avatarUrl
                )
                byId[postId] = review
                if (!updatedShared.has(postId)) {
                    updatedShared.put(
                        postId,
                        JSONObject()
                            .put("id", postId)
                            .put("ownerId", activeUid)
                            .put("author", review.author.take(80))
                            .put("handle", review.handle.take(60))
                            .put("avatar", review.avatar.take(2000))
                            .put("comicKey", cKey)
                            .put("title", review.title.take(300))
                            .put("cover", review.cover.take(2000))
                            .put("rating", review.rating)
                            .put("body", review.body.take(5000))
                            .put("updatedAt", tsMillis)
                    )
                    sharedChanged = true
                }
            }
            if (sharedChanged) {
                storage.setItem(SHARED_POSTS_KEY, updatedShared.toString())
            }
        }

        return byId.values.sortedByDescending { it.createdAt.toDate().time }
    }

    fun localComments(storage: SafeStorage, postId: String): List<CommunityComment> {
        val raw = storage.getItem(commentsKey(postId)) ?: return emptyList()
        val arr = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val id = o.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            CommunityComment(
                id = id,
                ownerId = o.optString("ownerId"),
                author = o.optString("author").ifBlank { "Leitor" },
                body = o.optString("body"),
                createdAt = Timestamp(Date(o.optLong("createdAt").takeIf { it > 0L } ?: System.currentTimeMillis()))
            )
        }
    }

    @Synchronized fun addLocalComment(
        storage: SafeStorage,
        uid: String,
        postId: String,
        profile: ReaderProfile,
        body: String
    ): CommunityComment {
        val existing = runCatching { JSONArray(storage.getItem(commentsKey(postId)) ?: "[]") }.getOrDefault(JSONArray())
        val now = System.currentTimeMillis()
        val id = "local_c_${now}_${(100..999).random()}"
        val author = profile.name.trim().take(80).ifBlank { "Leitor" }
        val cleanBody = body.trim().take(1200)
        val obj = JSONObject()
            .put("id", id)
            .put("ownerId", uid)
            .put("author", author)
            .put("body", cleanBody)
            .put("createdAt", now)
        existing.put(obj)
        storage.setItem(commentsKey(postId), existing.toString())
        return CommunityComment(id, uid, author, cleanBody, Timestamp(Date(now)))
    }

    @Synchronized fun deleteLocalComment(storage: SafeStorage, postId: String, commentId: String) {
        val existing = runCatching { JSONArray(storage.getItem(commentsKey(postId)) ?: "[]") }.getOrDefault(JSONArray())
        val next = JSONArray()
        for (i in 0 until existing.length()) {
            val o = existing.optJSONObject(i) ?: continue
            if (o.optString("id") != commentId) next.put(o)
        }
        storage.setItem(commentsKey(postId), next.toString())
    }
}
