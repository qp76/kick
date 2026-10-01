package com.kickstra.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kickstra.R
import com.kickstra.domain.model.LiveStream
import com.kickstra.presentation.home.components.StreamItem
import com.kickstra.presentation.shared.SharedDataViewModel
import com.kickstra.ui.theme.KickGreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    onStreamClick: (LiveStream) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    sharedDataViewModel: SharedDataViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val pullRefreshState = rememberPullRefreshState(state.isRefreshing) { viewModel.refresh() }
    val listState = remember { sharedDataViewModel.getSavedHomeListState() ?: LazyListState() }

    LaunchedEffect(listState) { sharedDataViewModel.saveHomeListState(listState) }
    LaunchedEffect(state.streams.size, state.isLoadingMore, state.nextCursor) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }.collect { visibleItems ->
            if (visibleItems.isNotEmpty() && visibleItems.last().index >= state.streams.size - 3 &&
                !state.isLoadingMore && state.nextCursor != null) viewModel.loadMoreStreams()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("KICK", fontSize = 25.sp, fontWeight = FontWeight.Black, color = KickGreen)
                    Spacer(Modifier.width(6.dp))
                    Text("STRA", fontSize = 25.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { viewModel.toggleLanguageFilter() }) {
                        Icon(Icons.Default.Language, "Language", tint = if (state.selectedLanguage != null) KickGreen else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f))
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues).pullRefresh(pullRefreshState)
        ) {
            when {
                state.isLoading && state.streams.isEmpty() -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = KickGreen)
                state.error != null && state.streams.isEmpty() -> {
                    Column(Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.error ?: stringResource(R.string.error_loading_streams), style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(14.dp))
                        Button(onClick = viewModel::retry, colors = ButtonDefaults.buttonColors(containerColor = KickGreen, contentColor = MaterialTheme.colorScheme.onBackground)) { Text(stringResource(R.string.retry)) }
                    }
                }
                state.streams.isEmpty() -> Text(stringResource(R.string.no_streams), Modifier.align(Alignment.Center), style = MaterialTheme.typography.bodyLarge)
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                                Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(KickGreen))
                                Spacer(Modifier.width(8.dp))
                                Text("LIVE NOW", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Spacer(Modifier.weight(1f))
                                if (state.selectedLanguage != null) {
                                    TextButton(onClick = viewModel::clearLanguageFilter, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Clear", color = KickGreen) }
                                }
                            }
                        }
                        if (state.showLanguageFilter) {
                            item {
                                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                    com.kickstra.presentation.home.components.LanguageFilter(
                                        selectedLanguage = state.selectedLanguage,
                                        onLanguageSelected = viewModel::selectLanguage,
                                        onClearFilter = viewModel::clearLanguageFilter
                                    )
                                }
                            }
                        }
                        items(state.streams, key = { it.channel.username + ":" + it.id }) { stream ->
                            StreamItem(stream = stream, onClick = { onStreamClick(stream) }, modifier = Modifier.fillMaxWidth())
                        }
                        if (state.isLoadingMore) item { Box(Modifier.fillMaxWidth().padding(12.dp), Alignment.Center) { CircularProgressIndicator(Modifier.size(22.dp), color = KickGreen, strokeWidth = 2.dp) } }
                    }
                }
            }
            PullRefreshIndicator(state.isRefreshing, pullRefreshState, Modifier.align(Alignment.TopCenter), color = KickGreen)
        }
    }
}
