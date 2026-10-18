package ua.cherkasy.outage

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.messaging.FirebaseMessaging
import ua.cherkasy.outage.fcm.FcmTokenStore
import ua.cherkasy.outage.worker.OutageCheckWorker
import java.util.concurrent.TimeUnit

class OutageApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        schedulePeriodicCheck()
        fetchFcmToken()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                getString(R.string.notification_channel_id),
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Сповіщення про нові графіки відключень для черги 1.2"
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun schedulePeriodicCheck() {
        val request = PeriodicWorkRequestBuilder<OutageCheckWorker>(
            20, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "outage_check",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun fetchFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("OutageApp", "FCM token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result ?: return@addOnCompleteListener
            FcmTokenStore.save(this, token)
            Log.d("OutageApp", "FCM token ready (len=${token.length})")
        }
    }
}
