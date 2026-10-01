package com.kickstra.presentation.channel_profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kickstra.domain.model.ChannelDetails
import com.kickstra.domain.model.ChannelVideo
import com.kickstra.domain.model.ChannelClip
import com.kickstra.R
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelProfileScreen(
    username: String,
    onNavigateBack: () -> Unit,
    onVideoClick: (String) -> Unit = {},
    onLiveStreamClick: (ChannelDetails) -> Unit = {},
    onClipClick: (title: String, url: String, views: Int?, durationSec: Int?, date: String?) -> Unit = { _, _, _, _, _ -> },
    viewModel: ChannelProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(username) {
        viewModel.loadChannelProfile(username)
    }
    
    // Refresh follow status when channelDetails is loaded
    LaunchedEffect(uiState.channelDetails) {
        if (uiState.channelDetails != null) {
            viewModel.refreshFollowStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = uiState.channelDetails?.user?.username ?: username,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    // Follow Button in Header
                    if (uiState.channelDetails != null) {
                        Button(
                            onClick = { viewModel.toggleFollowStatus() },
                            enabled = !uiState.isFollowLoading,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .height(32.dp),
                            colors = if (uiState.isFollowed) {
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                ButtonDefaults.buttonColors()
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            if (uiState.isFollowLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isFollowed) Icons.Default.Check else Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (uiState.isFollowed) stringResource(R.string.following) else stringResource(R.string.follow),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        var selectedTab by rememberSaveable { mutableStateOf(0) }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Channel Profile Header
            item {
                ChannelProfileHeader(
                    channelDetails = uiState.channelDetails,
                    isLoading = uiState.isLoadingChannel,
                    error = uiState.channelError,
                    onLiveStreamClick = onLiveStreamClick
                )
            }

            // Tabs for Videos / Clips
            item {
                Column {
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text(stringResource(R.string.channel_tab_videos)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text(stringResource(R.string.channel_tab_clips)) }
                        )
                    }
                    // Quick filter chips shown only for Clips tab
                    if (selectedTab == 1) {
                        ClipsFilterBar(
                            selectedSort = uiState.clipsSort,
                            selectedTime = uiState.clipsTime,
                            onSortChange = { newSort ->
                                viewModel.setClipsFilter(sort = newSort, time = uiState.clipsTime)
                            },
                            onTimeChange = { newTime ->
                                viewModel.setClipsFilter(sort = uiState.clipsSort, time = newTime)
                            }
                        )
                    }
                }
            }

            if (selectedTab == 0) {
                // Videos Content
                when {
                    uiState.isLoadingVideos -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    uiState.videosError != null -> {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = uiState.videosError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.loadChannelProfile(username) }
                                ) {
                                    Text(stringResource(R.string.retry))
                                }
                            }
                        }
                    }
                    uiState.videos.isEmpty() -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.channel_no_videos),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                    else -> {
                        items(uiState.videos) { video ->
                            VideoItem(
                                video = video,
                                username = username,
                                viewModel = viewModel,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                onClick = {
                                    onVideoClick(video.id.toString())
                                }
                            )
                        }
                    }
                }
            } else {
                // Clips Content
                when {
                    uiState.isLoadingClips -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    uiState.clipsError != null -> {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = uiState.clipsError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { viewModel.refreshClips(username) }) {
                                    Text(stringResource(R.string.retry))
                                }
                            }
                        }
                    }
                    uiState.clips.isEmpty() -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.channel_no_clips),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                    else -> {
                        items(uiState.clips) { clip ->
                            val url = clip.videoUrl ?: clip.clipUrl
                            ClipItem(
                                clip = clip,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                onClick = {
                                    if (!url.isNullOrBlank()) {
                                        onClipClick(
                                            clip.title,
                                            url,
                                            clip.views ?: clip.viewCount,
                                            clip.duration,
                                            clip.createdAt ?: clip.startedAt
                                        )
                                    }
                                }
                            )
                        }
                        // Load more button
                        if (uiState.clipsNextCursor != null) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(onClick = { viewModel.loadMoreClips() }, enabled = !uiState.isLoadingClips) {
                                        Text(stringResource(R.string.load_more))
                                    }
                                    if (uiState.isLoadingClips) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClipsFilterBar(
    selectedSort: String,
    selectedTime: String,
    onSortChange: (String) -> Unit,
    onTimeChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Sort chips
        Text(
            text = stringResource(R.string.sort_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val sortOptions = listOf(
                "view" to R.string.clips_sort_most_viewed,
                "date" to R.string.clips_sort_newest
            )
            sortOptions.forEach { (key, labelRes) ->
                SimpleChoiceChip(
                    selected = selectedSort == key,
                    onClick = { if (selectedSort != key) onSortChange(key) },
                    label = stringResource(labelRes)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Time chips
        Text(
            text = stringResource(R.string.time_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val timeOptions = listOf(
                "day" to R.string.clips_time_day,
                "week" to R.string.clips_time_week,
                "month" to R.string.clips_time_month,
                "all" to R.string.clips_time_all
            )
            timeOptions.forEach { (key, labelRes) ->
                SimpleChoiceChip(
                    selected = selectedTime == key,
                    onClick = { if (selectedTime != key) onTimeChange(key) },
                    label = stringResource(labelRes)
                )
            }
        }
    }
}

@Composable
private fun SimpleChoiceChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    // Use Kick brand green when selected
    val containerColor = if (selected) com.kickstra.ui.theme.KickGreen else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (selected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = if (selected) 2.dp else 0.dp,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ClipItem(
    clip: ChannelClip,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable { onClick() } else it },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                if (!clip.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(clip.thumbnailUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = stringResource(R.string.clip_thumbnail_content_description),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.clip_default_title),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Duration badge
                clip.duration?.let { dur ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = formatClipDuration(dur),
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Info
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = clip.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.views_count, clip.views ?: clip.viewCount ?: 0),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatClipDate(clip.createdAt),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                clip.category?.let { cat ->
                    Text(
                        text = cat.name,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelProfileHeader(
    channelDetails: ChannelDetails?,
    isLoading: Boolean,
    error: String?,
    onLiveStreamClick: (ChannelDetails) -> Unit = {}
) {
    when {
        isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        error != null -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        channelDetails != null -> {
            Column {
                // Banner Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    if (channelDetails.bannerImage?.src != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(channelDetails.bannerImage.src)
                                .crossfade(true)
                                .build(),
                            contentDescription = stringResource(R.string.channel_banner_content_description),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                                        )
                                    )
                                )
                        )
                    }
                }

                // Profile Info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Picture with Live Indicator
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clickable {
                                if (channelDetails.livestream?.isLive == true) {
                                    onLiveStreamClick(channelDetails)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Live border (kırmızı çerçeve)
                        if (channelDetails.livestream?.isLive == true) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                                    .padding(3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (channelDetails.user.profilePic != null) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(channelDetails.user.profilePic)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = stringResource(R.string.profile_picture_content_description),
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = stringResource(R.string.default_profile_icon_description),
                                            modifier = Modifier.size(40.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                if (channelDetails.user.profilePic != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(channelDetails.user.profilePic)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = stringResource(R.string.profile_picture_content_description),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = stringResource(R.string.default_profile_icon_description),
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        
                        // Live yazısı
                        if (channelDetails.livestream?.isLive == true) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .offset(y = (-5).dp)
                                    .background(
                                        Color.Red,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.live),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Channel Info
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = channelDetails.user.username,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (channelDetails.user.bio?.isNotBlank() == true) {
                            Text(
                                text = channelDetails.user.bio,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Live status indicator
                        if (channelDetails.livestream?.isLive == true) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color.Red, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.channel_live_now),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.Red
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(
                                            R.string.viewers,
                                            channelDetails.livestream?.viewerCount ?: 0
                                        ),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                Button(
                                    onClick = { 
                                        onLiveStreamClick(channelDetails)
                                    },
                                    modifier = Modifier.padding(top = 8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Red,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(
                                        text = stringResource(R.string.watch_live_stream),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.followers_count, channelDetails.followersCount),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            if (channelDetails.verified) {
                                Text(
                                    text = stringResource(R.string.verified_label),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoItem(
    video: ChannelVideo,
    username: String,
    viewModel: ChannelProfileViewModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    // Cache the watch progress to avoid recalculating
    val watchProgress = remember(video.id, username) {
        viewModel.getVideoWatchProgress(video.id.toString(), username)
    }
    
    val isCompleted = remember(video.id, username) {
        viewModel.isVideoCompleted(video.id.toString(), username)
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Video Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                if (video.thumbnail?.src != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(video.thumbnail.src)
                            .crossfade(true)
                            .build(),
                        contentDescription = stringResource(R.string.video_thumbnail_content_description),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.video_placeholder),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Duration Badge
                if (video.duration > 0) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = formatDuration(video.duration),
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Live Badge
                if (video.isLive) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Red
                    ) {
                        Text(
                            text = stringResource(R.string.live),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                
                // Watch Progress Bar (at bottom of thumbnail)
                if (!video.isLive) {
                    if (watchProgress > 0f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(4.dp)
                                .padding(horizontal = 0.dp)
                        ) {
                            // Background bar (slightly transparent black)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Color.Black.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                                    )
                            )
                            
                            // Progress bar  
                            val progressColor = if (isCompleted) {
                                Color(0xFF4CAF50) // Green for completed videos
                            } else {
                                Color(0xFFFF0000) // Red for in-progress videos
                            }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(watchProgress)
                                    .background(
                                        progressColor,
                                        shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = if (watchProgress >= 0.99f) 12.dp else 0.dp)
                                    )
                            )
                        }
                    }
                }
                
                // Watched/Completed Badge
                if (!video.isLive) {
                    if (isCompleted) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            shape = CircleShape,
                            color = Color(0xFF4CAF50),
                            shadowElevation = 2.dp
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.watched_badge),
                                tint = Color.White,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(2.dp)
                            )
                        }
                    }
                }
            }

            // Video Info
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = video.sessionTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.views_count, video.views),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = formatDateTime(video.createdAt),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (video.categories.isNotEmpty()) {
                    Text(
                        text = video.categories.first().name,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    if (seconds <= 0) return "00:00"

    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%d:%02d", minutes, secs)
    }
}

private fun formatDateTime(dateTime: String): String {
    // Simple date formatting - in a real app you'd use proper date parsing
    return try {
        dateTime.substring(0, 10) // Just return the date part
    } catch (e: Exception) {
        dateTime
    }
}

private fun formatClipDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", mins, secs)
}

private fun formatClipDate(dateTime: String?): String {
    if (dateTime.isNullOrBlank()) return ""
    return try {
        dateTime.substring(0, 10)
    } catch (e: Exception) {
        dateTime
    }
}
