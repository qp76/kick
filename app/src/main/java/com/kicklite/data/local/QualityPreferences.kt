package com.kicklite.data.local

import android.content.Context
import android.content.SharedPreferences
import com.kicklite.data.model.StreamQuality
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QualityPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "quality_preferences", 
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_SELECTED_QUALITY_ID = "selected_quality_id"
        private const val KEY_SELECTED_QUALITY_NAME = "selected_quality_name"
        private const val KEY_SELECTED_QUALITY_RESOLUTION = "selected_quality_resolution"
        private const val KEY_SELECTED_QUALITY_BANDWIDTH = "selected_quality_bandwidth"
        private const val KEY_SELECTED_QUALITY_FRAME_RATE = "selected_quality_frame_rate"
        private const val KEY_SELECTED_QUALITY_URL = "selected_quality_url"
        private const val KEY_SELECTED_QUALITY_IS_DEFAULT = "selected_quality_is_default"
        private const val KEY_PLAYBACK_MODE = "playback_mode"
    }
    
    fun saveSelectedQuality(quality: StreamQuality) {
        prefs.edit().apply {
            putString(KEY_SELECTED_QUALITY_ID, quality.id)
            putString(KEY_SELECTED_QUALITY_NAME, quality.name)
            putString(KEY_SELECTED_QUALITY_RESOLUTION, quality.resolution)
            putLong(KEY_SELECTED_QUALITY_BANDWIDTH, quality.bandwidth)
            putFloat(KEY_SELECTED_QUALITY_FRAME_RATE, quality.frameRate.toFloat())
            putString(KEY_SELECTED_QUALITY_URL, quality.url)
            putBoolean(KEY_SELECTED_QUALITY_IS_DEFAULT, quality.isDefault)
            apply()
        }
    }
    
    fun getSavedQuality(): StreamQuality {
        return if (prefs.contains(KEY_SELECTED_QUALITY_ID)) {
            StreamQuality(
                id = prefs.getString(KEY_SELECTED_QUALITY_ID, "auto") ?: "auto",
                name = prefs.getString(KEY_SELECTED_QUALITY_NAME, "Auto") ?: "Auto",
                resolution = prefs.getString(KEY_SELECTED_QUALITY_RESOLUTION, "Auto") ?: "Auto",
                bandwidth = prefs.getLong(KEY_SELECTED_QUALITY_BANDWIDTH, 0),
                frameRate = prefs.getFloat(KEY_SELECTED_QUALITY_FRAME_RATE, 0f).toDouble(),
                url = prefs.getString(KEY_SELECTED_QUALITY_URL, "") ?: "",
                isDefault = prefs.getBoolean(KEY_SELECTED_QUALITY_IS_DEFAULT, true)
            )
        } else {
            StreamQuality.AUTO
        }
    }
    
    fun clearSavedQuality() {
        prefs.edit().apply {
            remove(KEY_SELECTED_QUALITY_ID)
            remove(KEY_SELECTED_QUALITY_NAME)
            remove(KEY_SELECTED_QUALITY_RESOLUTION)
            remove(KEY_SELECTED_QUALITY_BANDWIDTH)
            remove(KEY_SELECTED_QUALITY_FRAME_RATE)
            remove(KEY_SELECTED_QUALITY_URL)
            remove(KEY_SELECTED_QUALITY_IS_DEFAULT)
            apply()
        }
    }

    // Playback mode persistence (stores enum name as String)
    fun savePlaybackMode(modeName: String) {
        prefs.edit().putString(KEY_PLAYBACK_MODE, modeName).apply()
    }

    fun getSavedPlaybackMode(): String? = prefs.getString(KEY_PLAYBACK_MODE, null)
}
