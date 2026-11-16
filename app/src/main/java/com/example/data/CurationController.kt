package com.example.data

import android.content.Context
import com.example.model.ComicEntry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

data class CurationState(val owner: String = "", val lists: List<ComicList> = emptyList(),
    val feedback: RecommendationFeedback = RecommendationFeedback(), val loading: Boolean = false,
    val saving: Boolean = false, val error: String? = null, val ready: Boolean = false,
    val localOnly: Boolean = false)

class CurationController internal constructor(context: Context, private val scope: CoroutineScope,
    private val currentOwner: () -> String, private val onFeedbackChanged: () -> Unit,
    private val isLocalOwner: (String) -> Boolean = {it=="guest_local"}) {
    private val storage=CurationStorage(context)
    private val cloud by lazy { CurationCloudRepository(context) }
    private val mutable=MutableStateFlow(CurationState())
    val state=mutable.asStateFlow()
    private var loadJob: Job?=null
    private var localJob: Job?=null
    private var loaded=false
    @Volatile private var localReady=false
    private var generation=0L
    fun activate() {
        val uid=currentOwner()
        val localOnly=isLocalOwner(uid)
        if(mutable.value.owner==uid && mutable.value.localOnly==localOnly) return
        generation++; loadJob?.cancel();localJob?.cancel(); loaded=false;localReady=false
        mutable.value=CurationState(owner=uid,localOnly=localOnly)
        val token=generation
        localJob=scope.launch(Dispatchers.IO) {
            val local=runCatching { storage.load(uid) }.getOrElse {
                withContext(Dispatchers.Main) {if(token==generation && currentOwner()==uid) mutable.value=mutable.value.copy(error="Não foi possível ler os dados locais. A cópia original foi preservada. Recarregue para recuperar da conta.")}
                return@launch
            }
            withContext(Dispatchers.Main) {
                if(token==generation && currentOwner()==uid) {
                    mutable.value=mutable.value.copy(lists=local.lists,feedback=local.feedback)
                    localReady=true
                    onFeedbackChanged()
                }
            }
        }
    }
    fun load(force: Boolean=false) {
        activate()
        if((!force && loaded) || loadJob?.isActive==true || mutable.value.saving) return
        val uid=currentOwner(); val token=generation
        val localOnly=mutable.value.localOnly
        val pendingLocal=localJob
        if(uid=="local_default") { mutable.value=mutable.value.copy(error="Entre em uma conta ou use o modo convidado."); return }
        loadJob=scope.launch {
            mutable.value=mutable.value.copy(loading=true,error=null)
            pendingLocal?.join()
            try {
                var cacheIssue=false
                val snapshot=withContext(Dispatchers.IO) {
                    if(localOnly) storage.load(uid) else cloud.load(uid).also {
                        cacheIssue=runCatching {storage.save(uid,it)}.isFailure
                    }
                }
                if(token==generation && currentOwner()==uid) {
                    loaded=true;localReady=true; mutable.value=CurationState(uid,snapshot.lists,snapshot.feedback,
                        error=if(cacheIssue) "Dados carregados da conta, mas a cópia local não pôde ser atualizada. Verifique o espaço disponível ou reduza o tamanho das listas." else null,ready=true,localOnly=localOnly)
                    onFeedbackChanged()
                }
            } catch(cancelled: CancellationException) { throw cancelled }
            catch(e: Exception) {
                if(com.example.BuildConfig.DEBUG) android.util.Log.w("PrivateCuration",
                    "Load ${e.javaClass.simpleName} code=${(e as? com.google.firebase.firestore.FirebaseFirestoreException)?.code?.name ?: "none"} at ${e.stackTrace.firstOrNull {it.className.startsWith("com.example")}}")
                if(token==generation && currentOwner()==uid) mutable.value=mutable.value.copy(loading=false,error="Não foi possível carregar seus dados privados. Tente novamente.")
            }
        }
    }
    suspend fun save(list: ComicList, owner: String = state.value.owner): Boolean =
        if(owner!=currentOwner()) false else mutate(list = list,expectedOwner=owner)
    suspend fun feedback(comic: ComicEntry, action: String): Boolean {
        if(mutable.value.owner!=currentOwner()) { activate(); return false }
        val ref=ListComic.from(comic)
        val old=mutable.value.feedback
        val work=PersonalizedDiscovery.recommendationWorkKey(comic)
        fun without(items: List<ListComic>)=items.filterNot { PersonalizedDiscovery.recommendationWorkKey(it.comic())==work }
        val next=when(action) {
            "hide" -> old.copy(hidden=without(old.hidden)+ref,boosted=without(old.boosted))
            "more" -> old.copy(hidden=without(old.hidden),boosted=without(old.boosted)+ref)
            "undo" -> old.copy(hidden=without(old.hidden),boosted=without(old.boosted))
            else -> return false
        }
        return mutate(feedback=next)
    }
    // A dismissed/rotated editor must not cancel an acknowledged user action or strand saving=true.
    // The ViewModel owns the operation; cancelling an awaiter does not cancel this Deferred.
    private suspend fun mutate(list: ComicList?=null, feedback: RecommendationFeedback?=null,
        expectedOwner: String=currentOwner()): Boolean = scope.async {mutateOwned(list,feedback,expectedOwner)}.await()
    private suspend fun mutateOwned(list: ComicList?, feedback: RecommendationFeedback?, expectedOwner: String): Boolean {
        activate()
        val old=mutable.value; val uid=old.owner; val token=generation
        if(!loaded || old.saving || old.loading || uid!=expectedOwner || uid!=currentOwner() || uid=="local_default") return false
        mutable.value=old.copy(saving=true,error=null)
        return try {
            var cacheIssue=false
            val next=withContext(Dispatchers.IO) {
                val lists=if(list==null) old.lists else (old.lists.filterNot { it.id==list.id }+list.copy(revision=list.revision+1))
                require(lists.count {!it.deleted}<=100 && lists.size<=2100) { "Limite de listas atingido. Exclua uma lista ativa antes de criar outra." }
                list?.let { CurationCodec.encode(it); storage.saveDraft(uid,it) }
                feedback?.let { CurationCodec.encode(it) }
                if(!old.localOnly) {
                    if(list!=null) cloud.saveList(uid,list)
                    if(feedback!=null) cloud.saveFeedback(uid,feedback)
                } else if(list!=null) {
                    val previous=old.lists.firstOrNull { it.id==list.id }
                    if((previous?.revision ?: 0)!=list.revision || previous?.deleted==true) throw CurationConflict()
                } else if(feedback!=null && feedback.revision!=old.feedback.revision) {
                    throw CurationConflict()
                }
                CurationSnapshot(lists,feedback?.copy(revision=feedback.revision+1) ?: old.feedback).also {
                    try {storage.save(uid,it)} catch(e: Exception) {
                        if(old.localOnly) throw e
                        cacheIssue=true // The server acknowledged the write. Never report it as unsaved or hide the new revision.
                    }
                    if(list!=null) storage.clearDraft(uid,list.id)
                }
            }
            if(token==generation && currentOwner()==uid) {
                mutable.value=CurationState(uid,next.lists,next.feedback,
                    error=if(cacheIssue) "Salvo na conta. A cópia local não pôde ser atualizada; verifique o espaço ou reduza o tamanho das listas." else null,ready=true,localOnly=old.localOnly)
                if(feedback!=null) onFeedbackChanged()
            }
            true
        } catch(cancelled: CancellationException) { throw cancelled }
        catch(e: Exception) {
            if(token==generation && currentOwner()==uid) {
                val conflict=generateSequence<Throwable>(e) { it.cause }.any { it is CurationConflict }
                mutable.value=old.copy(error=when {
                    conflict -> "Alterado em outro aparelho. Recarregue; seu rascunho está preservado."
                    e is IllegalArgumentException -> e.message ?: "Verifique os limites da lista."
                    else -> "Não foi possível salvar. Seu rascunho foi preservado; tente novamente."
                })
            }
            false
        }
    }
    suspend fun draft(): ComicList? = withContext(Dispatchers.IO) { storage.draft(currentOwner()) }
    internal fun isReady(uid: String) = localReady && mutable.value.owner==uid
    internal suspend fun awaitLocal() = withContext(Dispatchers.Main.immediate) {activate();localJob?.join()}
    suspend fun drafts(): List<ComicList> = withContext(Dispatchers.IO) {storage.drafts(currentOwner())}
    suspend fun discardDraft(id: String, owner: String): Boolean = withContext(Dispatchers.IO) {
        if(owner!=currentOwner()) false else {storage.clearDraft(owner,id);true}
    }
    suspend fun preserveDraft(list: ComicList, owner: String = state.value.owner): Boolean {
        if(owner!=currentOwner()) return false
        val success=withContext(Dispatchers.IO) {
            runCatching { storage.saveDraft(owner,list.copy(name=list.name.ifBlank {"Lista sem título"})) }.isSuccess
        }
        if(!success && owner==currentOwner()) mutable.value=mutable.value.copy(error="Não foi possível preservar o rascunho. Reduza o tamanho ou libere espaço e tente novamente.")
        return success
    }
    internal fun export(): JSONObject {
        val uid=currentOwner(); val current=mutable.value
        val snapshot=if(loaded && current.owner==uid) CurationSnapshot(current.lists,current.feedback) else storage.load(uid)
        return storage.json(snapshot)
    }
    internal fun validateBackup(root: JSONObject) = storage.decode(root)
    internal suspend fun restore(snapshot: CurationSnapshot, owner: String = currentOwner()): Boolean {
        // Restore through normal revision-checked writes. Never copy source account revisions/ownership.
        if(currentOwner()!=owner) return false
        load(force=true); loadJob?.join()
        if(state.value.error!=null || currentOwner()!=owner) return false
        for(list in snapshot.lists.filterNot { it.deleted }) {
            val previous=state.value.lists.firstOrNull { it.id==list.id && !it.deleted }
            val target=list.copy(id=previous?.id ?: java.util.UUID.randomUUID().toString(),revision=previous?.revision ?: 0)
            if(!save(target,owner)) return false
        }
        if(currentOwner()!=owner) return false
        return mutate(feedback=snapshot.feedback.copy(revision=state.value.feedback.revision))
    }
}
