package com.kicklite.presentation.player

import com.kicklite.domain.model.ChannelDetails
import com.kicklite.domain.model.ChatMessage
import com.kicklite.domain.model.LiveStream
import com.kicklite.data.model.StreamQuality

data class PlayerState(
    val isLoading: Boolean = false,
    val stream: LiveStream? = null,
    val channelDetails: ChannelDetails? = null,
    val chatMessages: List<ChatMessage> = emptyList(),
    val isChatEnabled: Boolean = true,
    val error: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val showControls: Boolean = true,
    val isFullscreen: Boolean = false,
    val isPipMode: Boolean = false,
    val isMuted: Boolean = false,
    val streamUrl: String? = null,
    val isFollowed: Boolean = false,
    val availableQualities: List<StreamQuality> = emptyList(),
    val selectedQuality: StreamQuality = StreamQuality.AUTO,
    val isLoadingQualities: Boolean = false,
    val manifestUrl: String? = null,
    val isLiveBehind: Boolean = false,
    val liveBehindSeconds: Long = 0,
    val playbackMode: PlaybackMode = PlaybackMode.NORMAL
)

enum class PlaybackMode {
    NORMAL,            // Video + chat
    CHAT_ONLY,         // Only chat, stop playback
    AUDIO_WITH_CHAT    // Audio continues, no video view
}
