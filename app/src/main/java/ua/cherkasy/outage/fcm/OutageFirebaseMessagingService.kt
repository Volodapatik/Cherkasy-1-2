package ua.cherkasy.outage.fcm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ua.cherkasy.outage.MainActivity
import ua.cherkasy.outage.R
import ua.cherkasy.outage.data.OutageEntity
import ua.cherkasy.outage.data.OutageRepository

class OutageFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed")
        FcmTokenStore.save(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val title = message.notification?.title
            ?: data["title"]
            ?: getString(R.string.fcm_default_title)
        val body = message.notification?.body
            ?: data["body"]
            ?: data["timesText"]
            ?: "Новий графік для черги 1.2"

        val dateText = data["dateText"]
        val timesText = data["timesText"]
        val messageId = data["messageId"] ?: data["msgId"]
        if (!dateText.isNullOrBlank() && !timesText.isNullOrBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo = OutageRepository(applicationContext)
                    val id = messageId ?: ("fcm_" + dateText + "_" + timesText).hashCode().toString()
                    repo.insertFromFcm(
                        OutageEntity(
                            messageId = id,
                            dateText = dateText,
                            timesText = timesText,
                            fullMessage = body,
                            publishedAt = System.currentTimeMillis()
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save FCM schedule", e)
                }
            }
        }

        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            this,
            getString(R.string.notification_channel_id)
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }

    companion object {
        private const val TAG = "OutageFCM"
    }
}
