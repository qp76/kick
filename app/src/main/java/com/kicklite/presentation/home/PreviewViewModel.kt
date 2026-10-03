package com.kicklite.presentation.home

import androidx.lifecycle.ViewModel
import com.kicklite.data.local.DataSaverPreferences
import com.kicklite.data.preview.StreamPreviewManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PreviewViewModel @Inject constructor(
    val manager: StreamPreviewManager,
    val settings: DataSaverPreferences
) : ViewModel() {
}
