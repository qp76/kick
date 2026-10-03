package com.kicklite.presentation.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import android.os.Build
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.platform.LocalContext
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.kicklite.data.local.ChatPreferences
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kicklite.R
import com.kicklite.domain.model.ChatMessage
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private val EMOTE_REGEX = Regex("""\[emote:(\d+):([^\]]*)]""")
private val KNOWN_BOTS = setOf(
    "botrix", "kickbot", "kicklet", "nightbot", "moobot", "streamelements", "streamlabs", "wizebot"
)
private val MENTION_REGEX = Regex("@[A-Za-z0-9_]+")
private val EMOTE_TOKEN = Regex("""\[emote:\d+:([^\]]*)]""")

/** Quoted message for replies: Pair(username, text) or null. */
private fun parseReply(metadata: String?): Pair<String, String>? {
    if (metadata.isNullOrBlank() || !metadata.contains("original_message")) return null
    return runCatching {
        val o = org.json.JSONObject(metadata)
        val user = o.optJSONObject("original_sender")?.optString("username").orEmpty()
        val text = o.optJSONObject("original_message")?.optString("content").orEmpty()
        if (user.isEmpty()) null else user to text.replace(EMOTE_TOKEN, "$1")
    }.getOrNull()
}

private fun emoteUrl(id: String) = "https://files.kick.com/emotes/$id/fullsize"

