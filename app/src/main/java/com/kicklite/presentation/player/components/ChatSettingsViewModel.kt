package com.kicklite.presentation.player.components

import androidx.lifecycle.ViewModel
import com.kicklite.data.local.ChatPreferences
import com.kicklite.data.local.AdvancedChatPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChatSettingsViewModel @Inject constructor(
    val prefs: ChatPreferences,
    val advancedPrefs: AdvancedChatPreferences
) : ViewModel()
