package com.kickstra.presentation.channel_profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kickstra.domain.model.ChannelDetails
import com.kickstra.domain.model.ChannelVideo
import com.kickstra.domain.usecase.GetChannelDetailsUseCase
import com.kickstra.domain.usecase.GetChannelVideosUseCase
import com.kickstra.domain.usecase.ToggleFollowChannelUseCase
import com.kickstra.domain.usecase.IsChannelFollowedUseCase
import com.kickstra.domain.usecase.GetChannelClipsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChannelProfileViewModel @Inject constructor(
    private val getChannelDetailsUseCase: GetChannelDetailsUseCase,
    private val getChannelVideosUseCase: GetChannelVideosUseCase,
    private val toggleFollowChannelUseCase: ToggleFollowChannelUseCase,
    private val isChannelFollowedUseCase: IsChannelFollowedUseCase,
    private val getChannelClipsUseCase: GetChannelClipsUseCase,
    private val watchHistoryManager: com.kickstra.data.local.VideoWatchHistoryManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChannelProfileUiState())
    val uiState: StateFlow<ChannelProfileUiState> = _uiState.asStateFlow()

    fun loadChannelProfile(username: String) {
        println("DEBUG: Loading channel profile for: $username")
        
        // Reset follow state
        _uiState.value = _uiState.value.copy(isFollowed = false)
        
        // Load channel details
        viewModelScope.launch {
            getChannelDetailsUseCase(username).collect { resource ->
                when (resource) {
                    is com.kickstra.domain.common.Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(
                            isLoadingChannel = true,
                            channelError = null
                        )
                    }
                    is com.kickstra.domain.common.Resource.Success -> {
                        println("DEBUG: Channel details loaded: ${resource.data?.slug}")
                        _uiState.value = _uiState.value.copy(
                            isLoadingChannel = false,
                            channelDetails = resource.data,
                            channelError = null
                        )
                        
                        // Check follow status when channel details are loaded
                        resource.data?.let { channelDetails ->
                            checkFollowStatus(channelDetails.id)
                        }
                    }
                    is com.kickstra.domain.common.Resource.Error -> {
                        println("DEBUG: Channel details error: ${resource.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoadingChannel = false,
                            channelError = resource.message
                        )
                    }
                }
            }
        }

        // Load channel videos
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingVideos = true, videosError = null)
            
            try {
                getChannelVideosUseCase(username).collect { result ->
                    if (result.isSuccess) {
                        val videos = result.getOrNull() ?: emptyList()
                        println("DEBUG: Channel videos loaded: ${videos.size} videos")
                        _uiState.value = _uiState.value.copy(
                            isLoadingVideos = false,
                            videos = videos,
                            videosError = null
                        )
                    } else {
                        val exception = result.exceptionOrNull()
                        println("DEBUG: Channel videos error: ${exception?.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoadingVideos = false,
                            videosError = exception?.message ?: "Failed to load videos"
                        )
                    }
                }
            } catch (exception: Exception) {
                println("DEBUG: Channel videos exception: ${exception.message}")
                _uiState.value = _uiState.value.copy(
                    isLoadingVideos = false,
                    videosError = exception.message ?: "Failed to load videos"
                )
            }
        }
        
        // Load channel clips (initial)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingClips = true, clipsError = null)
            try {
                getChannelClipsUseCase(username, sort = clipsSort, time = clipsTime, cursor = null).collect { result ->
                    if (result.isSuccess) {
                        val (clips, nextCursor) = result.getOrNull() ?: Pair(emptyList(), null)
                        println("DEBUG: Channel clips loaded: ${clips.size} clips")
                        _uiState.value = _uiState.value.copy(
                            isLoadingClips = false,
                            clips = clips,
                            clipsNextCursor = nextCursor,
                            clipsError = null
                        )
                    } else {
                        val exception = result.exceptionOrNull()
                        println("DEBUG: Channel clips error: ${exception?.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoadingClips = false,
                            clipsError = exception?.message ?: "Failed to load clips"
                        )
                    }
                }
            } catch (e: Exception) {
                println("DEBUG: Channel clips exception: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoadingClips = false,
                    clipsError = e.message ?: "Failed to load clips"
                )
            }
        }
    }
    
    private fun checkFollowStatus(channelId: Long) {
        viewModelScope.launch {
            try {
                println("DEBUG: Checking follow status for channelId: $channelId")
                val isFollowed = isChannelFollowedUseCase(channelId)
                println("DEBUG: Channel $channelId follow status: $isFollowed")
                _uiState.value = _uiState.value.copy(isFollowed = isFollowed)
            } catch (e: Exception) {
                println("DEBUG: Error checking follow status: ${e.message}")
                // Don't show error for follow status check
            }
        }
    }
    
    fun toggleFollowStatus() {
        val channelDetails = _uiState.value.channelDetails ?: return
        val isCurrentlyFollowed = _uiState.value.isFollowed
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isFollowLoading = true)
            
            try {
                toggleFollowChannelUseCase(
                    channelId = channelDetails.id.toString(),
                    isCurrentlyFollowed = isCurrentlyFollowed,
                    username = channelDetails.user.username
                )
                
                // Update the UI state immediately
                _uiState.value = _uiState.value.copy(
                    isFollowed = !isCurrentlyFollowed,
                    isFollowLoading = false
                )
                
                println("DEBUG: Successfully ${if (isCurrentlyFollowed) "unfollowed" else "followed"} channel")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isFollowLoading = false)
                println("DEBUG: Exception during follow toggle: ${e.message}")
            }
        }
    }
    
    fun refreshFollowStatus() {
        val channelDetails = _uiState.value.channelDetails
        if (channelDetails != null) {
            checkFollowStatus(channelDetails.id)
        }
    }

    fun getCurrentChannelDetails(): ChannelDetails? {
        return _uiState.value.channelDetails
    }

    // Expose current clips sort/time
    private val clipsSort: String
        get() = _uiState.value.clipsSort
    private val clipsTime: String
        get() = _uiState.value.clipsTime

    fun setClipsFilter(sort: String, time: String) {
        val username = _uiState.value.channelDetails?.user?.username ?: return
        _uiState.value = _uiState.value.copy(clipsSort = sort, clipsTime = time)
        refreshClips(username)
    }

    fun refreshClips(username: String? = null) {
        val user = username ?: _uiState.value.channelDetails?.user?.username ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingClips = true, clipsError = null)
            try {
                getChannelClipsUseCase(user, sort = clipsSort, time = clipsTime, cursor = null).collect { result ->
                    if (result.isSuccess) {
                        val (clips, nextCursor) = result.getOrNull() ?: Pair(emptyList(), null)
                        _uiState.value = _uiState.value.copy(
                            isLoadingClips = false,
                            clips = clips,
                            clipsNextCursor = nextCursor,
                            clipsError = null
                        )
                    } else {
                        val exception = result.exceptionOrNull()
                        _uiState.value = _uiState.value.copy(
                            isLoadingClips = false,
                            clipsError = exception?.message ?: "Failed to load clips"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingClips = false,
                    clipsError = e.message ?: "Failed to load clips"
                )
            }
        }
    }

    fun loadMoreClips() {
        val username = _uiState.value.channelDetails?.user?.username ?: return
        val cursor = _uiState.value.clipsNextCursor ?: return
        if (_uiState.value.isLoadingClips) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingClips = true)
            try {
                getChannelClipsUseCase(username, sort = clipsSort, time = clipsTime, cursor = cursor).collect { result ->
                    if (result.isSuccess) {
                        val (newClips, nextCursor) = result.getOrNull() ?: Pair(emptyList(), null)
                        _uiState.value = _uiState.value.copy(
                            isLoadingClips = false,
                            clips = _uiState.value.clips + newClips,
                            clipsNextCursor = nextCursor
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoadingClips = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingClips = false)
            }
        }
    }
    
    /**
     * Get video watch progress for a specific video
     * Returns percentage (0.0 to 1.0) of how much the video has been watched
     */
    fun getVideoWatchProgress(videoId: String, username: String): Float {
        return try {
            val watchHistory = watchHistoryManager.getWatchPosition(videoId, username)
            if (watchHistory != null && watchHistory.totalDuration > 0) {
                // If video is marked as completed, don't show progress bar
                if (watchHistory.isCompleted) {
                    println("DEBUG: Video $videoId is completed - no progress bar shown")
                    return 0f
                }
                
                val progress = watchHistory.lastWatchedPosition.toFloat() / watchHistory.totalDuration.toFloat()
                val clampedProgress = progress.coerceIn(0f, 1f)
                
                println("DEBUG: Video $videoId progress: ${(clampedProgress * 100).toInt()}% (${watchHistory.lastWatchedPosition}ms/${watchHistory.totalDuration}ms)")
                
                clampedProgress
            } else {
                println("DEBUG: No watch history for video $videoId")
                0f
            }
        } catch (e: Exception) {
            println("DEBUG: Error getting watch progress for video $videoId: ${e.message}")
            0f
        }
    }
    
    /**
     * Check if video has been completed (watched more than 90%)
     */
    fun isVideoCompleted(videoId: String, username: String): Boolean {
        return try {
            val isCompleted = watchHistoryManager.isVideoCompleted(videoId, username)
            println("DEBUG: Video $videoId completion status: $isCompleted")
            isCompleted
        } catch (e: Exception) {
            println("DEBUG: Error checking completion for video $videoId: ${e.message}")
            false
        }
    }
}

data class ChannelProfileUiState(
    val isLoadingChannel: Boolean = false,
    val isLoadingVideos: Boolean = false,
    val isLoadingClips: Boolean = false,
    val channelDetails: ChannelDetails? = null,
    val videos: List<ChannelVideo> = emptyList(),
    val clips: List<com.kickstra.domain.model.ChannelClip> = emptyList(),
    val channelError: String? = null,
    val videosError: String? = null,
    val clipsError: String? = null,
    val clipsNextCursor: String? = null,
    val clipsSort: String = "view",
    val clipsTime: String = "week",
    val isFollowed: Boolean = false,
    val isFollowLoading: Boolean = false
)
