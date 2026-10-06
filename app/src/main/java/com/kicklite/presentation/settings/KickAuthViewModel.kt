package com.kicklite.presentation.settings

import androidx.lifecycle.ViewModel
import com.kicklite.data.local.KickTokenStore
import com.kicklite.data.remote.KickOAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class KickAuthViewModel @Inject constructor(
    private val tokenStore: KickTokenStore,
    private val oauthManager: KickOAuthManager
) : ViewModel() {
    private val _configured = MutableStateFlow(tokenStore.getAccessToken()?.isNotBlank() == true)
    val configured = _configured.asStateFlow()

    fun startLogin(context: android.content.Context): Boolean = oauthManager.startLogin(context)

    fun handleCallback(context: android.content.Context, uri: android.net.Uri): Boolean = oauthManager.handleCallback(context, uri)

    fun saveToken(token: String) {
        val clean = token.trim()
        if (clean.isEmpty()) return
        tokenStore.saveAccessToken(clean)
        _configured.value = true
    }

    fun clearToken() {
        tokenStore.clear()
        _configured.value = false
    }
}
