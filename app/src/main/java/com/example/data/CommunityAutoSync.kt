package com.example.data

import android.content.Context
import com.example.model.ComicEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/** Durable publication queue for new edits; never scans or publishes other accounts. */
object CommunityAutoSync {
    private val change=MutableStateFlow(0L)
    val revision=change.asStateFlow()
    private fun key(uid:String)="community_pending_$uid"
    @Synchronized fun enqueue(storage:SafeStorage,uid:String,comic:ComicEntry,deleted:Boolean=false){
        if(uid=="guest_local"||uid.isBlank())return
        val queue=runCatching { JSONObject(storage.getItem(key(uid)) ?: "{}") }.getOrDefault(JSONObject())
        queue.put(comic.id,JSONObject().put("key",communityComicKey(comic)).put("rev",comic.updatedAt).put("deleted",deleted))
        storage.setItem(key(uid),queue.toString())
    }
    fun hasPending(storage:SafeStorage,uid:String)=runCatching { JSONObject(storage.getItem(key(uid)) ?: "{}").length()>0 }.getOrDefault(false)

    /** Decisão para um item da fila, isolada para permitir teste sem Firebase. */
    enum class PendingAction { SKIP, WITHDRAW, PUBLISH }

    /**
     * Órfãos (quadrinho removido localmente, por exemplo pelo reset do epoch) precisam virar
     * retirada: antes eram pulados para sempre, o item nunca era reconhecido e o worker
     * ficava em `Result.retry()` indefinidamente.
     */
    internal fun pendingAction(deleted:Boolean,comic:ComicEntry?,revision:Long):PendingAction=when{
        deleted->PendingAction.WITHDRAW
        comic==null->PendingAction.WITHDRAW
        comic.updatedAt<revision->PendingAction.SKIP
        comic.rating<=0f&&comic.review.isBlank()->PendingAction.WITHDRAW
        else->PendingAction.PUBLISH
    }

    @Synchronized private fun acknowledge(storage:SafeStorage,uid:String,id:String,revision:Long){
        val queue=JSONObject(storage.getItem(key(uid)) ?: "{}")
        if(queue.optJSONObject(id)?.optLong("rev")==revision){queue.remove(id);storage.setItem(key(uid),queue.toString())}
    }
    suspend fun flush(context:Context,uid:String,storage:SafeStorage):Boolean {
        val state=storage.loadAccountState(uid) ?: return false
        val repo=CommunityRepository(context)
        if(repo.currentUid()!=uid)return false
        val queue=JSONObject(storage.getItem(key(uid)) ?: "{}")
        for(id in queue.keys().asSequence().take(20).toList()){
            val item=queue.getJSONObject(id);val rev=item.optLong("rev");val comic=state.comics.firstOrNull { it.id==id }
            when(pendingAction(item.optBoolean("deleted"),comic,rev)){
                PendingAction.SKIP->continue
                PendingAction.WITHDRAW->repo.withdrawKey(uid,item.getString("key"))
                PendingAction.PUBLISH->{
                    val target=comic?:continue
                    repo.publish(uid,state.profile,target,target.review,target.rating.toDouble())
                }
            }
            acknowledge(storage,uid,id,rev);change.value=System.currentTimeMillis()
        }
        return !hasPending(storage,uid)
    }
}
