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
import com.kickstra.domain.model.LiveStream

@Composable
fun MiniPlayer(
    stream: LiveStream,
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
        ),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .offset(y = offsetY.dp)
                .clickable { onTap() }
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
                        offsetY += dragAmount.y
                        if (offsetY < 0) offsetY = 0f
                    }
                },
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail
                AsyncImage(
                    model = stream.thumbnail?.src,
                    contentDescription = stringResource(R.string.stream_thumbnail_content_description),
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop,
                    fallback = painterResource(id = android.R.drawable.ic_media_play)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Stream info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stream.title,
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
                        // Live indicator if video is playing
                        if (isVideoPlaying && streamUrl != null) {
                            Card(
                                modifier = Modifier
                                    .size(8.dp),
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
                            text = stream.channel.username,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Play/Pause button
                IconButton(
                    onClick = { 
                        // Toggle play/pause functionality could be added here
                        // For now just showing the state
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (isVideoPlaying) {
                                android.R.drawable.ic_media_pause
                            } else {
                                android.R.drawable.ic_media_play
                            }
                        ),
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
                        painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                        contentDescription = stringResource(R.string.close),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
