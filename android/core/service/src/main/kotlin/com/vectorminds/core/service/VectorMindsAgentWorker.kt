package com.vectorminds.core.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.vectorminds.core.service.agent.VectorMindsAgent
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that periodically triggers the VectorMinds agent loop.
 */
@HiltWorker
class VectorMindsAgentWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val agent: VectorMindsAgent,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "VectorMinds agent cycle — WorkManager trigger")
        return try {
            agent.runCycle()
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Agent cycle failed in WorkManager", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "VectorMindsAgentWorker"
        const val WORK_NAME = "vectorminds_agent_cycle"

        /**
         * Enqueues a 15-minute periodic work request.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<VectorMindsAgentWorker>(
                repeatInterval = 15L,
                repeatIntervalTimeUnit = TimeUnit.MINUTES,
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