@Composable
fun ChatSection(
    chatMessages: List<ChatMessage>,
    channelName: String,
    pinnedMessage: ChatMessage? = null,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    settingsViewModel: ChatSettingsViewModel = hiltViewModel()
) {
    val prefs = settingsViewModel.prefs
    val textSize by prefs.textSize.collectAsStateWithLifecycle()
    val compact by prefs.compact.collectAsStateWithLifecycle()
    val hideCommands by prefs.hideCommands.collectAsStateWithLifecycle()
    val showBadges by prefs.showBadges.collectAsStateWithLifecycle()
    val emoteScale by prefs.emoteScale.collectAsStateWithLifecycle()
    val animateEmotes by prefs.animateEmotes.collectAsStateWithLifecycle()
    val performanceMode by prefs.performanceMode.collectAsStateWithLifecycle()
    val showTimestamps by prefs.showTimestamps.collectAsStateWithLifecycle()
    val animate = animateEmotes && !performanceMode
    val hideBots by prefs.hideBots.collectAsStateWithLifecycle()
    val highlightWords by prefs.highlightWords.collectAsStateWithLifecycle()
    val mutedUsers by prefs.mutedUsers.collectAsStateWithLifecycle()
    val highlightList = remember(highlightWords) {
        highlightWords.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
    }
    val mutedSet = remember(mutedUsers) {
        mutedUsers.split(",").map { it.trim().lowercase().removePrefix("@") }.filter { it.isNotEmpty() }.toSet()
    }
    var dismissedPinId by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    // Newest message first (the list is drawn with reverseLayout, so index 0 is the bottom)
    val visible = remember(chatMessages, hideCommands, hideBots, mutedSet) {
        chatMessages.asReversed().distinctBy { it.id }.filter {
            (!hideCommands || !it.content.startsWith("!")) &&
                (!hideBots || it.sender.username.lowercase() !in KNOWN_BOTS) &&
                it.sender.username.lowercase() !in mutedSet
        }
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // true = stick to the newest message; false = the user scrolled up and we must NOT move the list
    var followLive by remember { mutableStateOf(true) }
    var pending by remember { mutableStateOf(0) }
    var lastSeenId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(listState) {
        snapshotFlow {
            Pair(
                listState.isScrollInProgress,
                listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 120
            )
        }.distinctUntilChanged().collect { (scrolling, atBottom) ->
            if (scrolling && !atBottom) followLive = false
            if (!scrolling && atBottom) {
                followLive = true
                pending = 0
            }
        }
    }

    val newestId = visible.firstOrNull()?.id
    LaunchedEffect(newestId) {
        if (newestId == null) return@LaunchedEffect
        if (followLive) {
            listState.scrollToItem(0)
            pending = 0
        } else {
            val idx = visible.indexOfFirst { it.id == lastSeenId }
            pending += if (idx > 0) idx else 1
        }
        lastSeenId = newestId
    }

    if (showSettings) {
        ChatSettingsDialog(prefs = prefs, onDismiss = { showSettings = false })
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(start = 14.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.chat_channel_title, channelName),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showSettings = true }) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Chat settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (onClose != null) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (pinnedMessage != null && pinnedMessage.id != dismissedPinId) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("\uD83D\uDCCC", fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        ChatMessageItem(
                            message = pinnedMessage,
                            textSize = textSize,
                            showBadges = showBadges,
                            emoteScale = emoteScale,
                            animate = animate
                        )
                    }
                    IconButton(onClick = { dismissedPinId = pinnedMessage.id }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Dismiss pinned message",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (visible.isEmpty()) {
                    Text(
                        text = stringResource(R.string.chat_empty_state),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        reverseLayout = true,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(if (compact) 1.dp else 6.dp)
                    ) {
                        items(visible, key = { it.id }) { message ->
                            ChatMessageItem(
                                message = message,
                                textSize = textSize,
                                showBadges = showBadges,
                                emoteScale = emoteScale,
                                animate = animate,
                                showTimestamps = showTimestamps,
                                highlight = highlightList.isNotEmpty() &&
                                    message.content.lowercase().let { c -> highlightList.any { c.contains(it) } },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Jump back to the live bottom
                if (!followLive && visible.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .clickable {
                                scope.launch {
                                    listState.scrollToItem(0)
                                    followLive = true
                                    pending = 0
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (pending > 0) "$pending new messages" else "Back to live chat",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatSettingsDialog(prefs: ChatPreferences, onDismiss: () -> Unit) {
    val textSize by prefs.textSize.collectAsStateWithLifecycle()
    val compact by prefs.compact.collectAsStateWithLifecycle()
    val hideCommands by prefs.hideCommands.collectAsStateWithLifecycle()
    val showBadges by prefs.showBadges.collectAsStateWithLifecycle()
    val emoteScale by prefs.emoteScale.collectAsStateWithLifecycle()
    val animateEmotes by prefs.animateEmotes.collectAsStateWithLifecycle()
    val performanceMode by prefs.performanceMode.collectAsStateWithLifecycle()
    val maxMessages by prefs.maxMessages.collectAsStateWithLifecycle()
    val showTimestamps by prefs.showTimestamps.collectAsStateWithLifecycle()
    val hideBots by prefs.hideBots.collectAsStateWithLifecycle()
    val sideWidth by prefs.sideWidth.collectAsStateWithLifecycle()
    val chatLeft by prefs.chatLeft.collectAsStateWithLifecycle()
    val highlightWords by prefs.highlightWords.collectAsStateWithLifecycle()
    val mutedUsers by prefs.mutedUsers.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chat settings") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Text size: ${textSize.toInt()}", style = MaterialTheme.typography.labelLarge)
                Slider(value = textSize, onValueChange = { prefs.setTextSize(it.toInt().toFloat()) }, valueRange = 11f..24f)
                SettingSwitch("Compact messages", compact, prefs::setCompact)
                SettingSwitch("Show badges", showBadges, prefs::setShowBadges)
                Text("Emote size: ${"%.1f".format(emoteScale)}x", style = MaterialTheme.typography.labelLarge)
                Slider(value = emoteScale, onValueChange = { prefs.setEmoteScale(Math.round(it * 10f) / 10f) }, valueRange = 1.0f..3.2f)
                SettingSwitch("Animated emotes", animateEmotes, prefs::setAnimateEmotes)
                SettingSwitch("Show timestamps", showTimestamps, prefs::setShowTimestamps)
                SettingSwitch("Hide ! commands", hideCommands, prefs::setHideCommands)
                SettingSwitch("Hide bot messages", hideBots, prefs::setHideBots)
                Spacer(Modifier.height(8.dp))
                Text("Performance", style = MaterialTheme.typography.titleSmall)
                SettingSwitch("Performance mode (no animations, lighter chat)", performanceMode, prefs::setPerformanceMode)
                Text("Messages kept in memory: ${maxMessages.toInt()}", style = MaterialTheme.typography.labelLarge)
                Slider(value = maxMessages, onValueChange = { prefs.setMaxMessages((it / 50f).toInt() * 50f) }, valueRange = 50f..500f)
                Spacer(Modifier.height(8.dp))
                Text("Fullscreen chat width: ${sideWidth.toInt()}", style = MaterialTheme.typography.labelLarge)
                Slider(value = sideWidth, onValueChange = { prefs.setSideWidth(it.toInt().toFloat()) }, valueRange = 220f..440f)
                SettingSwitch("Fullscreen chat on the left", chatLeft, prefs::setChatLeft)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = highlightWords,
                    onValueChange = prefs::setHighlightWords,
                    label = { Text("Highlight words (comma separated)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = mutedUsers,
                    onValueChange = prefs::setMutedUsers,
                    label = { Text("Muted users (comma separated)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun formatTime(createdAt: String): String = try {
    val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
    parser.timeZone = java.util.TimeZone.getTimeZone("UTC")
    val date = parser.parse(createdAt.take(19))
    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(date!!)
} catch (e: Exception) {
    ""
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    modifier: Modifier = Modifier,
    textSize: Float = 15f,
    showBadges: Boolean = true,
    emoteScale: Float = 1.7f,
    animate: Boolean = true,
    showTimestamps: Boolean = false,
    highlight: Boolean = false
) {
    val primary = MaterialTheme.colorScheme.primary
    val nameColor = remember(message.sender.identity.color, primary) {
        runCatching { Color(android.graphics.Color.parseColor(message.sender.identity.color)) }
            .getOrDefault(primary)
    }

    val (text, inline) = remember(message.id, textSize, showBadges, emoteScale, animate, showTimestamps, nameColor, primary) {
        buildChatText(
            message, nameColor, primary, showBadges,
            (textSize * emoteScale).sp, (textSize * 1.3f).sp, animate, showTimestamps
        )
    }
    val reply = remember(message.id) { parseReply(message.metadata) }
    val itemModifier = if (highlight) {
        modifier.background(primary.copy(alpha = 0.18f), RoundedCornerShape(6.dp)).padding(horizontal = 4.dp)
    } else modifier

    if (reply != null) {
        Column(modifier = itemModifier) {
            Text(
                text = "\u21A9 ${reply.first}: ${reply.second}",
                fontSize = (textSize * 0.8f).sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
            )
            Text(
                text = text,
                inlineContent = inline,
                fontSize = textSize.sp,
                lineHeight = (textSize * 1.45f).sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
        return
    }

    Text(
        text = text,
        inlineContent = inline,
        fontSize = textSize.sp,
        lineHeight = (textSize * 1.45f).sp,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = itemModifier.padding(vertical = 2.dp)
    )
}

/** Turns "[emote:123:Name]" tokens into inline images; everything else (unicode emoji included) stays text. */
private fun buildChatText(
    message: ChatMessage,
    nameColor: Color,
    mentionColor: Color,
    showBadges: Boolean,
    emoteSize: TextUnit,
    badgeSize: TextUnit,
    animate: Boolean,
    showTimestamps: Boolean
): Pair<AnnotatedString, Map<String, InlineTextContent>> {
    val inline = HashMap<String, InlineTextContent>()
    val content = message.content
    val text = buildAnnotatedString {
        if (showTimestamps) {
            withStyle(SpanStyle(color = Color(0xFF8A968E))) { append(formatTime(message.createdAt)); append(" ") }
        }
        if (showBadges) {
            for (b in message.sender.identity.badges) {
                if (!isKnownBadge(b.type)) continue
                val key = "b_${b.type}"
                appendInlineContent(key, "[badge]")
                append(" ")
                if (!inline.containsKey(key)) {
                    inline[key] = InlineTextContent(
                        Placeholder(badgeSize, badgeSize, PlaceholderVerticalAlign.Center)
                    ) {
                        KickBadgeIcon(b.type, Modifier.fillMaxSize())
                    }
                }
            }
        }
        withStyle(SpanStyle(color = nameColor, fontWeight = FontWeight.Bold)) { append(message.sender.username) }
        append(": ")
        var last = 0
        for (m in EMOTE_REGEX.findAll(content)) {
            if (m.range.first > last) appendWithMentions(content.substring(last, m.range.first), mentionColor)
            val id = m.groupValues[1]
            val name = m.groupValues[2]
            val key = "e$id"
            appendInlineContent(key, name)
            if (!inline.containsKey(key)) {
                inline[key] = InlineTextContent(
                    Placeholder(emoteSize, emoteSize, PlaceholderVerticalAlign.Center)
                ) {
                    val ctx = LocalContext.current
                    val request = remember(id, animate) {
                        ImageRequest.Builder(ctx)
                            .data(emoteUrl(id))
                            .apply {
                                if (animate) {
                                    decoderFactory(
                                        if (Build.VERSION.SDK_INT >= 28) ImageDecoderDecoder.Factory() else GifDecoder.Factory()
                                    )
                                }
                            }
                            .build()
                    }
                    AsyncImage(
                        model = request,
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            last = m.range.last + 1
        }
        if (last < content.length) appendWithMentions(content.substring(last), mentionColor)
    }
    return text to inline
}

private fun AnnotatedString.Builder.appendWithMentions(s: String, color: Color) {
    var i = 0
    for (m in MENTION_REGEX.findAll(s)) {
        if (m.range.first > i) append(s.substring(i, m.range.first))
        withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold)) { append(m.value) }
        i = m.range.last + 1
    }
    if (i < s.length) append(s.substring(i))
}
