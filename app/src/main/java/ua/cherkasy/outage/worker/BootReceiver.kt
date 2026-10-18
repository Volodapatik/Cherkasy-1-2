package ua.cherkasy.outage.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            // Local polling disabled — pushes come from Termux FCM server.
            WorkManager.getInstance(context).cancelUniqueWork("outage_check")
        }
    }
}
