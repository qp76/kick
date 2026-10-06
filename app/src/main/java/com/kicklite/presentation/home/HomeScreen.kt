package com.kicklite.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.kicklite.domain.common.Resource
import com.kicklite.domain.model.LiveStream
import com.kicklite.domain.model.StreamCategory
import com.kicklite.presentation.home.components.LanguageFilter
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HomeScreen(
    onStreamClick: (LiveStream) -> Unit,
    onSettingsClick: () -> Unit,
    onChannelClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    sharedDataViewModel: com.kicklite.presentation.shared.SharedDataViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val categories = remember(state.streams) {
        state.streams.groupBy { it.category }
            .mapValues { it.value.maxOfOrNull { stream -> stream.viewerCount } ?: 0 }
            .toList()
            .sortedByDescending { it.second }
            .take(8)
            .map { it.first }
    }

    val featured = remember(state.streams) {
        state.streams.distinctBy { it.channel.slug }.sortedByDescending { it.viewerCount }.take(8)
    }

    val sections = remember(state.streams, selectedCategory) {
        val base = if (selectedCategory == null) state.streams else state.streams.filter { it.category.name == selectedCategory }
        base.groupBy { it.category.name.ifBlank { "Live" } }
            .toList()
            .sortedByDescending { pair -> pair.second.maxOfOrNull { it.viewerCount } ?: 0 }
            .take(8)
    }

    LaunchedEffect(state.streams.size, state.nextCursor, state.isLoadingMore) {
        if (state.streams.isNotEmpty() && listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 >= listState.layoutInfo.totalItemsCount - 3) {
            viewModel.loadMoreStreams()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("KICK", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black), color = Color(0xFF53FF00))
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { viewModel.toggleLanguageFilter() }) {
                    Icon(Icons.Default.Language, contentDescription = "Language", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (state.showLanguageFilter) {
                item {
                    LanguageFilter(
                        selectedLanguage = state.selectedLanguage,
                        onLanguageSelected = viewModel::selectLanguage,
                        onClearFilter = viewModel::clearLanguageFilter
                    )
                }
            }

            if (state.isLoading && state.streams.isEmpty()) {
                item { Box(Modifier.fillMaxWidth().height(420.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            } else if (state.error != null && state.streams.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.error ?: "Unable to load KICK")
                        Spacer(Modifier.height(12.dp))
                        Text("Pull down to retry", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                item {
                    SectionTitle("Featured Creators")
                    Spacer(Modifier.height(10.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(featured, key = { "featured_${it.id}" }) { stream ->
                            FeaturedCard(stream, onClick = { onStreamClick(stream) }, onChannelClick = onChannelClick)
                        }
                    }
                }

                if (categories.isNotEmpty()) {
                    item {
                        SectionTitle("Top Live Categories")
                        Spacer(Modifier.height(10.dp))
                        LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(categories, key = { "cat_${it.id}" }) { category ->
                                CategoryCard(category, selected = selectedCategory == category.name) {
                                    selectedCategory = if (selectedCategory == category.name) null else category.name
                                }
                            }
                        }
                    }
                }

                sections.forEach { (name, streams) ->
                    item(key = "section_$name") {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(6.dp))
                            Text("›", style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                    item(key = "row_$name") {
                        LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(streams.distinctBy { it.channel.slug }, key = { "stream_${it.id}" }) { stream ->
                                StreamCard(stream, onClick = { onStreamClick(stream) }, onChannelClick = onChannelClick)
                            }
                        }
                    }
                }

                if (state.isLoadingMore) {
                    item { Box(Modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(26.dp)) } }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, Modifier.padding(horizontal = 18.dp), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
}

@Composable
private fun FeaturedCard(stream: LiveStream, onClick: () -> Unit, onChannelClick: (String) -> Unit) {
    Card(Modifier.width(330.dp).clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Box {
            AsyncImage(model = stream.thumbnail?.src, contentDescription = stream.title, modifier = Modifier.fillMaxWidth().aspectRatio(1.65f), contentScale = ContentScale.Crop)
            ViewerPill(stream.viewerCount, Modifier.align(Alignment.TopStart).padding(12.dp))
            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Color.Black.copy(alpha = .72f)).padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = stream.channel.profilePic, contentDescription = null, modifier = Modifier.size(38.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stream.channel.username, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(stream.category.name, color = Color.LightGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(stream.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White)
            }
        }
    }
}

@Composable
private fun StreamCard(stream: LiveStream, onClick: () -> Unit, onChannelClick: (String) -> Unit) {
    Column(Modifier.width(240.dp).clickable(onClick = onClick)) {
        Box {
            AsyncImage(model = stream.thumbnail?.src, contentDescription = stream.title, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
            ViewerPill(stream.viewerCount, Modifier.align(Alignment.TopStart).padding(9.dp))
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = stream.channel.profilePic, contentDescription = null, modifier = Modifier.size(38.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(stream.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(stream.channel.username, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun CategoryCard(category: StreamCategory, selected: Boolean, onClick: () -> Unit) {
    Card(Modifier.width(155.dp).height(180.dp).clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(28.dp), tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(category.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ViewerPill(count: Int, modifier: Modifier = Modifier) {
    Row(modifier.clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = .72f)).padding(horizontal = 9.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(Color(0xFF53FF00)))
        Spacer(Modifier.width(6.dp))
        Text(formatViewers(count), color = Color.White, fontWeight = FontWeight.Bold)
    }
}

private fun formatViewers(value: Int): String = when {
    value >= 1_000_000 -> "%.1fM".format(value / 1_000_000f)
    value >= 1_000 -> "%.1fK".format(value / 1_000f)
    else -> value.toString()
}
