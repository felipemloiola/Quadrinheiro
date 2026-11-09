package com.example.data

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.tasks.await
import java.util.concurrent.Executor
import kotlin.coroutines.coroutineContext

/**
 * A coroutine timeout does not cancel a Firestore write. Keep that Task until the
 * SDK finishes it, so retries wait rather than append duplicate persistent mutations.
 * New revisions wait for the current write; SafeStorage remains the durable outbox.
 */
internal class FirestoreWriteGate {
    private data class Flight(val payload: Any, val task: Task<Void>)
    private val active=HashMap<Any,Flight>()
    private val direct=Executor { it.run() }

    suspend fun execute(key: Any, payload: Any, start: () -> Task<Void>) {
        while(true) {
            coroutineContext.ensureActive()
            val flight=synchronized(active) {
                active[key] ?: Flight(payload,start()).also { next ->
                    active[key]=next
                    next.task.addOnCompleteListener(direct) {
                        synchronized(active) { if(active[key]===next)active.remove(key) }
                    }
                }
            }
            try { flight.task.await() }
            catch(error: Exception) {
                coroutineContext.ensureActive()
                if(flight.payload==payload)throw error
                // A different, older write failed; the current dirty revision can try.
                continue
            }
            if(flight.payload==payload)return
        }
    }
}
