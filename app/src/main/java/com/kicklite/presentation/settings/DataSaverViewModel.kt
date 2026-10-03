package com.kicklite.presentation.settings

import androidx.lifecycle.ViewModel
import com.kicklite.data.local.DataSaverPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DataSaverViewModel @Inject constructor(
    val prefs: DataSaverPreferences
) : ViewModel()
