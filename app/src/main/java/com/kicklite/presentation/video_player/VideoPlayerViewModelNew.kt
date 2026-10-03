package com.kicklite.presentation.video_player

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kicklite.data.model.StreamQuality
import com.kicklite.domain.model.ChannelVideo
import com.kicklite.domain.usecase.GetChannelVideosUseCase
import com.kicklite.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoPlayerViewModelNew @Inject constructor(
    private val getChannelVideosUseCase: GetChannelVideosUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoPlayerUiStateNew())
    val uiState: StateFlow<VideoPlayerUiStateNew> = _uiState.asStateFlow()

    // Video quality management
    private val _availableQualities = MutableStateFlow(getDefaultQualities())
    val availableQualities: StateFlow<List<StreamQuality>> = _availableQualities.asStateFlow()

    private val _selectedQuality = MutableStateFlow(StreamQuality.AUTO)
    val selectedQuality: StateFlow<StreamQuality> = _selectedQuality.asStateFlow()

    fun loadVideo(videoId: String, username: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    errorMessageRes = null,
                    errorMessageArgs = emptyList()
                )
                
                val channelUsername = username
                
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

    fun onQualitySelected(quality: StreamQuality) {
        _selectedQuality.value = quality
        println("DEBUG: Quality selected: ${quality.name}")
    }

    private fun initializeVideoQualities(video: ChannelVideo) {
        // Generate different quality options for the video
        val baseUrl = video.source ?: ""
        val qualities = mutableListOf<StreamQuality>()
        
        // Add auto quality first
        qualities.add(StreamQuality.AUTO)
        
        // Add standard video qualities
        qualities.addAll(listOf(
            StreamQuality(
                id = "1080p",
                name = "1080p",
                resolution = "1920x1080",
                bandwidth = 8000000,
                frameRate = 60.0,
                url = baseUrl,
                isDefault = false
            ),
            StreamQuality(
                id = "720p",
                name = "720p HD",
                resolution = "1280x720",
                bandwidth = 5000000,
                frameRate = 60.0,
                url = baseUrl,
                isDefault = false
            ),
            StreamQuality(
                id = "480p",
                name = "480p",
                resolution = "854x480",
                bandwidth = 2500000,
                frameRate = 30.0,
                url = baseUrl,
                isDefault = false
            ),
            StreamQuality(
                id = "360p",
                name = "360p",
                resolution = "640x360",
                bandwidth = 1000000,
                frameRate = 30.0,
                url = baseUrl,
                isDefault = false
            )
        ))
        
        _availableQualities.value = qualities
    }

    private fun getDefaultQualities(): List<StreamQuality> {
        return listOf(StreamQuality.AUTO)
    }
}

data class VideoPlayerUiStateNew(
    val isLoading: Boolean = false,
    val video: ChannelVideo? = null,
    @StringRes val errorMessageRes: Int? = null,
    val errorMessageArgs: List<Any> = emptyList()
)
