package com.kicklite.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kicklite.data.local.KickTokenStore
import com.kicklite.data.remote.KickOAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class KickAuthViewModel @Inject constructor(
    private val tokenStore: KickTokenStore,
    private val oauthManager: KickOAuthManager
) : ViewModel() {
    val configured = tokenStore.accessToken
        .map { !it.isNullOrBlank() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = !tokenStore.getAccessToken().isNullOrBlank()
        )

    fun startLogin(context: android.content.Context): Boolean = oauthManager.startLogin(context)

    fun handleCallback(context: android.content.Context, uri: android.net.Uri): Boolean =
        oauthManager.handleCallback(context, uri)

    fun saveToken(token: String) {
        tokenStore.saveAccessToken(token)
    }

    fun clearToken() {
        tokenStore.clear()
    }
}
