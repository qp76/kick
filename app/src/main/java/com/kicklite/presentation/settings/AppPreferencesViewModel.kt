package com.kicklite.presentation.settings

import androidx.lifecycle.ViewModel
import com.kicklite.data.local.AdvancedChatPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppPreferencesViewModel @Inject constructor(val prefs: AdvancedChatPreferences) : ViewModel()
