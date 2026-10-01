package com.kickstra.presentation.shared

import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.ViewModel
import com.kickstra.domain.model.LiveStream
import com.kickstra.domain.model.ChannelVideo
import com.kickstra.domain.model.ChannelDetails
import com.kickstra.domain.model.StreamChannel
import com.kickstra.domain.model.StreamCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

// Sealed class for different media types
sealed class MiniPlayerContent {
    data class LiveStreamContent(val stream: LiveStream) : MiniPlayerContent()
    data class VideoContent(val video: ChannelVideo, val username: String) : MiniPlayerContent()
    data class ClipContent(
        val title: String,
        val url: String,
        val views: Int? = null,
        val durationSec: Int? = null,
        val date: String? = null,
        val thumbnailUrl: String? = null,
        val username: String? = null
    ) : MiniPlayerContent()
}

@HiltViewModel
class SharedDataViewModel @Inject constructor() : ViewModel() {
    
    private var selectedStream: LiveStream? = null
    
    private val _isPipMode = MutableStateFlow(false)
    val isPipMode: StateFlow<Boolean> = _isPipMode.asStateFlow()
    
    // Mini player state
    private val _isMiniPlayerVisible = MutableStateFlow(false)
    val isMiniPlayerVisible: StateFlow<Boolean> = _isMiniPlayerVisible.asStateFlow()
    
    private var miniPlayerContent: MiniPlayerContent? = null
    
    // Video player state for mini player
    private val _isVideoPlaying = MutableStateFlow(false)
    val isVideoPlaying: StateFlow<Boolean> = _isVideoPlaying.asStateFlow()
    
    private var currentStreamUrl: String? = null
    
    // Home screen state preservation
    private var savedHomeListState: LazyListState? = null
    
    fun setSelectedStream(stream: LiveStream) {
        selectedStream = stream
    }
    
    fun setSelectedStreamFromChannel(channelDetails: ChannelDetails) {
        val livestream = channelDetails.livestream
        if (livestream != null && livestream.isLive) {
            val stream = LiveStream(
                id = livestream.id.toString(),
                title = livestream.sessionTitle,
                viewerCount = livestream.viewerCount,
                thumbnail = livestream.thumbnail,
                startTime = livestream.startTime ?: "",
                channel = StreamChannel(
                    id = channelDetails.id,
                    username = channelDetails.user.username,
                    slug = channelDetails.slug,
                    profilePic = channelDetails.user.profilePic
                ),
                category = StreamCategory(
                    id = livestream.categories.firstOrNull()?.id ?: 0,
                    name = livestream.categories.firstOrNull()?.name ?: "Just Chatting",
                    slug = livestream.categories.firstOrNull()?.slug ?: "just-chatting"
                ),
                language = livestream.language,
                isMature = livestream.isMature,
                tags = livestream.tags
            )
            selectedStream = stream
        }
    }
    
    fun getSelectedStream(): LiveStream? {
        return selectedStream
    }
    
    fun clearSelectedStream() {
        selectedStream = null
    }
    
    fun setPipMode(isInPip: Boolean) {
        _isPipMode.value = isInPip
    }
    
    fun showMiniPlayer(stream: LiveStream) {
        miniPlayerContent = MiniPlayerContent.LiveStreamContent(stream)
        _isMiniPlayerVisible.value = true
    }
    
    fun showMiniPlayerForVideo(video: ChannelVideo, username: String) {
        miniPlayerContent = MiniPlayerContent.VideoContent(video, username)
        _isMiniPlayerVisible.value = true
    }
    
    fun showMiniPlayerForClip(
        title: String,
        url: String,
        views: Int? = null,
        durationSec: Int? = null,
        date: String? = null,
        thumbnailUrl: String? = null,
        username: String? = null
    ) {
        miniPlayerContent = MiniPlayerContent.ClipContent(
            title = title,
            url = url,
            views = views,
            durationSec = durationSec,
            date = date,
            thumbnailUrl = thumbnailUrl,
            username = username
        )
        _isMiniPlayerVisible.value = true
    }
    
    fun hideMiniPlayer() {
        _isMiniPlayerVisible.value = false
        miniPlayerContent = null
        // Don't automatically stop video when mini player is closed
        // Let the player manager handle the transition
    }
    
    fun getMiniPlayerContent(): MiniPlayerContent? {
        return miniPlayerContent
    }
    
    // Backward compatibility for existing code
    fun getMiniPlayerStream(): LiveStream? {
        return when (val content = miniPlayerContent) {
            is MiniPlayerContent.LiveStreamContent -> content.stream
            else -> null
        }
    }
    
    // Video player state management
    fun setVideoState(isPlaying: Boolean, streamUrl: String?) {
        _isVideoPlaying.value = isPlaying
        currentStreamUrl = streamUrl
    }
    
    fun getStreamUrl(): String? {
        return currentStreamUrl
    }
    
    fun saveHomeListState(listState: LazyListState) {
        savedHomeListState = listState
    }
    
    fun getSavedHomeListState(): LazyListState? {
        return savedHomeListState
    }
}
