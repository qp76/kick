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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.kicklite.domain.model.LiveStream
import com.kicklite.domain.model.StreamCategory
import com.kicklite.presentation.home.components.LanguageFilter

private val KickAccent = Color(0xFF53FC18)
private val PageText = Color(0xFFF4F7F4)
private val MutedText = Color(0xFFA0AAA3)

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

    val featured = remember(state.streams) {
        state.streams.distinctBy { it.channel.slug }
            .sortedByDescending { it.viewerCount }
            .take(8)
    }
    val categories = remember(state.streams) {
        state.streams.groupBy { it.category }
            .mapValues { entry -> entry.value.maxOfOrNull { it.viewerCount } ?: 0 }
            .toList()
            .sortedByDescending { it.second }
            .take(10)
            .map { it.first }
    }
    val visibleStreams = remember(state.streams, selectedCategory) {
        val filtered = if (selectedCategory == null) state.streams
        else state.streams.filter { it.category.name == selectedCategory }
        filtered.distinctBy { it.channel.slug }
            .sortedByDescending { it.viewerCount }
    }
    val sections = remember(visibleStreams, selectedCategory) {
        if (selectedCategory != null) emptyList()
        else visibleStreams.groupBy { it.category.name.ifBlank { "Live" } }
            .toList()
            .sortedByDescending { entry -> entry.second.maxOfOrNull { it.viewerCount } ?: 0 }
            .take(6)
    }

    LaunchedEffect(state.streams.size, state.nextCursor, state.isLoadingMore) {
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        if (state.streams.isNotEmpty() &&
            !state.isLoadingMore &&
            listState.layoutInfo.totalItemsCount > 0 &&
            lastVisible >= listState.layoutInfo.totalItemsCount - 3
        ) {
            viewModel.loadMoreStreams()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D0A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 14.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(KickAccent)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "KICK",
                    color = PageText,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    "DISCOVER",
                    color = MutedText,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { viewModel.toggleLanguageFilter() }) {
                Icon(Icons.Default.Language, contentDescription = "Language", tint = PageText)
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = PageText)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Column(Modifier.padding(horizontal = 22.dp)) {
                    Text(
                        "Find your next",
                        color = PageText,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "live moment.",
                        color = KickAccent,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        "Explore creators streaming right now",
                        color = MutedText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (state.showLanguageFilter) {
                item {
                    LanguageFilter(
                        selectedLanguage = state.selectedLanguage,
                        onLanguageSelected = viewModel::selectLanguage,
                        onClearFilter = viewModel::clearLanguageFilter
                    )
                }
            }

            if (categories.isNotEmpty()) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { selectedCategory = null },
                                label = { Text("For you") }
                            )
                        }
                        items(categories, key = { "category_${it.id}" }) { category ->
                            FilterChip(
                                selected = selectedCategory == category.name,
                                onClick = {
                                    selectedCategory = if (selectedCategory == category.name) null else category.name
                                },
                                label = { Text(category.name, maxLines = 1) }
                            )
                        }
                    }
                }
            }

            if (state.isLoading && state.streams.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().height(280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = KickAccent)
                    }
                }
            } else if (state.error != null && state.streams.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Couldn't load live channels", color = PageText, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(state.error ?: "Check your connection and try again.", color = MutedText)
                    }
                }
            } else {
                if (featured.isNotEmpty()) {
                    item {
                        Column {
                            SectionHeading(
                                title = "Featured live",
                                subtitle = "The biggest streams right now"
                            )
                            Spacer(Modifier.height(12.dp))
                            HeroStreamCard(featured.first(), onClick = { onStreamClick(featured.first()) })
                        }
                    }
                }

                if (visibleStreams.isNotEmpty()) {
                    item {
                        Column {
                            SectionHeading(
                                title = if (selectedCategory == null) "Trending now" else selectedCategory!!,
                                subtitle = "Popular with the community"
                            )
                            Spacer(Modifier.height(12.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 18.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(visibleStreams.take(12), key = { "trending_${it.id}" }) { stream ->
                                    LiveStreamCard(stream, onClick = { onStreamClick(stream) })
                                }
                            }
                        }
                    }
                }

                sections.forEach { (name, streams) ->
                    item(key = "section_$name") {
                        Column {
                            SectionHeading(title = name, subtitle = "${streams.size} live channels")
                            Spacer(Modifier.height(12.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 18.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(streams.distinctBy { it.channel.slug }.take(10), key = { "row_${it.id}" }) { stream ->
                                    LiveStreamCard(stream, onClick = { onStreamClick(stream) })
                                }
                            }
                        }
                    }
                }

                if (state.isLoadingMore) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(24.dp), color = KickAccent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text(title, color = PageText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Text(subtitle, color = MutedText, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun HeroStreamCard(stream: LiveStream, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF151B17))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = stream.thumbnail?.src,
            contentDescription = stream.title,
            modifier = Modifier.fillMaxWidth().aspectRatio(1.48f),
            contentScale = ContentScale.Crop
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.42f to Color.Black.copy(alpha = 0.08f),
                        1f to Color.Black.copy(alpha = 0.88f)
                    )
                )
        )
        Row(
            Modifier.align(Alignment.TopStart).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LivePill()
            Spacer(Modifier.width(8.dp))
            ViewerPill(stream.viewerCount)
        }
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(18.dp)
        ) {
            Text(
                stream.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = stream.channel.profilePic,
                    contentDescription = null,
                    modifier = Modifier.size(38.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(stream.channel.username, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(stream.category.name, color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.bodySmall)
                }
                Text("WATCH", color = KickAccent, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun LiveStreamCard(stream: LiveStream, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(248.dp).clickable(onClick = onClick)
    ) {
        Box {
            AsyncImage(
                model = stream.thumbnail?.src,
                contentDescription = stream.title,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(RoundedCornerShape(19.dp)),
                contentScale = ContentScale.Crop
            )
            Row(Modifier.align(Alignment.TopStart).padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
                LivePill()
                Spacer(Modifier.width(6.dp))
                ViewerPill(stream.viewerCount)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = stream.channel.profilePic,
                contentDescription = null,
                modifier = Modifier.size(38.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(stream.title, color = PageText, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(stream.channel.username, color = MutedText, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun LivePill() {
    Row(
        Modifier.clip(RoundedCornerShape(7.dp)).background(Color(0xFFE83B4B)).padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(Color.White))
        Spacer(Modifier.width(5.dp))
        Text("LIVE", color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ViewerPill(count: Int) {
    Text(
        text = "${formatViewers(count)} watching",
        modifier = Modifier.clip(RoundedCornerShape(7.dp)).background(Color.Black.copy(alpha = 0.66f)).padding(horizontal = 8.dp, vertical = 5.dp),
        color = Color.White,
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.labelSmall
    )
}

private fun formatViewers(value: Int): String = when {
    value >= 1_000_000 -> "%.1fM".format(value / 1_000_000f)
    value >= 1_000 -> "%.1fK".format(value / 1_000f)
    else -> value.toString()
}
