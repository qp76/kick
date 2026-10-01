package com.kickstra.presentation.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.kickstra.domain.model.ChannelDetails
import com.kickstra.domain.model.LiveStream
import com.kickstra.data.model.StreamQuality
import com.kickstra.R

@Composable
fun VideoPlayerControls(
    isPlaying: Boolean,
    isBuffering: Boolean,
    showControls: Boolean,
    isFullscreen: Boolean,
    stream: LiveStream? = null,
    channelDetails: ChannelDetails? = null,
    isFollowed: Boolean = false,
    isMuted: Boolean = false,
    isLiveBehind: Boolean = false,
    liveBehindSeconds: Long = 0,
    availableQualities: List<StreamQuality> = emptyList(),
    selectedQuality: StreamQuality = StreamQuality.AUTO,
    onPlayPause: () -> Unit,
    onFullscreenToggle: () -> Unit,
    onMuteToggle: () -> Unit,
    onBackClick: () -> Unit,
    onFollowClick: () -> Unit,
    onQualitySelected: (StreamQuality) -> Unit = {},
    onSelectChatOnly: (() -> Unit)? = null,
    onSelectAudioWithChat: (() -> Unit)? = null,
    currentMode: com.kickstra.presentation.player.PlaybackMode = com.kickstra.presentation.player.PlaybackMode.NORMAL,
    onChannelClick: (String) -> Unit = {},
    onLiveClick: () -> Unit = {},
    // When true, the bottom bar (LIVE + Quality) stays visible regardless of showControls
    alwaysShowBottomBar: Boolean = false,
    modifier: Modifier = Modifier
) {
    val showOverlay = showControls || isBuffering || alwaysShowBottomBar
    if (showOverlay) {
        Box(modifier = modifier.fillMaxSize()) {
            // Top controls (only when controls are shown)
            if (showControls || isBuffering) {
                Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .safeDrawingPadding()
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
                ) {
                // Back button - only show when not in fullscreen
                if (!isFullscreen) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    // Empty spacer to maintain layout balance in fullscreen
                    Spacer(modifier = Modifier.size(48.dp))
                }

                channelDetails?.let { channel ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onChannelClick(channel.user.username) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = channel.user.username,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        if (channel.livestream?.isLive == true) {
                            Text(
                                text = stringResource(
                                    R.string.live_viewers_count,
                                    channel.livestream?.viewerCount ?: 0
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                } ?: stream?.let { streamData ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onChannelClick(streamData.channel.username) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = streamData.channel.username,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.live_viewers_count, streamData.viewerCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                // Follow button
                IconButton(
                    onClick = onFollowClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isFollowed) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFollowed) stringResource(R.string.unfollow) else stringResource(R.string.follow),
                        tint = if (isFollowed) Color.Red else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                MuteButton(
                    isMuted = isMuted,
                    onMuteToggle = onMuteToggle,
                    modifier = Modifier.size(48.dp)
                )

                // Fullscreen button - always at the end
                IconButton(
                    onClick = onFullscreenToggle,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isFullscreen) stringResource(R.string.exit_fullscreen) else stringResource(R.string.fullscreen),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // PiP button removed - now automatic on app background
                }
            }

            // Center play/pause button
            if (showControls || isBuffering) {
                if (isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(60.dp)
                            .align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                } else {
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier
                            .size(80.dp)
                            .align(Alignment.Center)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            // Bottom controls
            val showBottomBar = alwaysShowBottomBar || showControls || isBuffering
            if (showBottomBar) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .safeDrawingPadding()
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LIVE indicator / GO TO LIVE button
                    if (channelDetails?.livestream?.isLive == true || stream != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isLiveBehind) 
                                    Color.Gray.copy(alpha = 0.8f) 
                                else 
                                    Color.Red.copy(alpha = 0.8f)
                            ),
                            modifier = Modifier.clickable { onLiveClick() }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (isLiveBehind) stringResource(R.string.go_to_live) else stringResource(R.string.live),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                if (isLiveBehind && liveBehindSeconds > 0) {
                                    Text(
                                        text = stringResource(R.string.live_latency_seconds, liveBehindSeconds),
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.8f
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    
                    // Quality selector
                    if (availableQualities.isNotEmpty()) {
                        QualitySelector(
                            qualities = availableQualities,
                            selectedQuality = selectedQuality,
                            onQualitySelected = onQualitySelected,
                            onSelectChatOnly = onSelectChatOnly,
                            onSelectAudioWithChat = onSelectAudioWithChat,
                            currentMode = currentMode
                        )
                    }
                }
            }
        }
    }
}
