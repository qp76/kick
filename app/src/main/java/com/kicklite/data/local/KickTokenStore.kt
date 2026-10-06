package com.kicklite.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KickTokenStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("kick_auth", Context.MODE_PRIVATE)

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun saveAccessToken(token: String) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
    }
}
