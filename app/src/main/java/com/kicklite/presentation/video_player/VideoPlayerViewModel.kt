package com.kicklite.presentation.video_player

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kicklite.R
import com.kicklite.data.local.QualityPreferences
import com.kicklite.data.model.StreamQuality
import com.kicklite.data.parser.SimpleM3U8Parser
import com.kicklite.domain.model.ChannelVideo
import com.kicklite.domain.usecase.GetChannelVideosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    private val getChannelVideosUseCase: GetChannelVideosUseCase,
    private val watchHistoryManager: com.kicklite.data.local.VideoWatchHistoryManager,
    private val qualityPreferences: QualityPreferences,
    okHttpClient: OkHttpClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoPlayerUiState())
    val uiState: StateFlow<VideoPlayerUiState> = _uiState.asStateFlow()

    // Video quality management
    private val _availableQualities = MutableStateFlow(getDefaultQualities())
    val availableQualities: StateFlow<List<StreamQuality>> = _availableQualities.asStateFlow()

    private val _selectedQuality = MutableStateFlow(StreamQuality.AUTO)
    val selectedQuality: StateFlow<StreamQuality> = _selectedQuality.asStateFlow()

    // Currently playing URL - changes based on selected quality
    private val _currentVideoUrl = MutableStateFlow<String?>(null)
    val currentVideoUrl: StateFlow<String?> = _currentVideoUrl.asStateFlow()

    private val httpClient = okHttpClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
    private var originalVideoUrl: String? = null

    fun loadVideo(videoId: String, username: String) {
        println("DEBUG: Loading video with ID: $videoId from channel: $username")
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessageRes = null,
                errorMessageArgs = emptyList()
            )
            
            try {
                // Gerçek API'den video verisini çekiyoruz
                val channelUsername = username // Artık parametreden geliyor
                
                getChannelVideosUseCase(channelUsername).collect { result ->
                    result.fold(
                        onSuccess = { videos ->
                            println("DEBUG: Received ${videos.size} videos from API")
                            videos.forEach { video ->
                                println("DEBUG: Available video ID: ${video.id}, title: ${video.sessionTitle}")
                            }
                            
                            val video = videos.find { it.id.toString() == videoId }
                            if (video != null) {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    video = video,
                                    errorMessageRes = null,
                                    errorMessageArgs = emptyList()
                                )
                                println("DEBUG: Video loaded successfully: ${video.sessionTitle}")
                                
                                // Initialize video qualities for this specific video
                                initializeVideoQualities(video)
                                
                                // Check for resume position from watch history
                                checkAndLoadResumePosition(videoId, username)
                            } else {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    errorMessageRes = R.string.error_video_not_found,
                                    errorMessageArgs = listOf(videoId)
                                )
                                println("DEBUG: Video not found with ID: $videoId")
                            }
                        },
                        onFailure = { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessageRes = R.string.error_loading_video,
                                errorMessageArgs = emptyList()
                            )
                            println("DEBUG: Video loading error: ${exception.message}")
                        }
                    )
                }
            } catch (exception: Exception) {
                println("DEBUG: Video loading exception: ${exception.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessageRes = R.string.error_failed_to_load_video,
                    errorMessageArgs = emptyList()
                )
            }
        }
    }

    fun onQualitySelected(quality: StreamQuality, currentPosition: Long = 0L) {
        val normalizedQuality = if (quality.id == StreamQuality.AUTO.id) {
            StreamQuality.AUTO.copy(url = originalVideoUrl ?: quality.url)
        } else {
            quality
        }
        _selectedQuality.value = normalizedQuality
        println("DEBUG: Quality selected: ${normalizedQuality.name} at position: $currentPosition ms")
        
        // Store current position for resuming playback
        val resumePosition = currentPosition
        
        // Update current video URL based on selected quality
        if (normalizedQuality.id == StreamQuality.AUTO.id) {
            // Use original URL for auto quality
            _currentVideoUrl.value = originalVideoUrl
        } else {
            // Use specific quality URL
            _currentVideoUrl.value = normalizedQuality.url
        }
        
        // Trigger position restoration after URL change
        _resumePosition.value = resumePosition
        
        println("DEBUG: Video URL changed to: ${_currentVideoUrl.value}")
        println("DEBUG: Will resume at position: $resumePosition ms")

        qualityPreferences.saveSelectedQuality(normalizedQuality)
    }
    
    // Add state for resume position
    private val _resumePosition = MutableStateFlow(0L)
    val resumePosition: StateFlow<Long> = _resumePosition.asStateFlow()
    
    fun clearResumePosition() {
        _resumePosition.value = 0L
    }
    
    // Video ve kullanıcı bilgileri için current state
    private var currentVideoId: String? = null
    private var currentUsername: String? = null
    
    /**
     * Kaldığı yerden devam etme pozisyonunu kontrol et ve yükle
     */
    private fun checkAndLoadResumePosition(videoId: String, username: String) {
        currentVideoId = videoId
        currentUsername = username
        
        viewModelScope.launch {
            val watchHistory = watchHistoryManager.getWatchPosition(videoId, username)
            if (watchHistory != null) {
                _resumePosition.value = watchHistory.lastWatchedPosition
                println("DEBUG: Found resume position: ${watchHistory.lastWatchedPosition}ms for video $videoId")
                println("DEBUG: Video was ${(watchHistory.lastWatchedPosition / 1000)} seconds in when last watched")
            } else {
                _resumePosition.value = 0L
                println("DEBUG: No resume position found for video $videoId")
            }
        }
    }
    
    /**
     * Video izleme pozisyonunu kaydet (periyodik olarak çağrılacak)
     */
    fun saveCurrentWatchPosition(currentPosition: Long, totalDuration: Long) {
        val videoId = currentVideoId
        val username = currentUsername
        
        if (videoId != null && username != null) {
            viewModelScope.launch {
                watchHistoryManager.saveWatchPosition(
                    videoId = videoId,
                    username = username,
                    currentPosition = currentPosition,
                    totalDuration = totalDuration
                )
            }
        }
    }
    
    /**
     * Video tamamlandığında çağır (izleme geçmişinden kaldırmak için)
     */
    fun markVideoAsCompleted() {
        val videoId = currentVideoId
        val username = currentUsername
        
        if (videoId != null && username != null) {
            viewModelScope.launch {
                watchHistoryManager.markVideoAsCompleted(videoId, username)
            }
        }
    }
    
    /**
     * Bu video için izleme geçmişini temizle
     */
    fun clearVideoWatchHistory() {
        val videoId = currentVideoId
        val username = currentUsername
        
        if (videoId != null && username != null) {
            viewModelScope.launch {
                watchHistoryManager.clearWatchHistory(videoId, username)
            }
        }
    }
    
    /**
     * Tam ekran modunu toggle et
     */
    fun onFullscreenToggle() {
        _uiState.value = _uiState.value.copy(
            isFullscreen = !_uiState.value.isFullscreen
        )
    }
    
    fun prepareForPipMode() {
        // First enter fullscreen if not already
        if (!_uiState.value.isFullscreen) {
            _uiState.value = _uiState.value.copy(isFullscreen = true)
        }
        // Signal that we should enter PiP mode
        _uiState.value = _uiState.value.copy(shouldEnterPip = true)
    }
    
    fun onPipModeEntered() {
        _uiState.value = _uiState.value.copy(
            shouldEnterPip = false,
            isPipModeEnabled = true
        )
    }
    
    fun onPipModeExited() {
        _uiState.value = _uiState.value.copy(
            isPipModeEnabled = false,
            isFullscreen = false
        )
    }
    
    fun clearPipRequest() {
        _uiState.value = _uiState.value.copy(shouldEnterPip = false)
    }

    private fun initializeVideoQualities(video: ChannelVideo) {
        val videoUrl = video.source ?: return
        originalVideoUrl = videoUrl
        _currentVideoUrl.value = videoUrl
        
        println("DEBUG: Initializing video qualities for URL: $videoUrl")
        
        viewModelScope.launch {
            try {
                // For testing, always try to fetch from a test master.m3u8 URL if available
                val testUrls = listOf(
                    videoUrl, // Original URL
                    convertToMasterUrl(videoUrl), // Converted URL
                    "https://sample-videos.com/zip/10/mp4/SampleVideo_1280x720_1mb.mp4" // Fallback for testing
                )
                
                for (testUrl in testUrls) {
                    if (testUrl.contains(".m3u8") || testUrl.contains("master")) {
                        println("DEBUG: Testing URL: $testUrl")
                        
                        val realQualities = fetchVideoQualities(testUrl)
                        if (realQualities.isNotEmpty()) {
                            _availableQualities.value = realQualities
                            applySavedQualitySelection(realQualities)
                            println("DEBUG: Successfully loaded ${realQualities.size} qualities from: $testUrl")
                            return@launch
                        } else {
                            println("DEBUG: No qualities found from: $testUrl")
                        }
                    }
                }
                
                // Fallback: Try to convert regular video URL to M3U8 master URL
                val possibleMasterUrl = convertToMasterUrl(videoUrl)
                if (possibleMasterUrl != videoUrl) {
                    println("DEBUG: Trying converted master URL: $possibleMasterUrl")
                    try {
                        val qualities = fetchVideoQualities(possibleMasterUrl)
                        if (qualities.size > 1) {
                            _availableQualities.value = qualities
                            applySavedQualitySelection(qualities)
                            println("DEBUG: Successfully loaded ${qualities.size} qualities from converted master URL")
                            return@launch
                        }
                    } catch (e: Exception) {
                        println("DEBUG: Failed to fetch from converted URL: ${e.message}")
                    }
                }
                
                // Final fallback: Use default qualities with the original URL
                println("DEBUG: Using fallback qualities")
                val fallbackQualities = createFallbackQualities(videoUrl)
                _availableQualities.value = fallbackQualities
                applySavedQualitySelection(fallbackQualities)
                
            } catch (e: Exception) {
                println("DEBUG: Error initializing qualities: ${e.message}")
                e.printStackTrace()
                val defaultQualities = getDefaultQualities()
                _availableQualities.value = defaultQualities
                applySavedQualitySelection(defaultQualities)
            }
        }
    }

    private fun applySavedQualitySelection(qualities: List<StreamQuality>) {
        if (qualities.isEmpty()) return

        val savedQuality = qualityPreferences.getSavedQuality()

        val matchedQuality = when {
            savedQuality.id == StreamQuality.AUTO.id -> StreamQuality.AUTO.copy(url = originalVideoUrl ?: savedQuality.url)
            else -> qualities.firstOrNull { it.id == savedQuality.id }
                ?: qualities.firstOrNull { it.name.equals(savedQuality.name, ignoreCase = true) }
        }

        val resolvedQuality = matchedQuality ?: StreamQuality.AUTO.copy(url = originalVideoUrl ?: "")

        _selectedQuality.value = resolvedQuality
        _currentVideoUrl.value = if (resolvedQuality.id == StreamQuality.AUTO.id) {
            originalVideoUrl
        } else {
            resolvedQuality.url
        }

        // Update stored preference with the latest quality metadata (e.g., fresh URL)
        qualityPreferences.saveSelectedQuality(resolvedQuality)
    }
    
    private fun convertToMasterUrl(videoUrl: String): String {
        // Try to detect common video URL patterns and convert them to master.m3u8
        return when {
            videoUrl.contains("stream.kick.com") && !videoUrl.contains("master.m3u8") -> {
                // If it's a Kick stream URL but not master.m3u8, try to convert it
                if (videoUrl.contains("/hls/")) {
                    // Replace any specific quality path with master.m3u8
                    val baseHlsUrl = videoUrl.substring(0, videoUrl.indexOf("/hls/") + 5)
                    "${baseHlsUrl}master.m3u8"
                } else {
                    "$videoUrl/master.m3u8"
                }
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
    
    private suspend fun fetchVideoQualities(manifestUrl: String): List<StreamQuality> = withContext(Dispatchers.IO) {
        try {
            println("DEBUG: Fetching manifest from URL: $manifestUrl")

            val request = Request.Builder()
                .url(manifestUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .addHeader("Accept", "*/*")
                .addHeader("Origin", "https://kick.com")
                .addHeader("Referer", "https://kick.com/")
                .build()

            httpClient.newCall(request).execute().use { response ->
                println("DEBUG: HTTP Response code: ${response.code}")
                println("DEBUG: Response headers: ${response.headers}")

                if (response.isSuccessful) {
                    val manifestContent = response.body?.string()
                    println("DEBUG: Manifest content length: ${manifestContent?.length}")

                    if (!manifestContent.isNullOrEmpty()) {
                        println("DEBUG: Manifest content preview:")
                        manifestContent.lines().take(10).forEachIndexed { index, line ->
                            println("DEBUG: Line $index: $line")
                        }

                        val qualities = SimpleM3U8Parser.parseManifest(manifestContent, manifestUrl)
                        println("DEBUG: Successfully parsed ${qualities.size} qualities from real manifest")

                        qualities.forEach { quality ->
                            println("DEBUG: Real Quality: ${quality.name} (${quality.id}) - ${quality.resolution} - ${quality.url}")
                        }

                        return@use qualities
                    } else {
                        println("DEBUG: Empty manifest content")
                    }
                } else {
                    println("DEBUG: HTTP request failed with code: ${response.code}")
                    println("DEBUG: Error body: ${response.body?.string()}")
                }

                emptyList()
            }
        } catch (e: Exception) {
            println("DEBUG: Exception while fetching manifest: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    private fun getDefaultQualities(): List<StreamQuality> {
        return listOf(StreamQuality.AUTO)
    }
}

data class VideoPlayerUiState(
    val isLoading: Boolean = false,
    val video: ChannelVideo? = null,
    @StringRes val errorMessageRes: Int? = null,
    val errorMessageArgs: List<Any> = emptyList(),
    val isFullscreen: Boolean = false,
    val shouldEnterPip: Boolean = false,
    val isPipModeEnabled: Boolean = false
)
