package com.kicklite.presentation.player.components

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Fullscreen gestures: tap = show/hide controls, double tap = play/pause,
 * swipe up/down on the left half = brightness, on the right half = volume.
 */
@Composable
fun PlayerGestureLayer(
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }

    var brightness by remember {
        mutableStateOf(
            activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.5f
        )
    }
    var volume by remember {
        mutableStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume)
    }
    var hud by remember { mutableStateOf<String?>(null) }

    // Give brightness back to the system when leaving the player
    DisposableEffect(Unit) {
        onDispose {
            activity?.window?.let {
                val lp = it.attributes
                lp.screenBrightness = -1f
                it.attributes = lp
            }
        }
    }

    // Hide the percentage label shortly after the finger lifts
    LaunchedEffect(hud) {
        if (hud != null) {
            kotlinx.coroutines.delay(900)
            hud = null
        }
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTap() }, onDoubleTap = { onDoubleTap() })
            }
            .pointerInput(Unit) {
                var leftSide = true
                detectVerticalDragGestures(
                    onDragStart = { offset -> leftSide = offset.x < size.width / 2f }
                ) { change, dragAmount ->
                    change.consume()
                    val delta = -dragAmount / size.height * 1.5f
                    if (leftSide) {
                        brightness = (brightness + delta).coerceIn(0.02f, 1f)
                        activity?.window?.let {
                            val lp = it.attributes
                            lp.screenBrightness = brightness
                            it.attributes = lp
                        }
                        hud = "Brightness ${(brightness * 100).roundToInt()}%"
                    } else {
                        volume = (volume + delta).coerceIn(0f, 1f)
                        audio.setStreamVolume(AudioManager.STREAM_MUSIC, (volume * maxVolume).roundToInt(), 0)
                        hud = "Volume ${(volume * 100).roundToInt()}%"
                    }
                }
            }
    ) {
        hud?.let {
            Text(
                text = it,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}
