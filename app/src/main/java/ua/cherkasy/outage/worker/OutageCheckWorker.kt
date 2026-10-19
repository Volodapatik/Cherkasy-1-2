package ua.cherkasy.outage.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ua.cherkasy.outage.data.OutageRepository

/** Legacy local checker (not scheduled). No notifications. */
class OutageCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            OutageRepository(applicationContext).checkForNewSchedules()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
