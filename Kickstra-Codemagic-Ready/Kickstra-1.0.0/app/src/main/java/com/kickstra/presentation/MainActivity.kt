package com.kickstra.presentation

import android.annotation.TargetApi
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.kickstra.presentation.home.HomeScreen
import com.kickstra.presentation.player.PlayerScreenWithStream
import com.kickstra.presentation.player.PipModeManager
import com.kickstra.presentation.following.FollowingScreen
import com.kickstra.presentation.search.SearchScreen
import com.kickstra.presentation.channel_profile.ChannelProfileScreen
import com.kickstra.presentation.settings.SettingsScreen
import com.kickstra.presentation.settings.ThemeViewModel
import com.kickstra.presentation.video_player.VideoPlayerScreen
import com.kickstra.presentation.MiniPlayerNew
import com.kickstra.presentation.shared.SharedDataViewModel
import com.kickstra.presentation.shared.MiniPlayerContent
import com.kickstra.domain.model.LiveStream
import com.kickstra.domain.model.ChannelDetails
import com.kickstra.ui.theme.KickstraTheme
import com.kickstra.R
import dagger.hilt.android.AndroidEntryPoint
import androidx.media3.common.util.UnstableApi
import javax.inject.Inject
import android.Manifest
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.kickstra.presentation.notifications.PlaybackNotificationManager
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import com.kickstra.domain.model.ThemePreference

@UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var pipModeManager: PipModeManager
    
    @Inject
    lateinit var playerManager: com.kickstra.presentation.player.PlayerManager
    
    @Inject
    lateinit var playbackNotificationManager: PlaybackNotificationManager
    
    private var isVideoPlaying = false
    private var isOnPlayerScreen = false
    private var isOnVideoPlayerScreen = false
    private var currentVideoPlayerViewModel: com.kickstra.presentation.video_player.VideoPlayerViewModel? = null
    private var currentPlayerViewModel: com.kickstra.presentation.player.PlayerViewModel? = null
    private var navController: androidx.navigation.NavHostController? = null
    var sharedDataViewModel: SharedDataViewModel? = null
        private set
    
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // If launched from notification, we may need to open the now playing screen
    val openNowPlayingFromIntent = intent?.getBooleanExtra("open_now_playing", false) == true
        // Register runtime permission launcher for Android 13+ notifications
        notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            println("DEBUG: POST_NOTIFICATIONS permission granted = $isGranted")
        }
        enableEdgeToEdge()
        
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val themePreference by themeViewModel.themePreference.collectAsState()
            val systemDarkTheme = isSystemInDarkTheme()
            val useDarkTheme = when (themePreference) {
                ThemePreference.AUTO -> systemDarkTheme
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }
            val useDynamicColor = themePreference == ThemePreference.AUTO

            KickstraTheme(
                darkTheme = useDarkTheme,
                dynamicColor = useDynamicColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val showNotifEdu = remember { mutableStateOf(false) }
                        
                        // Show one-time education sheet on first launch (Android 13+ and permission not granted)
                        LaunchedEffect(Unit) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val granted = ContextCompat.checkSelfPermission(
                                    this@MainActivity,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                val prefs = this@MainActivity.getSharedPreferences("onboarding_prefs", android.content.Context.MODE_PRIVATE)
                                val shown = prefs.getBoolean("notif_edu_shown", false)
                                if (!granted && !shown) {
                                    showNotifEdu.value = true
                                }
                            }
                        }
                        
                        val navController = rememberNavController()
                        this@MainActivity.navController = navController
                        val sharedDataViewModel: SharedDataViewModel = hiltViewModel()
                        this@MainActivity.sharedDataViewModel = sharedDataViewModel
                        
                        // Setup PlayerManager callback to update SharedDataViewModel and notification
                        LaunchedEffect(Unit) {
                            playerManager.setPlaybackStateCallback { isPlaying ->
                                if (isPlaying) requestNotificationPermissionIfNeeded()
                                sharedDataViewModel.setVideoState(isPlaying, playerManager.getCurrentStreamUrl())
                                playbackNotificationManager.onPlaybackStateChanged(isPlaying)
                            }
                        }
                        
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route
                        
                        // Mini player state
                        val isMiniPlayerVisible by sharedDataViewModel.isMiniPlayerVisible.collectAsState()
                        val miniPlayerContent = sharedDataViewModel.getMiniPlayerContent()
                        
                        // Track selected tab manually to avoid race conditions
                        // Educational bottom sheet for notification permission (first run)
                        if (showNotifEdu.value) {
                            ModalBottomSheet(
                                onDismissRequest = {
                                    showNotifEdu.value = false
                                    this@MainActivity.getSharedPreferences("onboarding_prefs", android.content.Context.MODE_PRIVATE)
                                        .edit().putBoolean("notif_edu_shown", true).apply()
                                }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 16.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.notif_permission_title),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(R.string.notif_permission_message),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        OutlinedButton(onClick = {
                                            showNotifEdu.value = false
                                            this@MainActivity.getSharedPreferences("onboarding_prefs", android.content.Context.MODE_PRIVATE)
                                                .edit().putBoolean("notif_edu_shown", true).apply()
                                        }) {
                                            Text(stringResource(R.string.notif_permission_later))
                                        }
                                        Button(onClick = {
                                            // Trigger permission flow then hide
                                            requestNotificationPermissionIfNeeded()
                                            showNotifEdu.value = false
                                            this@MainActivity.getSharedPreferences("onboarding_prefs", android.content.Context.MODE_PRIVATE)
                                                .edit().putBoolean("notif_edu_shown", true).apply()
                                        }) {
                                            Text(stringResource(R.string.notif_permission_allow))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                        var selectedTab by remember { mutableStateOf("home") }
                    
                    // Update selected tab based on current route
                    LaunchedEffect(currentRoute) {
                        Log.d("Navigation", "Current route changed to: $currentRoute")
                        when (currentRoute) {
                            "home" -> selectedTab = "home"
                            "following" -> selectedTab = "following"
                            "search" -> selectedTab = "search"
                            // Don't change selectedTab for player or other screens
                        }
                    }
                    
                    var isFullscreen by remember { mutableStateOf(false) }
                    
                    // Observe PiP mode state
                    val isPipMode by sharedDataViewModel.isPipMode.collectAsState()
                        // If we were launched from notification, navigate to now playing once
                        LaunchedEffect(openNowPlayingFromIntent) {
                            if (openNowPlayingFromIntent) {
                                handleNowPlayingNavigation(navController, sharedDataViewModel)
                                this@MainActivity.intent?.removeExtra("open_now_playing")
                            }
                        }
                    
                    // Determine if we should show bottom bar
                    val isMainScreen = currentRoute in listOf("home", "following", "search")
                    val isPlayerScreen = currentRoute == "player"
                    val isChannelProfileScreen = currentRoute?.startsWith("channel_profile") == true
                    val shouldShowMiniPlayer = isMiniPlayerVisible && miniPlayerContent != null && !isPlayerScreen && !isPipMode
                    val shouldShowBottomBar = !isPipMode && (isMainScreen || (shouldShowMiniPlayer && (isMainScreen || isChannelProfileScreen)))
                    
                    // Debug messages
                    println("DEBUG: Route='$currentRoute', isMainScreen=$isMainScreen, isPlayerScreen=$isPlayerScreen, isChannelProfile=$isChannelProfileScreen, isMiniPlayerVisible=$isMiniPlayerVisible, hasContent=${miniPlayerContent != null}, shouldShowMiniPlayer=$shouldShowMiniPlayer, shouldShowBottomBar=$shouldShowBottomBar, isPipMode=$isPipMode")
                    
                    if (isPipMode) {
                        // In PiP mode, show minimal UI - just the player content
                        Box(modifier = Modifier.fillMaxSize()) {
                            // The player content is already showing in PiP window
                            // Just need a minimal container
                        }
                    } else {
                        // Normal mode with Scaffold
                        Scaffold(
                                modifier = if (!isFullscreen) Modifier.systemBarsPadding() else Modifier,
                                bottomBar = {
                                    if (shouldShowBottomBar && !isFullscreen) {
                                        Column {
                                            // Mini Player - above navigation
                                            if (shouldShowMiniPlayer) {
                                            MiniPlayerNew(
                                                content = miniPlayerContent!!,
                                                isVisible = isMiniPlayerVisible,
                                                onDismiss = {
                                                    sharedDataViewModel.hideMiniPlayer()
                                                    // Stop video when mini player is dismissed
                                                    playerManager.stop()
                                                },
                                                onTap = {
                                                    // Navigate to appropriate player screen based on content type
                                                    when (val content = miniPlayerContent) {
                                                        is MiniPlayerContent.LiveStreamContent -> {
                                                            sharedDataViewModel.setSelectedStream(content.stream)
                                                            sharedDataViewModel.hideMiniPlayer()
                                                            navController.navigate("player")
                                                        }
                                                        is MiniPlayerContent.VideoContent -> {
                                                            sharedDataViewModel.hideMiniPlayer()
                                                            navController.navigate("video_player/${content.username}/${content.video.id}") {
                                                                launchSingleTop = true
                                                                restoreState = true
                                                            }
                                                        }
                                                        is MiniPlayerContent.ClipContent -> {
                                                            sharedDataViewModel.hideMiniPlayer()
                                                            val encodedTitle = android.net.Uri.encode(content.title)
                                                            val encodedUrl = android.net.Uri.encode(content.url)
                                                            val v = content.views ?: 0
                                                            val d = content.durationSec ?: 0
                                                            val encodedDate = android.net.Uri.encode(content.date ?: "")
                                                            navController.navigate("clip_player?title=$encodedTitle&url=$encodedUrl&views=$v&duration=$d&date=$encodedDate")
                                                        }
                                                        null -> {
                                                            // Handle null case - shouldn't happen but required for exhaustive when
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                        
                                        // Navigation Bar - only show on main screens
                                        if (isMainScreen) {
                                            NavigationBar(
                                                containerColor = com.kickstra.ui.theme.KickBlack,
                                                tonalElevation = 0.dp
                                            ) {
                                        NavigationBarItem(
                                            icon = { 
                                                Icon(Icons.Default.Home, contentDescription = stringResource(R.string.nav_home)) 
                                            },
                                            label = { Text(stringResource(R.string.nav_home)) },
                                            selected = selectedTab == "home",
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = com.kickstra.ui.theme.KickBlack,
                                                selectedTextColor = com.kickstra.ui.theme.KickGreen,
                                                indicatorColor = com.kickstra.ui.theme.KickGreen,
                                                unselectedIconColor = com.kickstra.ui.theme.KickMuted,
                                                unselectedTextColor = com.kickstra.ui.theme.KickMuted
                                            ),
                                            onClick = {
                                                Log.d("Navigation", "Home tab clicked, selectedTab: $selectedTab, currentRoute: $currentRoute")
                                                if (selectedTab != "home") {
                                                    selectedTab = "home"
                                                    navController.navigate("home")
                                                }
                                            }
                                        )
                                        NavigationBarItem(
                                            icon = { 
                                                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.nav_search)) 
                                            },
                                            label = { Text(stringResource(R.string.nav_search)) },
                                            selected = selectedTab == "search",
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = com.kickstra.ui.theme.KickBlack,
                                                selectedTextColor = com.kickstra.ui.theme.KickGreen,
                                                indicatorColor = com.kickstra.ui.theme.KickGreen,
                                                unselectedIconColor = com.kickstra.ui.theme.KickMuted,
                                                unselectedTextColor = com.kickstra.ui.theme.KickMuted
                                            ),
                                            onClick = {
                                                Log.d("Navigation", "Search tab clicked, selectedTab: $selectedTab, currentRoute: $currentRoute")
                                                if (selectedTab != "search") {
                                                    selectedTab = "search"
                                                    navController.navigate("search")
                                                }
                                            }
                                        )
                                        NavigationBarItem(
                                            icon = { 
                                                Icon(Icons.Default.Favorite, contentDescription = stringResource(R.string.nav_following)) 
                                            },
                                            label = { Text(stringResource(R.string.nav_following)) },
                                            selected = selectedTab == "following",
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = com.kickstra.ui.theme.KickBlack,
                                                selectedTextColor = com.kickstra.ui.theme.KickGreen,
                                                indicatorColor = com.kickstra.ui.theme.KickGreen,
                                                unselectedIconColor = com.kickstra.ui.theme.KickMuted,
                                                unselectedTextColor = com.kickstra.ui.theme.KickMuted
                                            ),
                                            onClick = {
                                                Log.d("Navigation", "Following tab clicked, selectedTab: $selectedTab, currentRoute: $currentRoute")
                                                if (selectedTab != "following") {
                                                    selectedTab = "following"
                                                    navController.navigate("following")
                                                }
                                            }
                                        )
                                    }
                                        }
                                        }
                                    }
                                },
                            ) { _ ->
                        NavHost(
                            navController = navController,
                            startDestination = "home",
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable("home") {
                                HomeScreen(
                                    onStreamClick = { stream: LiveStream ->
                                        sharedDataViewModel.setSelectedStream(stream)
                                        navController.navigate("player")
                                    },
                                    onSettingsClick = {
                                        navController.navigate("settings")
                                    },
                                    sharedDataViewModel = sharedDataViewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            composable("following") {
                                FollowingScreen(
                                    onStreamClick = { stream: LiveStream ->
                                        sharedDataViewModel.setSelectedStream(stream)
                                        navController.navigate("player")
                                    },
                                    onChannelClick = { username: String ->
                                        navController.navigate("channel_profile/$username")
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            composable("search") {
                                SearchScreen(
                                    onChannelClick = { username: String ->
                                        navController.navigate("channel_profile/$username")
                                    },
                                    onCategoryClick = { categorySlug: String ->
                                        // TODO: Navigate to category screen when implemented
                                    }
                                )
                            }
                            composable("settings") {
                                SettingsScreen(
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable("channel_profile/{username}") { backStackEntry ->
                                val username = backStackEntry.arguments?.getString("username") ?: ""
                                ChannelProfileScreen(
                                    username = username,
                                    onNavigateBack = { navController.popBackStack() },
                                    onVideoClick = { videoId ->
                                        navController.navigate("video_player/$username/$videoId") {
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onLiveStreamClick = { channelDetails ->
                                        // Create LiveStream from ChannelDetails and navigate to player
                                        sharedDataViewModel.setSelectedStreamFromChannel(channelDetails)
                                        navController.navigate("player")
                                    }, 
                                    onClipClick = { title, url, views, durationSec, date ->
                                        val encodedTitle = android.net.Uri.encode(title)
                                        val encodedUrl = android.net.Uri.encode(url)
                                        val v = views ?: -1
                                        val d = durationSec ?: -1
                                        val encodedDate = android.net.Uri.encode(date ?: "")
                                        navController.navigate("clip_player?title=$encodedTitle&url=$encodedUrl&views=$v&duration=$d&date=$encodedDate")
                                    }
                                )
                            }
                            composable("video_player/{username}/{videoId}") { backStackEntry ->
                                val username = backStackEntry.arguments?.getString("username") ?: ""
                                val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
                                
                                val videoPlayerViewModel: com.kickstra.presentation.video_player.VideoPlayerViewModel = hiltViewModel()
                                
                                // Track video player screen state
                                DisposableEffect(Unit) {
                                    isOnVideoPlayerScreen = true
                                    currentVideoPlayerViewModel = videoPlayerViewModel
                                    onDispose {
                                        isOnVideoPlayerScreen = false
                                        currentVideoPlayerViewModel = null
                                    }
                                }
                                
                                VideoPlayerScreen(
                                    videoId = videoId,
                                    username = username,
                                    onNavigateBack = { navController.popBackStack() },
                                    onFullscreenChange = { fullscreen ->
                                        isFullscreen = fullscreen
                                        requestedOrientation = if (fullscreen) {
                                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                        } else {
                                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                        }
                                        
                                        if (fullscreen) {
                                            enableImmersiveMode()
                                        } else {
                                            disableImmersiveMode()
                                        }
                                    },
                                    viewModel = videoPlayerViewModel
                                )
                            }
                            composable("clip_player?title={title}&url={url}&views={views}&duration={duration}&date={date}") { backStackEntry ->
                                val title = backStackEntry.arguments?.getString("title")
                                val url = backStackEntry.arguments?.getString("url")
                                val views = backStackEntry.arguments?.getString("views")?.toIntOrNull()?.takeIf { it >= 0 }
                                val duration = backStackEntry.arguments?.getString("duration")?.toIntOrNull()?.takeIf { it >= 0 }
                                val date = backStackEntry.arguments?.getString("date")?.ifBlank { null }
                                com.kickstra.presentation.clip.ClipPlayerScreen(
                                    title = title,
                                    url = url,
                                    views = views,
                                    durationSec = duration,
                                    date = date,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("player") {
                                // Hide mini player when entering full player
                                LaunchedEffect(Unit) {
                                    sharedDataViewModel.hideMiniPlayer()
                                }
                                
                                // Track player screen state
                                val playerViewModel: com.kickstra.presentation.player.PlayerViewModel = hiltViewModel()
                                DisposableEffect(Unit) {
                                    isOnPlayerScreen = true
                                    currentPlayerViewModel = playerViewModel
                                    onDispose {
                                        isOnPlayerScreen = false
                                        currentPlayerViewModel = null
                                    }
                                }
                                
                                // Show mini player again when leaving player screen
                                DisposableEffect(Unit) {
                                    onDispose {
                                        val selectedStream = sharedDataViewModel.getSelectedStream()
                                        if (selectedStream != null) {
                                            sharedDataViewModel.showMiniPlayer(selectedStream)
                                        }
                                    }
                                }
                                
                                val selectedStream = sharedDataViewModel.getSelectedStream()
                                if (selectedStream != null) {
                                    PlayerScreenWithStream(
                                        stream = selectedStream,
                                        viewModel = playerViewModel,
                                        onBackClick = { 
                                            Log.d("Navigation", "Player back clicked")
                                            // Show mini player with current stream
                                            if (selectedStream != null) {
                                                sharedDataViewModel.showMiniPlayer(selectedStream)
                                            }
                                            // Reset orientation when leaving player
                                            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                            // Disable immersive mode
                                            disableImmersiveMode()
                                            // Pop back to previous screen (home or following)
                                            val popResult = navController.popBackStack()
                                            Log.d("Navigation", "PopBackStack result: $popResult")
                                            // Don't clear stream data since mini player needs it
                                        },
                                        onChannelClick = { username ->
                                            navController.navigate("channel_profile/$username")
                                        },
                                        onFullscreenChange = { fullscreen ->
                                            isFullscreen = fullscreen
                                            requestedOrientation = if (fullscreen) {
                                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                            } else {
                                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                            }
                                            
                                            // Enable/disable immersive mode
                                            if (fullscreen) {
                                                enableImmersiveMode()
                                            } else {
                                                disableImmersiveMode()
                                            }
                                        }
                                    )
                                } else {
                                    // Fallback - navigate back to home if no stream selected
                                    LaunchedEffect(Unit) {
                                        navController.navigate("home") {
                                            popUpTo("home") {
                                                inclusive = false
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
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        if (intent?.getBooleanExtra("open_now_playing", false) == true) {
            handleNowPlayingNavigation(navController ?: return, sharedDataViewModel ?: return)
        }
    }

    private fun handleNowPlayingNavigation(navController: androidx.navigation.NavController, shared: SharedDataViewModel) {
        // First attempt: existing selected stream
        val selected = shared.getSelectedStream()
        if (selected != null) {
            if (navController.currentDestination?.route != "player") {
                navController.navigate("player") {
                    launchSingleTop = true
                    restoreState = true
                }
            }
            return
        }
        // Second: mini player content
        val mini = shared.getMiniPlayerContent()
        when (mini) {
            is MiniPlayerContent.LiveStreamContent -> {
                shared.setSelectedStream(mini.stream)
                if (navController.currentDestination?.route != "player") {
                    navController.navigate("player") {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                    }
                }
                return
            }
            is MiniPlayerContent.VideoContent -> {
                if (navController.currentDestination?.route?.startsWith("video_player") != true) {
                    navController.navigate("video_player/${mini.username}/${mini.video.id}") {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                return
            }
            else -> Unit
        }
        // Third: reconstruct from intent extras (for cold start without ViewModel state yet)
        intent?.let { i ->
            when (i.getStringExtra("np_type")) {
                "live" -> {
                    val stream = try {
                        com.kickstra.domain.model.LiveStream(
                            id = i.getStringExtra("live_id") ?: return,
                            title = i.getStringExtra("live_title") ?: "",
                            viewerCount = i.getIntExtra("live_viewers", 0), // EXPECT Int
                            thumbnail = i.getStringExtra("live_thumbnail")?.takeIf { it.isNotBlank() }?.let { src ->
                                com.kickstra.domain.model.StreamThumbnail(src)
                            },
                            startTime = i.getStringExtra("live_start") ?: "",
                            channel = com.kickstra.domain.model.StreamChannel(
                                id = run {
                                    val direct = i.getLongExtra("live_channel_id", Long.MIN_VALUE)
                                    if (direct != Long.MIN_VALUE) direct else i.getIntExtra("live_channel_id", 0).toLong()
                                },
                                slug = i.getStringExtra("live_channel_slug") ?: "",
                                profilePic = i.getStringExtra("live_channel_profile") ?: "",
                                username = i.getStringExtra("live_channel_username") ?: ""
                            ),
                            category = com.kickstra.domain.model.StreamCategory(
                                id = i.getLongExtra("live_category_id", -1L).let { if (it == -1L) i.getIntExtra("live_category_id", 0).toLong() else it },
                                name = i.getStringExtra("live_category_name") ?: "",
                                slug = i.getStringExtra("live_category_slug") ?: ""
                            ),
                            language = i.getStringExtra("live_language") ?: "",
                            isMature = i.getBooleanExtra("live_is_mature", false),
                            tags = (i.getStringExtra("live_tags") ?: "").split(',').filter { it.isNotBlank() }
                        )
                    } catch (_: Exception) { null }
                    if (stream != null) {
                        shared.setSelectedStream(stream)
                        if (navController.currentDestination?.route != "player") {
                            navController.navigate("player") {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                            }
                        }
                        return
                    }
                }
                "video" -> {
                    val username = i.getStringExtra("video_username") ?: return
                    val videoId = i.getStringExtra("video_id") ?: return
                    if (navController.currentDestination?.route?.startsWith("video_player") != true) {
                        navController.navigate("video_player/$username/$videoId") {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    return
                }
            }
        }
        // Fallback: go home
        navController.navigate("home")
    }

    private fun enableImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Use WindowInsetsController for API 30+
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // Use deprecated flags for older versions
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
    }

    private fun disableImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Use WindowInsetsController for API 30+
            window.insetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            // Use deprecated flags for older versions
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }
    }

    fun setVideoPlayingState(isPlaying: Boolean, isOnPlayer: Boolean) {
        // This method is called from PlayerScreen to track video state
        // Update notification state as well
        playbackNotificationManager.onPlaybackStateChanged(isPlaying)
    }

    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                // Show system dialog; it's okay to request in response to a user action like starting playback
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        
        // Enter Picture-in-Picture mode when user leaves the app while watching
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                    try {
                    println("DEBUG: onUserLeaveHint called")
                    
                    // Get current route for more reliable screen detection
                    val currentRoute = navController?.currentDestination?.route
                    println("DEBUG: Current route = $currentRoute")
                    println("DEBUG: isOnVideoPlayerScreen = $isOnVideoPlayerScreen")
                    println("DEBUG: isOnPlayerScreen = $isOnPlayerScreen") 
                    println("DEBUG: currentVideoPlayerViewModel = $currentVideoPlayerViewModel")
                    println("DEBUG: currentPlayerViewModel = $currentPlayerViewModel")
                    
                    val miniPlayerContent = sharedDataViewModel?.getMiniPlayerContent()
                    val isMiniPlayerVisible = sharedDataViewModel?.isMiniPlayerVisible?.value == true
                    val isPlayerActive = playerManager.isPlaying()
                    
                    println("DEBUG: isMiniPlayerVisible = $isMiniPlayerVisible")
                    println("DEBUG: miniPlayerContent = $miniPlayerContent")
                    println("DEBUG: isPlayerActive = $isPlayerActive")
                    
                    // Check if we're on video player screen (by route or flag)
                    val isOnVideoPlayerRoute = currentRoute?.startsWith("video_player") == true
                    if (isOnVideoPlayerScreen || isOnVideoPlayerRoute) {
                        println("DEBUG: On video player screen, preparing for PiP")
                        // First, tell the ViewModel to enter fullscreen, then PiP (if available)
                        currentVideoPlayerViewModel?.prepareForPipMode()
                        
                        // Give a small delay for fullscreen to take effect, then enter PiP
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            enterPipModeForVideo()
                        }, 200) // 200ms delay to let fullscreen take effect
                        return
                    }
                    
                    // Check if we're on live stream player screen (by route or flag)
                    val isOnPlayerRoute = currentRoute == "player"
                    if (isOnPlayerScreen || isOnPlayerRoute) {
                        println("DEBUG: On live stream player screen, preparing for PiP")
                        currentPlayerViewModel?.prepareForPipMode()
                        
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            enterPipModeForLiveStream()
                        }, 200) // 200ms delay to let fullscreen take effect
                        return
                    }
                    
                    if (isMiniPlayerVisible && miniPlayerContent != null && isPlayerActive) {
                        println("DEBUG: Mini player visible and player is active, preparing for PiP")
                        
                        // Create a temporary fullscreen video view for PiP
                        // This is a workaround since PiP requires fullscreen video
                        
                        when (miniPlayerContent) {
                            is MiniPlayerContent.LiveStreamContent -> {
                                // For live streams, navigate to player first
                                sharedDataViewModel?.setSelectedStream(miniPlayerContent.stream)
                                sharedDataViewModel?.hideMiniPlayer()
                                navController?.navigate("player")
                                
                                // Wait a bit for navigation and then enter PiP
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    // Enter PiP from the now-active player screen
                                    try {
                                        val params = PictureInPictureParams.Builder()
                                            .setAspectRatio(Rational(16, 9))
                                            .build()
                                        
                                        val pipResult = enterPictureInPictureMode(params)
                                        if (pipResult) {
                                            println("DEBUG: Successfully entered PiP mode for live stream from mini player")
                                        } else {
                                            println("DEBUG: Failed to enter PiP mode for live stream")
                                        }
                                    } catch (e: Exception) {
                                        println("DEBUG: Exception entering PiP mode: ${e.message}")
                                    }
                                }, 100) // Short delay for navigation
                            }
                            is MiniPlayerContent.VideoContent -> {
                                // For videos, navigate to video player first  
                                sharedDataViewModel?.hideMiniPlayer()
                                val nc = navController
                                if (nc != null) {
                                    nc.navigate("video_player/${miniPlayerContent.username}/${miniPlayerContent.video.id}") {
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                                
                                // Wait a bit for navigation and then enter PiP
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    // Enter PiP from the now-active video player screen
                                    try {
                                        val params = PictureInPictureParams.Builder()
                                            .setAspectRatio(Rational(16, 9))
                                            .build()
                                        
                                        val pipResult = enterPictureInPictureMode(params)
                                        if (pipResult) {
                                            println("DEBUG: Successfully entered PiP mode for video from mini player")
                                        } else {
                                            println("DEBUG: Failed to enter PiP mode for video")
                                        }
                                    } catch (e: Exception) {
                                        println("DEBUG: Exception entering PiP mode: ${e.message}")
                                    }
                                }, 100) // Short delay for navigation
                            }
                            is MiniPlayerContent.ClipContent -> {
                                // For clips, navigate to clip player first
                                sharedDataViewModel?.hideMiniPlayer()
                                val encodedTitle = android.net.Uri.encode(miniPlayerContent.title)
                                val encodedUrl = android.net.Uri.encode(miniPlayerContent.url)
                                val v = miniPlayerContent.views ?: 0
                                val d = miniPlayerContent.durationSec ?: 0
                                val encodedDate = android.net.Uri.encode(miniPlayerContent.date ?: "")
                                navController?.navigate("clip_player?title=$encodedTitle&url=$encodedUrl&views=$v&duration=$d&date=$encodedDate")
                                
                                // Wait a bit then enter PiP
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    try {
                                        val params = PictureInPictureParams.Builder()
                                            .setAspectRatio(Rational(16, 9))
                                            .build()
                                        val pipResult = enterPictureInPictureMode(params)
                                        if (pipResult) {
                                            println("DEBUG: Successfully entered PiP mode for clip from mini player")
                                        } else {
                                            println("DEBUG: Failed to enter PiP mode for clip")
                                        }
                                    } catch (e: Exception) {
                                        println("DEBUG: Exception entering PiP mode for clip: ${e.message}")
                                    }
                                }, 100)
                            }
                        }
                        return
                    }
                    
                    println("DEBUG: No active video/stream found for PiP")
                    
                } catch (e: Exception) {
                    println("DEBUG: Failed to auto-enter PiP mode: ${e.message}")
                    e.printStackTrace()
                }
            } else {
                println("DEBUG: PiP not supported by device")
            }
        } else {
            println("DEBUG: Android version too old for PiP (requires API 26+)")
        }
    }
    
    private fun enterPipModeForVideo() {
        try {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            
            val pipResult = enterPictureInPictureMode(params)
            if (pipResult) {
                currentVideoPlayerViewModel?.onPipModeEntered()
                println("DEBUG: Successfully entered PiP mode for video")
            } else {
                currentVideoPlayerViewModel?.clearPipRequest()
                println("DEBUG: Failed to enter PiP mode for video")
            }
        } catch (e: Exception) {
            currentVideoPlayerViewModel?.clearPipRequest()
            println("DEBUG: Exception entering PiP mode for video: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun enterPipModeForLiveStream() {
        try {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            
            val pipResult = enterPictureInPictureMode(params)
            if (pipResult) {
                currentPlayerViewModel?.onPipModeEnter()
                println("DEBUG: Successfully entered PiP mode for live stream")
            } else {
                println("DEBUG: Failed to enter PiP mode for live stream")
            }
        } catch (e: Exception) {
            println("DEBUG: Exception entering PiP mode for live stream: ${e.message}")
            e.printStackTrace()
        }
    }

    @TargetApi(26)
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        }
        
        if (isInPictureInPictureMode) {
            // Entered PiP mode
            println("DEBUG: Entered Picture-in-Picture mode")
            currentVideoPlayerViewModel?.onPipModeEntered()
            currentPlayerViewModel?.onPipModeEnter()
        } else {
            // Exited PiP mode
            println("DEBUG: Exited Picture-in-Picture mode")
            currentVideoPlayerViewModel?.onPipModeExited()
            currentPlayerViewModel?.onPipModeExit()
        }
        
        // Notify PipModeManager
        pipModeManager.setPipMode(isInPictureInPictureMode)
    }
}
