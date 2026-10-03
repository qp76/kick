package com.kicklite.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KickAuthManager @Inject constructor(@ApplicationContext private val context: Context) {
    private val prefs = context.getSharedPreferences("kick_auth", Context.MODE_PRIVATE)
    private val clientId = com.kicklite.BuildConfig.KICK_CLIENT_ID
    private val redirectUri = com.kicklite.BuildConfig.KICK_REDIRECT_URI

    fun isConfigured(): Boolean = clientId.isNotBlank() && redirectUri.isNotBlank()

    fun beginLogin(): Boolean {
        if (!isConfigured()) return false
        val verifier = randomUrlSafe(64)
        val challenge = base64Url(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))
        val state = randomUrlSafe(32)
        prefs.edit().putString("pkce_verifier", verifier).putString("state", state).apply()
        val uri = Uri.parse("https://id.kick.com/oauth/authorize").buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("scope", "user:read channel:read chat:write")
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("state", state)
            .build()
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }

    fun consumeCallback(uri: Uri): CallbackResult {
        val expected = prefs.getString("state", null)
        val state = uri.getQueryParameter("state")
        val code = uri.getQueryParameter("code")
        prefs.edit().remove("state").apply()
        return when {
            state.isNullOrBlank() || expected == null || state != expected -> CallbackResult.Error("OAuth state validation failed")
            !code.isNullOrBlank() -> CallbackResult.Code(code, prefs.getString("pkce_verifier", null))
            else -> CallbackResult.Error(uri.getQueryParameter("error_description") ?: uri.getQueryParameter("error") ?: "Kick login was cancelled")
        }
    }

    fun clearPending() { prefs.edit().remove("state").remove("pkce_verifier").apply() }

    sealed interface CallbackResult {
        data class Code(val code: String, val verifier: String?) : CallbackResult
        data class Error(val message: String): CallbackResult
    }

    private fun randomUrlSafe(bytes: Int): String {
        val data = ByteArray(bytes)
        SecureRandom().nextBytes(data)
        return base64Url(data)
    }
    private fun base64Url(data: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(data)
}
