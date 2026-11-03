package com.example.data

import android.content.Context
import com.example.R
import com.example.model.ComicEntry
import com.example.model.ReaderProfile
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.pipeline.Expression.Companion.field
import com.google.firebase.firestore.pipeline.Expression.Companion.or
import com.google.firebase.firestore.pipeline.Expression.Companion.and
import com.google.firebase.firestore.pipeline.AggregateFunction.Companion.countAll
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.security.MessageDigest

data class CommunityCursor(val time: Timestamp, val path: String)
data class CommunityReview(val id: String, val ownerId: String, val author: String, val handle: String,
    val title: String, val cover: String, val rating: Double, val body: String, val createdAt: Timestamp,
    val avatar: String = "")
data class CommunityComment(val id: String, val ownerId: String, val author: String, val body: String, val createdAt: Timestamp)
data class CommunityPage<T>(val items: List<T>, val cursor: CommunityCursor?, val hasMore: Boolean)
data class CommunityRating(val average: Double, val count: Long)

fun communityComicKey(comic: ComicEntry): String {
    val identity = comic.externalId.takeIf { (it.startsWith("cv-")||it.startsWith("mt-")) } ?: comic.id.takeIf { it.startsWith("cv-") }
        ?: (PortugueseComicTranslator.canonicalWorkKey(comic.displayTitle) + ":" + SeriesSequenceEngine.extractPrimaryAuthor(comic.authors).lowercase())
    return MessageDigest.getInstance("SHA-256").digest(identity.toByteArray()).joinToString("") { "%02x".format(it) }.take(32)
}

/** Only deliberately published snapshots leave the private library. No private profile joins. */
class CommunityRepository(context: Context,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id)),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {
    fun currentUid(): String? = auth.currentUser?.uid
    private fun requireOwner(expectedUid: String): String {
        val uid = auth.currentUser?.uid ?: error("Entre na sua conta para participar.")
        check(uid == expectedUid) { "A conta foi alterada. Abra a Comunidade novamente." }; return uid
    }
    private fun afterCursor(pipeline: Pipeline, cursor: CommunityCursor?): Pipeline = if (cursor == null) pipeline else pipeline.where(
        or(field("createdAt").lessThan(cursor.time), and(field("createdAt").equal(cursor.time), field("__name__").lessThan(db.document(cursor.path)))))

