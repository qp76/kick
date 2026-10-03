package com.kicklite.presentation.player.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MuteButton(
    isMuted: Boolean,
    onMuteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onMuteToggle,
        modifier = modifier
    ) {
        Icon(
            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
            contentDescription = if (isMuted) "Unmute" else "Mute",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}
