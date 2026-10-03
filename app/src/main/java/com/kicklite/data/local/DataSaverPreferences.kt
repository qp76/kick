package com.kicklite.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataSaverPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("data_saver", Context.MODE_PRIVATE)
    private val _enabled = MutableStateFlow(prefs.getBoolean("enabled", false))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _previews = MutableStateFlow(prefs.getBoolean("previews", true))
    val previews: StateFlow<Boolean> = _previews.asStateFlow()
    fun setPreviews(v: Boolean) {
        _previews.value = v
        prefs.edit().putBoolean("previews", v).apply()
    }

    fun setEnabled(v: Boolean) {
        _enabled.value = v
        prefs.edit().putBoolean("enabled", v).apply()
    }
}
