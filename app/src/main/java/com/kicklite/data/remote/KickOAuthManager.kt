package com.kicklite.data.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.kicklite.BuildConfig
import com.kicklite.data.local.KickTokenStore
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KickOAuthManager @Inject constructor(
    private val client: OkHttpClient,
    private val tokenStore: KickTokenStore
) {
    private val prefsName = "kick_oauth"

    fun startLogin(context: Context): Boolean {
        val gateway = BuildConfig.KICK_OAUTH_GATEWAY_URL.trim().trimEnd('/')
        if (gateway.isBlank()) return false

        val verifier = randomUrlString(48)
        val challenge = base64Url(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))
        val state = randomUrlString(32)
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit()
            .putString("verifier", verifier)
            .putString("state", state)
            .apply()

        val body = JSONObject().put("state", state).put("code_verifier", verifier)
            .toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url("$gateway/oauth/pkce").post(body).build()
        Thread {
            runCatching {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val authorize = Uri.parse("$gateway/oauth/start").buildUpon()
                        .appendQueryParameter("state", state)
                        .appendQueryParameter("code_challenge", challenge)
                        .build()
                    context.startActivity(Intent(Intent.ACTION_VIEW, authorize).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        }.start()
        return true
    }

    fun handleCallback(context: Context, uri: Uri): Boolean {
        if (uri.scheme != "kicklite" || uri.host != "oauth" || uri.path != "/callback") return false
        val code = uri.getQueryParameter("code") ?: return false
        val state = uri.getQueryParameter("state") ?: return false
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        if (prefs.getString("state", null) != state) return false
        val gateway = BuildConfig.KICK_OAUTH_GATEWAY_URL.trim().trimEnd('/')
        if (gateway.isBlank()) return false

        val body = JSONObject().put("code", code).put("state", state).toString()
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url("$gateway/oauth/exchange").post(body).build()
        Thread {
            runCatching {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val json = JSONObject(response.body?.string().orEmpty())
                    val accessToken = json.optString("access_token")
                    if (accessToken.isNotBlank()) tokenStore.saveAccessToken(accessToken)
                    prefs.edit().clear().apply()
                }
            }
        }.start()
        return true
    }

    private fun randomUrlString(bytes: Int): String {
        val data = ByteArray(bytes)
        SecureRandom().nextBytes(data)
        return base64Url(data)
    }

    private fun base64Url(data: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(data)
}
