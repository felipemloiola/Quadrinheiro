package com.example.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import java.util.Collections
import java.util.WeakHashMap

/** Network-backed sessions: no new persistent SDK document/mutation database. */
internal object OnlineFirestorePolicy {
    private val configured = Collections.newSetFromMap(WeakHashMap<FirebaseFirestore, Boolean>())
    @Synchronized fun configure(db: FirebaseFirestore) {
        if (db in configured) return
        if (db.firestoreSettings.cacheSettings !is MemoryCacheSettings) {
            db.firestoreSettings = FirebaseFirestoreSettings.Builder(db.firestoreSettings)
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build()).build()
        }
        configured.add(db)
    }
}
