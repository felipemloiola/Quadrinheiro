package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

internal class CurationConflict : Exception("Esta lista ou preferência mudou em outro aparelho. Recarregue antes de salvar; seu rascunho foi preservado.")

internal class CurationCloudRepository(context: Context,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id)),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {
    init { OnlineFirestorePolicy.configure(db) }
    private fun owner(uid: String) { check(auth.currentUser?.uid==uid) { "A conta mudou. Abra novamente suas listas." } }
    suspend fun load(uid: String): CurationSnapshot {
        owner(uid)
        val docs=db.collection("users").document(uid).collection("lists").whereEqualTo("deleted",false).limit(101).get(Source.SERVER).await()
        require(docs.size()<=100) { "Limite de 100 listas atingido." }
        val lists=docs.documents.map { d -> CurationCodec.list(d.getString("payload")!!).also { require(it.id==d.id) }
            .copy(revision=d.getLong("revision")!!,deleted=d.getBoolean("deleted")==true) }
        val feedback=db.document("users/$uid/recommendationPreferences/state").get(Source.SERVER).await()
        owner(uid)
        return CurationSnapshot(lists, if(feedback.exists()) CurationCodec.feedback(feedback.getString("payload")!!).copy(revision=feedback.getLong("revision")!!) else RecommendationFeedback())
    }
    suspend fun saveList(uid: String, list: ComicList) = write(uid,"lists/${list.id}",list.revision,list.deleted,CurationCodec.encode(list),list)
    suspend fun saveFeedback(uid: String, feedback: RecommendationFeedback) = write(uid,"recommendationPreferences/state",feedback.revision,false,CurationCodec.encode(feedback))
    private suspend fun write(uid: String, path: String, revision: Long, deleted: Boolean, payload: String, list:ComicList?=null) {
        owner(uid)
        val ref=db.document("users/$uid/$path")
        gate.execute(ref.path to db, Triple(revision,deleted,payload)) {
            db.runTransaction<Void> { tx ->
                owner(uid)
                val previous=tx.get(ref)
                if ((previous.getLong("revision") ?: 0L)!=revision || previous.getBoolean("deleted")==true) throw CurationConflict()
                val wasPublic=previous.getString("payload")?.let {runCatching {CurationCodec.list(it).isPublic}.getOrDefault(false)} ?: false
                tx.set(ref,mapOf("ownerId" to uid,"revision" to revision+1,"deleted" to deleted,
                    "payload" to payload,"updatedAt" to FieldValue.serverTimestamp()))
                if(list!=null && (list.isPublic || wasPublic)) {
                    val published=list.isPublic && !list.deleted
                    tx.set(db.document("publicLists/${uid}_${list.id}"),mapOf("ownerId" to uid,"listId" to list.id,
                        "revision" to revision+1,"published" to published,
                        "payload" to if(published) CurationCodec.encode(list.publicSnapshot()) else "{}",
                        "updatedAt" to FieldValue.serverTimestamp()))
                }
                null
            }
        }
        owner(uid)
    }
    companion object { private val gate=FirestoreWriteGate() }
}

internal class PublicListsRepository(context:Context,private val db:FirebaseFirestore=FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))) {
    suspend fun load():List<ComicList> {
        OnlineFirestorePolicy.configure(db)
        return db.collection("publicLists").whereEqualTo("published",true).limit(20).get(Source.SERVER).await().documents
            .mapNotNull {document -> runCatching {CurationCodec.list(document.getString("payload")!!).publicSnapshot()}.getOrNull()}
    }
}
