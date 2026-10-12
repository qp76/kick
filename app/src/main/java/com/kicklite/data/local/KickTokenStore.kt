package com.kicklite.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KickTokenStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("kick_auth", Context.MODE_PRIVATE)
    private val _accessToken = MutableStateFlow(prefs.getString(KEY_ACCESS_TOKEN, null))
    val accessToken = _accessToken.asStateFlow()

    fun getAccessToken(): String? = _accessToken.value

    fun saveAccessToken(token: String) {
        val clean = token.trim()
        if (clean.isEmpty()) return
        prefs.edit().putString(KEY_ACCESS_TOKEN, clean).apply()
        _accessToken.value = clean
    }

    fun clear() {
        prefs.edit().clear().apply()
        _accessToken.value = null
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
    }
}
