package com.kickstra.presentation.video_player.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.kickstra.presentation.player.components.QualitySelector
import com.kickstra.R
import kotlin.math.roundToInt

@Composable
fun VideoPlayerControls(
    isPlaying: Boolean,
    isBuffering: Boolean,
    showControls: Boolean,
    currentPosition: Long,
    duration: Long,
    availableQualities: List<com.kickstra.data.model.StreamQuality>,
    selectedQuality: com.kickstra.data.model.StreamQuality,
    showQualitySelector: Boolean = true,
    playbackSpeed: Float,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onQualitySelected: (com.kickstra.data.model.StreamQuality) -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onControlsToggle: () -> Unit,
    onFullscreenToggle: (() -> Unit)? = null,
    isFullscreen: Boolean = false,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = showControls,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onControlsToggle() }
        ) {
            // Gradient overlay for better visibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.2f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    )
            )

            // Center controls (play/pause, seek buttons)
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Seek back 15 seconds
                IconButton(
                    onClick = { onSeekBack() },
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.FastRewind,
                        contentDescription = stringResource(R.string.seek_back_15_seconds),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Play/Pause button
                IconButton(
                    onClick = { onPlayPause() },
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            CircleShape
                        )
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Seek forward 15 seconds
                IconButton(
                    onClick = { onSeekForward() },
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.FastForward,
                        contentDescription = stringResource(R.string.seek_forward_15_seconds),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Top right controls (optional quality selector) and fullscreen button
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showQualitySelector) {
                    // Debug logs (optional)
                    println("DEBUG VideoPlayerControls: Available qualities count: ${availableQualities.size}")
                    availableQualities.forEachIndexed { index, quality ->
                        println("DEBUG VideoPlayerControls: Quality $index: ${quality.name} - ${quality.id}")
                    }

                    QualitySelector(
                        qualities = availableQualities,
                        selectedQuality = selectedQuality,
                        onQualitySelected = onQualitySelected,
                        modifier = Modifier
                    )
                }

                // Playback speed selector
                var showSpeedMenu by remember { mutableStateOf(false) }
                Box {
                    FilledTonalButton(
                        onClick = { showSpeedMenu = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.6f),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = stringResource(R.string.playback_speed),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val speedLabel = if (playbackSpeed % 1f == 0f) "${playbackSpeed.toInt()}x" else "${"%.2f".format(playbackSpeed)}x"
                        Text(text = speedLabel, style = MaterialTheme.typography.bodySmall)
                    }

                    DropdownMenu(
                        expanded = showSpeedMenu,
                        onDismissRequest = { showSpeedMenu = false },
                    ) {
                        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
                        speeds.forEach { speed ->
                            val label = if (speed % 1f == 0f) "${speed.toInt()}x" else "${"%.2f".format(speed)}x"
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    onSpeedSelected(speed)
                                    showSpeedMenu = false
                                }
                            )
                        }
                    }
                }

                // Fullscreen button
                if (onFullscreenToggle != null) {
                    IconButton(
                        onClick = onFullscreenToggle,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Color.Black.copy(alpha = 0.7f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isFullscreen) stringResource(R.string.exit_fullscreen) else stringResource(R.string.fullscreen),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Bottom controls (seekbar and time)
            if (duration > 0) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    // Time display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formatTime(duration),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Seekbar
                    VideoSeekBar(
                        currentPosition = currentPosition,
                        duration = duration,
                        onSeekTo = onSeekTo,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun VideoSeekBar(
    currentPosition: Long,
    duration: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableStateOf(0f) }

    val progress = if (isDragging) {
        dragPosition
    } else {
        if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
    }

    Slider(
        value = progress,
        onValueChange = { value ->
            isDragging = true
            dragPosition = value
        },
        onValueChangeFinished = {
            val newPosition = (dragPosition * duration).toLong()
            onSeekTo(newPosition)
            isDragging = false
        },
        colors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = Color.Red,
            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
        ),
        modifier = modifier
    )
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
