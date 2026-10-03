package com.kicklite.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kicklite.R
import com.kicklite.domain.model.LiveStream
import com.kicklite.presentation.home.components.StreamItem
import com.kicklite.presentation.home.components.LanguageFilter
import com.kicklite.presentation.shared.SharedDataViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    onStreamClick: (LiveStream) -> Unit,
    onSettingsClick: () -> Unit,
    onChannelClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    sharedDataViewModel: SharedDataViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    // Pull refresh state
    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isRefreshing,
        onRefresh = { viewModel.refresh() }
    )
    
    // Use preserved list state or create new one
    val listState = remember {
        sharedDataViewModel.getSavedHomeListState() ?: LazyListState()
    }
    
    // Save list state when it changes
    LaunchedEffect(listState) {
        sharedDataViewModel.saveHomeListState(listState)
    }

    // Infinite scroll logic - separate LaunchedEffect
    LaunchedEffect(state.streams.size, state.isLoadingMore, state.nextCursor) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                if (visibleItems.isNotEmpty() && 
                    visibleItems.last().index >= state.streams.size - 3 &&
                    !state.isLoadingMore && 
                    state.nextCursor != null) {
                    viewModel.loadMoreStreams()
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.home_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(onClick = { viewModel.toggleLanguageFilter() }) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = stringResource(R.string.language_filter),
                            tint = if (state.selectedLanguage != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.open_settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pullRefresh(pullRefreshState)
        ) {
            when {
                state.isLoading && state.streams.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.error != null && state.streams.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.error ?: stringResource(R.string.error_loading_streams),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.retry() }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                state.streams.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.no_streams),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (state.selectedLanguage != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.clearLanguageFilter() }) {
                                Text(stringResource(R.string.clear_filter))
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Language filter
                        if (state.showLanguageFilter) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    LanguageFilter(
                                        selectedLanguage = state.selectedLanguage,
                                        onLanguageSelected = { language -> 
                                            viewModel.selectLanguage(language)
                                        },
                                        onClearFilter = { viewModel.clearLanguageFilter() }
                                    )
                                }
                            }
                        }

                        // Selected language indicator
                        if (state.selectedLanguage != null) {
                            item {
                                val languageFlags = mapOf(
                                    "tr" to "🇹🇷",
                                    "en" to "🇺🇸",
                                    "es" to "🇪🇸",
                                    "de" to "🇩🇪",
                                    "fr" to "🇫🇷",
                                    "pt" to "🇵🇹",
                                    "ru" to "🇷🇺",
                                    "ja" to "🇯🇵",
                                    "ko" to "🇰🇷"
                                )

                                val selectedLanguageLabel = state.selectedLanguage?.let { code ->
                                    val languageName = when (code) {
                                        "tr" -> stringResource(R.string.language_name_turkish)
                                        "en" -> stringResource(R.string.language_name_english)
                                        "es" -> stringResource(R.string.language_name_spanish)
                                        "de" -> stringResource(R.string.language_name_german)
                                        "fr" -> stringResource(R.string.language_name_french)
                                        "pt" -> stringResource(R.string.language_name_portuguese)
                                        "ru" -> stringResource(R.string.language_name_russian)
                                        "ja" -> stringResource(R.string.language_name_japanese)
                                        "ko" -> stringResource(R.string.language_name_korean)
                                        else -> code
                                    }
                                    val flag = languageFlags[code]
                                    if (flag != null) "$flag $languageName" else languageName
                                } ?: state.selectedLanguage
                                
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(
                                                R.string.filtering_by_language,
                                                selectedLanguageLabel ?: state.selectedLanguage.orEmpty()
                                            ),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        TextButton(
                                            onClick = { viewModel.clearLanguageFilter() }
                                        ) {
                                            Text(stringResource(R.string.clear))
                                        }
                                    }
                                }
                            }
                        }

                        items(state.streams) { stream ->
                            StreamItem(
                        stream = stream,
                        onClick = { onStreamClick(stream) },
                        onChannelClick = { onChannelClick(stream.channel.username) },
                        modifier = Modifier.fillMaxWidth()
                    )
                        }
                        
                        // Loading more indicator
                        if (state.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
            
            // Pull refresh indicator
            PullRefreshIndicator(
                refreshing = state.isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}
