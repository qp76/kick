package com.kickstra.presentation

import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.kickstra.R
import com.kickstra.presentation.shared.MiniPlayerContent

@Composable
fun MiniPlayerNew(
    content: MiniPlayerContent,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetY by remember { mutableStateOf(0f) }
    val dismissThreshold = 100f
    val context = LocalContext.current
    
    // Get SharedDataViewModel from Activity
    val mainActivity = context as? com.kickstra.presentation.MainActivity
    val sharedDataViewModel = mainActivity?.sharedDataViewModel
    val playerManager = mainActivity?.playerManager
    val window = mainActivity?.window
    
    // Observe video playing state
    val isVideoPlaying by (sharedDataViewModel?.isVideoPlaying?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) })
    val streamUrl = sharedDataViewModel?.getStreamUrl()

    fun shouldKeepScreenOn(player: Player?): Boolean = player?.let {
        it.playWhenReady &&
            (it.playbackState == Player.STATE_READY || it.playbackState == Player.STATE_BUFFERING)
    } == true

    fun updateKeepScreenOn(player: Player?) {
        if (window == null) return
        if (shouldKeepScreenOn(player)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    DisposableEffect(playerManager, window) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                updateKeepScreenOn(playerManager?.exoPlayer)
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                updateKeepScreenOn(playerManager?.exoPlayer)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateKeepScreenOn(playerManager?.exoPlayer)
            }
        }

        playerManager?.addListener(listener)
        updateKeepScreenOn(playerManager?.exoPlayer)

        onDispose {
            playerManager?.removeListener(listener)
            if (!shouldKeepScreenOn(playerManager?.exoPlayer)) {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(300)
        ),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(300)
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .offset(y = (offsetY / 3).dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            if (offsetY > dismissThreshold) {
                                onDismiss()
                            } else {
                                offsetY = 0f
                            }
                        }
                    ) { _, dragAmount ->
                        val newOffset = offsetY + dragAmount.y
                        if (newOffset >= 0) {
                            offsetY = newOffset
                        }
                    }
                }
                .clickable { onTap() },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail
                val thumbnailUrl = when (content) {
                    is MiniPlayerContent.LiveStreamContent -> content.stream.thumbnail?.src
                    is MiniPlayerContent.VideoContent -> content.video.thumbnail?.src
                    is MiniPlayerContent.ClipContent -> content.thumbnailUrl
                }
                
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = stringResource(R.string.thumbnail_content_description),
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop,
                    fallback = painterResource(id = android.R.drawable.ic_media_play)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Content info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    val title = when (content) {
                        is MiniPlayerContent.LiveStreamContent -> content.stream.title
                        is MiniPlayerContent.VideoContent -> content.video.sessionTitle
                        is MiniPlayerContent.ClipContent -> content.title
                    }
                    
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Live indicator if video is playing and it's a live stream
                        when (content) {
                            is MiniPlayerContent.LiveStreamContent -> {
                                if (isVideoPlaying && streamUrl != null) {
                                    Card(
                                        modifier = Modifier.size(8.dp),
                                        shape = RoundedCornerShape(4.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color.Red
                                        )
                                    ) {}
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.live),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Red
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = content.stream.channel.username,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            is MiniPlayerContent.VideoContent -> {
                                if (isVideoPlaying && streamUrl != null) {
                                    Card(
                                        modifier = Modifier.size(8.dp),
                                        shape = RoundedCornerShape(4.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color.Blue
                                        )
                                    ) {}
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.mini_player_video_label),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Blue
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = content.username,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            is MiniPlayerContent.ClipContent -> {
                                if (isVideoPlaying && streamUrl != null) {
                                    Card(
                                        modifier = Modifier.size(8.dp),
                                        shape = RoundedCornerShape(4.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {}
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.mini_player_clip_label),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = content.username ?: "",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                
                // Play/Pause button
                IconButton(
                    onClick = { 
                        // Get PlayerManager from MainActivity and toggle play/pause
                        val playerManager = mainActivity?.playerManager
                        if (isVideoPlaying) {
                            playerManager?.pause()
                        } else {
                            playerManager?.play()
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isVideoPlaying) {
                            Icons.Default.Pause
                        } else {
                            Icons.Default.PlayArrow
                        },
                        contentDescription = if (isVideoPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Close button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
