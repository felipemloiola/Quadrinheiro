package com.example.data

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** One serial background executor shared by snapshot registrations; never used for UI work. */
internal object FirestoreSnapshotDispatch {
    val executor: ExecutorService by lazy {
        Executors.newSingleThreadExecutor { task ->
            Thread(task, "Quadrinheiro-Firestore-Snapshots").apply { isDaemon = true }
        }
    }
}
