package ua.cherkasy.outage.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ua.cherkasy.outage.data.OutageRepository

/**
 * Legacy local checker. Not scheduled anymore (Termux FCM sends pushes).
 * Kept without notifications in case it is triggered manually.
 */
class OutageCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val repo = OutageRepository(applicationContext)
            repo.checkForNewSchedules()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
