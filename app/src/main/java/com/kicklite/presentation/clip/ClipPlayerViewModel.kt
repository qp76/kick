package com.kicklite.presentation.clip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kicklite.data.model.StreamQuality
import com.kicklite.data.parser.SimpleM3U8Parser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

@HiltViewModel
class ClipPlayerViewModel @Inject constructor() : ViewModel() {

    private val _availableQualities = MutableStateFlow(getDefaultQualities())
    val availableQualities: StateFlow<List<StreamQuality>> = _availableQualities.asStateFlow()

    private val _selectedQuality = MutableStateFlow(StreamQuality.AUTO)
    val selectedQuality: StateFlow<StreamQuality> = _selectedQuality.asStateFlow()

    private val _currentVideoUrl = MutableStateFlow<String?>(null)
    val currentVideoUrl: StateFlow<String?> = _currentVideoUrl.asStateFlow()

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private var originalVideoUrl: String? = null

    fun loadClip(url: String?) {
        if (url.isNullOrBlank()) return
        originalVideoUrl = url
        _currentVideoUrl.value = url
        initializeQualities(url)
    }

    fun onQualitySelected(quality: StreamQuality, currentPosition: Long = 0L) {
        _selectedQuality.value = quality
        // Update current video URL based on selected quality
        _currentVideoUrl.value = if (quality.id == "auto") originalVideoUrl else quality.url
        // For clips, we rely on VideoPlayer to handle seeking back to the given position
    }

    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
    }

    private fun initializeQualities(videoUrl: String) {
        viewModelScope.launch {
            try {
                val testUrls = listOf(
                    videoUrl,
                    convertToMasterUrl(videoUrl)
                )

                for (testUrl in testUrls) {
                    if (testUrl.contains(".m3u8") || testUrl.contains("master")) {
                        val realQualities = fetchVideoQualities(testUrl)
                        if (realQualities.isNotEmpty()) {
                            _availableQualities.value = realQualities
                            return@launch
                        }
                    }
                }

                val possibleMasterUrl = convertToMasterUrl(videoUrl)
                if (possibleMasterUrl != videoUrl) {
                    try {
                        val qualities = fetchVideoQualities(possibleMasterUrl)
                        if (qualities.size > 1) {
                            _availableQualities.value = qualities
                            return@launch
                        }
                    } catch (_: Exception) { }
                }

                _availableQualities.value = createFallbackQualities(videoUrl)
            } catch (e: Exception) {
                e.printStackTrace()
                _availableQualities.value = getDefaultQualities()
            }
        }
    }

    private fun convertToMasterUrl(videoUrl: String): String {
        return when {
            videoUrl.contains("/hls/") && !videoUrl.endsWith("master.m3u8") -> {
                val baseHlsUrl = videoUrl.substring(0, videoUrl.indexOf("/hls/") + 5)
                "${baseHlsUrl}master.m3u8"
            }
            else -> videoUrl
        }
    }

    private fun createFallbackQualities(baseUrl: String): List<StreamQuality> {
        val baseUrlPath = if (baseUrl.contains("/hls/")) {
            baseUrl.substring(0, baseUrl.indexOf("/hls/") + 5)
        } else {
            val lastSlash = baseUrl.lastIndexOf('/')
            if (lastSlash != -1) baseUrl.substring(0, lastSlash + 1) else baseUrl
        }

        return listOf(
            StreamQuality.AUTO.copy(url = baseUrl),
            StreamQuality(
                id = "1080p60",
                name = "1080p60",
                resolution = "1920x1080",
                bandwidth = 8773023,
                frameRate = 60.0,
                url = "${baseUrlPath}1080p60/playlist.m3u8",
                isDefault = false
            ),
            StreamQuality(
                id = "720p60",
                name = "720p60 HD",
                resolution = "1280x720",
                bandwidth = 3484182,
                frameRate = 60.0,
                url = "${baseUrlPath}720p60/playlist.m3u8",
                isDefault = false
            ),
            StreamQuality(
                id = "480p30",
                name = "480p",
                resolution = "852x480",
                bandwidth = 1489182,
                frameRate = 30.0,
                url = "${baseUrlPath}480p30/playlist.m3u8",
                isDefault = false
            ),
            StreamQuality(
                id = "360p30",
                name = "360p",
                resolution = "640x360",
                bandwidth = 630000,
                frameRate = 30.0,
                url = "${baseUrlPath}360p30/playlist.m3u8",
                isDefault = false
            ),
            StreamQuality(
                id = "160p30",
                name = "160p",
                resolution = "284x160",
                bandwidth = 230000,
                frameRate = 30.0,
                url = "${baseUrlPath}160p30/playlist.m3u8",
                isDefault = false
            )
        )
    }

    private suspend fun fetchVideoQualities(manifestUrl: String): List<StreamQuality> {
        return try {
            val request = Request.Builder()
                .url(manifestUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .addHeader("Accept", "*/*")
                .addHeader("Origin", "https://kick.com")
                .addHeader("Referer", "https://kick.com/")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val manifestContent = response.body?.string()
                    if (!manifestContent.isNullOrEmpty()) {
                        val qualities = SimpleM3U8Parser.parseManifest(manifestContent, manifestUrl)
                        return qualities.ifEmpty { getDefaultQualities() }
                    }
                }
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun getDefaultQualities(): List<StreamQuality> = listOf(StreamQuality.AUTO)
}
