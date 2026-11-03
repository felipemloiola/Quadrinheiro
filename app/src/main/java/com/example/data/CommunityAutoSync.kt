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
            if(!item.optBoolean("deleted") && (comic==null || comic.updatedAt<rev))continue
            if(item.optBoolean("deleted")||comic==null|| (comic.rating<=0f&&comic.review.isBlank()))repo.withdrawKey(uid,item.getString("key"))
            else repo.publish(uid,state.profile,comic,comic.review,comic.rating.toDouble())
            acknowledge(storage,uid,id,rev);change.value=System.currentTimeMillis()
        }
        return !hasPending(storage,uid)
    }
}
