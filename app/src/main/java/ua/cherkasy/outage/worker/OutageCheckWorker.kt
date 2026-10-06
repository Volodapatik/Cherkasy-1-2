package ua.cherkasy.outage.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ua.cherkasy.outage.MainActivity
import ua.cherkasy.outage.R
import ua.cherkasy.outage.data.OutageRepository

class OutageCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val repo = OutageRepository(applicationContext)
            val newSchedules = repo.checkForNewSchedules()

            newSchedules.maxByOrNull { it.publishedAt }?.let { schedule ->
                showNotification(schedule.dateText, schedule.timesText)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun showNotification(dateText: String, timesText: String) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pending = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            applicationContext,
            applicationContext.getString(R.string.notification_channel_id)
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Новий графік для черги 1.2")
            .setContentText("$dateText: $timesText")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Дата: $dateText\nЧас відключення: $timesText")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1001, notification)
    }
}
