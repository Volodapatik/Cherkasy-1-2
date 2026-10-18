package ua.cherkasy.outage.fcm

import android.content.Context

object FcmTokenStore {
    private const val PREFS = "fcm_prefs"
    private const val KEY_TOKEN = "fcm_token"

    fun save(context: Context, token: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TOKEN, token)
            .apply()
    }

    fun get(context: Context): String? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, null)
    }
}