    private fun published(comicKey: String? = null): Pipeline {
        val pipeline = db.pipeline().collection("communityReviews").where(field("published").equal(true))
        return if (comicKey == null) pipeline else pipeline.where(field("comicKey").equal(comicKey))
    }
    suspend fun rating(comic: ComicEntry): CommunityRating = withTimeout(12000) {
        check(currentUid() != null)
        val row = published(communityComicKey(comic)).where(field("rating").greaterThan(0))
            .aggregate(field("rating").average().alias("average"), countAll().alias("count")).execute().await().results.firstOrNull()
        CommunityRating((row?.getData()?.get("average") as? Number)?.toDouble() ?: 0.0, (row?.getData()?.get("count") as? Number)?.toLong() ?: 0)
    }
    suspend fun reviews(cursor: CommunityCursor? = null, comicKey: String? = null): CommunityPage<CommunityReview> = withTimeout(12000) {
        check(currentUid() != null) { "Entre na sua conta para abrir a Comunidade." }
        // Server-side filtering, stable cursor and bounded result set. Pipelines do not poll/cache.
        val results = afterCursor(published(comicKey), cursor)
            .sort(field("createdAt").descending(), field("__name__").descending()).limit(20).execute().await().results
        val items = results.mapNotNull { row ->
            val data = row.getData(); val created = data["createdAt"] as? Timestamp ?: return@mapNotNull null
            CommunityReview(row.getId() ?: return@mapNotNull null, data["ownerId"] as? String ?: "", data["author"] as? String ?: "Leitor",
                data["handle"] as? String ?: "", data["title"] as? String ?: "HQ", data["cover"] as? String ?: "",
                (data["rating"] as? Number)?.toDouble() ?: 0.0, data["body"] as? String ?: "", created, data["avatar"] as? String ?: "")
        }
        val last = results.lastOrNull()
        CommunityPage(items, last?.let { row -> (row.getData()["createdAt"] as? Timestamp)?.let { CommunityCursor(it, row.ref!!.path) } }, results.size == 20)
    }
    suspend fun publish(expectedUid: String, profile: ReaderProfile, comic: ComicEntry, body: String, rating: Double): String = withTimeout(12000) {
        val uid = requireOwner(expectedUid)
        require(body.trim().length <= 5000 && rating in 0.0..5.0 && (body.isNotBlank() || rating > 0))
        val key = communityComicKey(comic)
        val ref = db.collection("communityReviews").document("${uid}_$key")
        db.runTransaction { transaction ->
            val old = transaction.get(ref)
            val data = mapOf("ownerId" to uid, "author" to profile.name.trim().take(80).ifBlank { "Leitor" },
                "handle" to profile.handle.take(60), "avatar" to profile.avatarUrl.takeIf { it.startsWith("https://") && it.length <= 2000 }.orEmpty(),
                "comicKey" to key, "title" to comic.displayTitle.take(300), "cover" to comic.coverUrl.takeIf { it.startsWith("https://") }.orEmpty().take(2000),
                "body" to body.trim(), "rating" to rating, "published" to true,
                "createdAt" to (old.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()), "updatedAt" to FieldValue.serverTimestamp())
            transaction.set(ref, data)
        }.await()
        ref.id
    }
    suspend fun withdraw(expectedUid: String, postId: String) = withTimeout(12000) {
        requireOwner(expectedUid)
        db.collection("communityReviews").document(postId).update(mapOf("published" to false, "updatedAt" to FieldValue.serverTimestamp())).await()
    }
    suspend fun withdrawKey(expectedUid:String,comicKey:String) = withTimeout(12000) {
        requireOwner(expectedUid)
        val ref=db.collection("communityReviews").document("${expectedUid}_$comicKey")
        if(ref.get(Source.SERVER).await().exists())ref.update(mapOf("published" to false,"updatedAt" to FieldValue.serverTimestamp())).await()
        Unit
    }
    suspend fun comments(postId: String, cursor: CommunityCursor? = null): CommunityPage<CommunityComment> = withTimeout(12000) {
        check(currentUid() != null)
        val results = afterCursor(db.pipeline().collection("communityReviews/$postId/comments"), cursor)
            .sort(field("createdAt").descending(), field("__name__").descending()).limit(20).execute().await().results
        val items = results.mapNotNull { row -> val d = row.getData()
            CommunityComment(row.getId() ?: return@mapNotNull null, d["ownerId"] as? String ?: "", d["author"] as? String ?: "Leitor",
                d["body"] as? String ?: "", d["createdAt"] as? Timestamp ?: return@mapNotNull null)
        }
        val last = results.lastOrNull()
        CommunityPage(items, last?.let { row -> (row.getData()["createdAt"] as? Timestamp)?.let { CommunityCursor(it, row.ref!!.path) } }, results.size == 20)
    }
    suspend fun comment(expectedUid: String, postId: String, profile: ReaderProfile, body: String) = withTimeout(12000) {
        val uid = requireOwner(expectedUid); require(body.trim().length in 1..1200)
        db.collection("communityReviews").document(postId).collection("comments").add(mapOf(
            "ownerId" to uid, "author" to profile.name.take(80).ifBlank { "Leitor" }, "body" to body.trim(), "createdAt" to FieldValue.serverTimestamp())).await()
    }
    suspend fun deleteComment(expectedUid: String, postId: String, commentId: String) = withTimeout(12000) {
        requireOwner(expectedUid)
        db.collection("communityReviews").document(postId).collection("comments").document(commentId).delete().await()
    }
}
