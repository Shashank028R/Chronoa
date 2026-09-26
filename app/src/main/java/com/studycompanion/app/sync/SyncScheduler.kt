package com.studycompanion.app.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.studycompanion.app.StudyCompanionApplication
import java.util.concurrent.TimeUnit

/**
 * Background WorkManager worker performing scheduled periodic sync.
 */
class PeriodicSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? StudyCompanionApplication ?: return Result.failure()
        val syncEngine = app.container.syncEngine
        val result = syncEngine.sync()
        return if (result.isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}

/**
 * Background WorkManager worker executing an immediate one-time sync when mutations are created.
 */
class OneTimeSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? StudyCompanionApplication ?: return Result.failure()
        val syncEngine = app.container.syncEngine
        val result = syncEngine.sync()
        return if (result.isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}

/**
 * Scheduler coordinating background WorkManager tasks for synchronization.
 */
class SyncScheduler(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    /**
     * Schedules periodic background sync (every 15 minutes with connected network).
     */
    fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<PeriodicSyncWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES // Flex interval
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }

    /**
     * Enqueues an immediate one-time background sync when new mutations are queued or connectivity is restored.
     */
    fun triggerImmediateSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeRequest = OneTimeWorkRequestBuilder<OneTimeSyncWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            ONE_TIME_SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest
        )
    }

    companion object {
        const val PERIODIC_SYNC_WORK_NAME = "study_companion_periodic_sync"
        const val ONE_TIME_SYNC_WORK_NAME = "study_companion_onetime_sync"
    }
}
