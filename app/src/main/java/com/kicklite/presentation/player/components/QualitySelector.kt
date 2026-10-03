package com.kicklite.presentation.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kicklite.data.model.StreamQuality
import com.kicklite.presentation.player.PlaybackMode
import com.kicklite.R

@Composable
fun QualitySelector(
    qualities: List<StreamQuality>,
    selectedQuality: StreamQuality,
    onQualitySelected: (StreamQuality) -> Unit,
    onSelectChatOnly: (() -> Unit)? = null,
    onSelectAudioWithChat: (() -> Unit)? = null,
    currentMode: PlaybackMode = PlaybackMode.NORMAL,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    
    // Debug logs
    println("DEBUG QualitySelector: Received ${qualities.size} qualities")
    qualities.forEach { quality ->
        println("DEBUG QualitySelector: ${quality.name} (${quality.id})")
    }
    
    // Quality button (shows mode when a non-normal mode is active) 
    FilledTonalButton(
        onClick = { 
            println("DEBUG QualitySelector: Button clicked, showing dialog")
            showDialog = true 
        },
        modifier = modifier,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = Color.Black.copy(alpha = 0.6f),
            contentColor = Color.White
        )
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = stringResource(R.string.quality_settings),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        val buttonLabel = when (currentMode) {
            PlaybackMode.CHAT_ONLY -> stringResource(R.string.mode_chat_only)
            PlaybackMode.AUDIO_WITH_CHAT -> stringResource(R.string.mode_audio_chat)
            else -> selectedQuality.name
        }
        Text(text = buttonLabel, style = MaterialTheme.typography.bodySmall)
    }
    
    // Quality selection dialog
    if (showDialog) {
        QualitySelectionDialog(
            qualities = qualities,
            selectedQuality = selectedQuality,
            onQualitySelected = { quality ->
                onQualitySelected(quality)
                // Returning to NORMAL video mode when a quality is chosen
                showDialog = false
            },
            onSelectChatOnly = onSelectChatOnly,
            onSelectAudioWithChat = onSelectAudioWithChat,
            currentMode = currentMode,
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun QualitySelectionDialog(
    qualities: List<StreamQuality>,
    selectedQuality: StreamQuality,
    onQualitySelected: (StreamQuality) -> Unit,
    onSelectChatOnly: (() -> Unit)? = null,
    onSelectAudioWithChat: (() -> Unit)? = null,
    currentMode: PlaybackMode = PlaybackMode.NORMAL,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.video_quality),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Special modes
                if (onSelectChatOnly != null || onSelectAudioWithChat != null) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (onSelectChatOnly != null) {
                            ModeItem(
                                title = stringResource(R.string.mode_chat_only),
                                subtitle = stringResource(R.string.mode_chat_only_description),
                                isSelected = currentMode == PlaybackMode.CHAT_ONLY,
                                onClick = {
                                    onSelectChatOnly()
                                    onDismiss()
                                }
                            )
                        }
                        if (onSelectAudioWithChat != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            ModeItem(
                                title = stringResource(R.string.mode_audio_chat),
                                subtitle = stringResource(R.string.mode_audio_chat_description),
                                isSelected = currentMode == PlaybackMode.AUDIO_WITH_CHAT,
                                onClick = {
                                    onSelectAudioWithChat()
                                    onDismiss()
                                }
                            )
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(qualities) { quality ->
                        QualityItem(
                            quality = quality,
                            isSelected = (currentMode == PlaybackMode.NORMAL) && quality.id == selectedQuality.id,
                            onClick = { onQualitySelected(quality) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    }
}

@Composable
private fun QualityItem(
    quality: StreamQuality,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = quality.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurface
            )
            
            if (quality.id != "auto") {
                Text(
                    text = buildString {
                        append(quality.resolution)
                        if (quality.frameRate > 30) {
                            append(" • ${quality.frameRate.toInt()}fps")
                        }
                        if (quality.bandwidth > 0) {
                            val mbps = quality.bandwidth / 1000000.0
                            append(" • ${"%.1f".format(mbps)} Mbps")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = stringResource(R.string.automatically_adjust_quality),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.selected),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ModeItem(
    title: String,
    subtitle: String,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.selected),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
