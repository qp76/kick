package com.kicklite.presentation.settings

import androidx.lifecycle.ViewModel
import com.kicklite.auth.KickAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(val auth: KickAuthManager) : ViewModel()
