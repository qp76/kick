package com.kickstra.presentation.player.components

import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import androidx.hilt.navigation.compose.hiltViewModel
import com.kickstra.presentation.player.PlayerManager
import com.kickstra.R
import javax.inject.Inject

@Composable
fun VideoPlayer(
    streamUrl: String?,
    manifestUrl: String?,
    selectedQuality: com.kickstra.data.model.StreamQuality,
    isPlaying: Boolean,
    isMuted: Boolean = false,
    showVideo: Boolean = true,
    onBufferingChanged: (Boolean) -> Unit,
    onPlayerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Get PlayerManager from MainActivity
    val mainActivity = context as? com.kickstra.presentation.MainActivity
    val playerManager = mainActivity?.playerManager
    val exoPlayer = playerManager?.exoPlayer
    val window = mainActivity?.window

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

    // Update media item when stream URL or quality changes
    LaunchedEffect(streamUrl, selectedQuality) {
        if (streamUrl != null && manifestUrl != null) {
            println("DEBUG: Setting media with quality: ${selectedQuality.name}")
            playerManager?.setStreamQuality(
                selectedQuality, 
                manifestUrl, 
                com.kickstra.presentation.player.PlayerManager.StreamType.LIVE_STREAM,
                savePreference = false
            )
        }
    }

    // Control play/pause
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            playerManager?.play()
        } else {
            playerManager?.pause()
        }
    }

    // Listen for player state changes
    DisposableEffect(playerManager, window) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        println("DEBUG: ExoPlayer STATE_BUFFERING")
                        onBufferingChanged(true)
                    }
                    Player.STATE_READY -> {
                        println("DEBUG: ExoPlayer STATE_READY")
                        onBufferingChanged(false)
                    }
                    Player.STATE_ENDED -> {
                        println("DEBUG: ExoPlayer STATE_ENDED")
                        onBufferingChanged(false)
                    }
                    Player.STATE_IDLE -> {
                        println("DEBUG: ExoPlayer STATE_IDLE")
                        onBufferingChanged(false)
                    }
                }
                updateKeepScreenOn(exoPlayer)
            }
            
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                println("DEBUG: ExoPlayer error: ${error.message}")
                println("DEBUG: Error cause: ${error.cause}")
                println("DEBUG: Error code: ${error.errorCode}")
                onBufferingChanged(false)
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                updateKeepScreenOn(exoPlayer)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateKeepScreenOn(exoPlayer)
            }
        }
        
        playerManager?.addListener(listener)
        updateKeepScreenOn(exoPlayer)
        
        onDispose {
            playerManager?.removeListener(listener)
            // Don't release player - keep it for background playback
            if (!shouldKeepScreenOn(exoPlayer)) {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    // Handle mute state changes
    LaunchedEffect(isMuted) {
        playerManager?.setVolume(if (isMuted) 0f else 1f)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (streamUrl != null && exoPlayer != null && showVideo) {
            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {
                        player = exoPlayer
                        useController = false // We'll use custom controls
                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                        setOnClickListener { onPlayerClick() }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else if (streamUrl != null && exoPlayer != null && !showVideo) {
            // Audio-only placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.audio_playing),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            // Show placeholder when no stream URL
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Gray.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.no_stream_available),
                        modifier = Modifier.padding(16.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
