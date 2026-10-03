package com.kicklite.presentation.clip

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kicklite.presentation.video_player.components.VideoPlayer
import com.kicklite.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipPlayerScreen(
    title: String?,
    url: String?,
    views: Int? = null,
    durationSec: Int? = null,
    date: String? = null,
    onBack: () -> Unit,
    viewModel: ClipPlayerViewModel = hiltViewModel()
) {
    val availableQualities by viewModel.availableQualities.collectAsState()
    val selectedQuality by viewModel.selectedQuality.collectAsState()
    val currentVideoUrl by viewModel.currentVideoUrl.collectAsState()
    val isFullscreen by viewModel.isFullscreen.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(url) {
        viewModel.loadClip(url)
    }

    // Back handler
    BackHandler {
        if (isFullscreen) {
            viewModel.toggleFullscreen()
        } else {
            // If video is playing, show mini player for clip
            val mainActivity = context as? com.kicklite.presentation.MainActivity
            val playerManager = mainActivity?.playerManager
            val sharedDataViewModel = mainActivity?.sharedDataViewModel
            if (!currentVideoUrl.isNullOrBlank() && playerManager?.isPlaying() == true) {
                sharedDataViewModel?.showMiniPlayerForClip(
                    title = title ?: "",
                    url = currentVideoUrl!!,
                    views = views,
                    durationSec = durationSec,
                    date = date
                )
            }
            onBack()
        }
    }

    if (isFullscreen) {
        // Fullscreen content without topbar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            VideoPlayer(
                videoUrl = currentVideoUrl,
                availableQualities = availableQualities,
                selectedQuality = selectedQuality,
                onQualitySelected = { quality, pos -> viewModel.onQualitySelected(quality, pos) },
                onFullscreenToggle = { viewModel.toggleFullscreen() },
                isFullscreen = true,
                isPipMode = false,
                modifier = Modifier.fillMaxSize(),
                controlsPersistent = false,
                showQualitySelector = false
            )
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = title ?: stringResource(R.string.clip_default_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (!currentVideoUrl.isNullOrBlank()) {
                    val configuration = LocalConfiguration.current
                    val thirdHeight = (configuration.screenHeightDp.dp) / 3
                    VideoPlayer(
                        videoUrl = currentVideoUrl,
                        availableQualities = availableQualities,
                        selectedQuality = selectedQuality,
                        onQualitySelected = { quality, pos -> viewModel.onQualitySelected(quality, pos) },
                        onFullscreenToggle = { viewModel.toggleFullscreen() },
                        isFullscreen = false,
                        isPipMode = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(thirdHeight.coerceAtLeast(180.dp)),
                        controlsPersistent = true,
                        showQualitySelector = false
                    )

                    // Metadata section under player
                    ClipMetaRow(
                        views = views,
                        durationSec = durationSec,
                        date = date,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.clip_url_not_found),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClipMetaRow(
    views: Int?,
    durationSec: Int?,
    date: String?,
    modifier: Modifier = Modifier
) {
    if (views == null && durationSec == null && date.isNullOrBlank()) return
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (views != null) {
            MetaPill(
                icon = Icons.Filled.Visibility,
                text = stringResource(R.string.clip_views, formatViews(views))
            )
        }
        if (durationSec != null) {
            MetaPill(icon = Icons.Filled.AccessTime, text = formatDuration(durationSec))
        }
        if (!date.isNullOrBlank()) {
            val dateText = (date.takeIf { it.length >= 10 }?.substring(0, 10)) ?: date
            MetaPill(icon = Icons.Filled.DateRange, text = dateText)
        }
    }
}

private fun formatViews(v: Int): String {
    return when {
        v >= 1_000_000 -> String.format("%.1fM", v / 1_000_000.0)
        v >= 1_000 -> String.format("%.1fk", v / 1_000.0)
        else -> v.toString()
    }
}

private fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%d:%02d", minutes, secs)
    }
}

@Composable
private fun MetaPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 1.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(text = text, style = MaterialTheme.typography.labelMedium)
        }
    }
}
