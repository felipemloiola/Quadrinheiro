package com.example.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Fila de Sincronização em Background com WorkManager (`CoroutineWorker`):
 * Garante que se o usuário editar o diário cronológico ou a biblioteca offline
 * (salvando no `SafeStorage.kt` com `isDirty = true`) e fechar o aplicativo logo em seguida,
 * as alterações locais serão enviadas para o `QuadrinheiroCloudRepository` assim que houver
 * conectividade de rede (`NetworkType.CONNECTED`).
 */
class QuadrinheiroSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val storage = SafeStorage(applicationContext)
            if (storage.isGuestMode()) {
                return@withContext Result.success()
            }
            val persisted = storage.loadState()
            val dirtyComics = persisted.comics.filter { it.isDirty }
            val profileDirty = persisted.profile.isDirty

            if (dirtyComics.isEmpty() && !profileDirty) {
                return@withContext Result.success()
            }

            val cloudRepo = runCatching {
                QuadrinheiroCloudRepository(applicationContext)
            }.getOrNull() ?: return@withContext Result.success()

            val currentUser = cloudRepo.currentAuthUser() ?: return@withContext Result.success()
            val uid = currentUser.uid

            if (profileDirty) {
                cloudRepo.saveUserProfile(uid, persisted.profile)
                storage.markProfileSynced(persisted.profile.updatedAt.takeIf { it > 0L } ?: System.currentTimeMillis())
            }

            if (dirtyComics.isNotEmpty()) {
                val syncedIds = cloudRepo.syncDirtyComicsBatch(uid, dirtyComics)
                if (syncedIds.isNotEmpty()) {
                    storage.markComicsSynced(syncedIds, System.currentTimeMillis())
                }
            }

            Result.success()
        } catch (_: Throwable) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "quadrinheiro_dirty_cloud_sync"

        fun enqueueDirtySync(context: Context) {
            runCatching {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val syncRequest = OneTimeWorkRequestBuilder<QuadrinheiroSyncWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        15L,
                        TimeUnit.SECONDS
                    )
                    .build()

                WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                    UNIQUE_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    syncRequest
                )
            }
        }
    }
}
