package com.kicklite.presentation.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.kicklite.data.local.AdvancedChatPreferences
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private val EMOTE_REGEX = Regex("""\[emote:(\d+):([^\]]*)]""")
private val KNOWN_BOTS = setOf(
    "botrix", "kickbot", "kicklet", "nightbot", "moobot", "streamelements", "streamlabs", "wizebot"
)
private val MENTION_REGEX = Regex("@[A-Za-z0-9_]+")
private val EMOTE_TOKEN = Regex("""\[emote:\d+:([^\]]*)]""")

/** Quoted message for replies: Pair(username, text) or null. */
private fun isPinned(metadata: String?): Boolean = metadata?.lowercase()?.let { "pinned" in it || "pin_message" in it || "pinned_message" in it } == true

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
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    settingsViewModel: ChatSettingsViewModel = hiltViewModel()
) {
    val prefs = settingsViewModel.prefs
    val textSize by prefs.textSize.collectAsStateWithLifecycle()
    val compact by prefs.compact.collectAsStateWithLifecycle()
    val hideCommands by prefs.hideCommands.collectAsStateWithLifecycle()
    val showBadges by prefs.showBadges.collectAsStateWithLifecycle()
    val bigEmotes by prefs.bigEmotes.collectAsStateWithLifecycle()
    val hideBots by prefs.hideBots.collectAsStateWithLifecycle()
    val mutedUsers by settingsViewModel.advancedPrefs.mutedUsers.collectAsStateWithLifecycle()
    val highlightWords by settingsViewModel.advancedPrefs.highlightWords.collectAsStateWithLifecycle()
    val chatWidth by settingsViewModel.advancedPrefs.chatWidth.collectAsStateWithLifecycle()
    val chatSide by settingsViewModel.advancedPrefs.chatSide.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }

    // Newest message first (the list is drawn with reverseLayout, so index 0 is the bottom)
    val visible = remember(chatMessages, hideCommands, hideBots, mutedUsers) {
        chatMessages.asReversed().distinctBy { it.id }.filter {
            (!hideCommands || !it.content.startsWith("!")) &&
                (!hideBots || it.sender.username.lowercase() !in KNOWN_BOTS) &&
                (it.sender.username.lowercase() !in mutedUsers)
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
        ChatSettingsDialog(
            textSize = textSize, compact = compact, hideCommands = hideCommands,
            showBadges = showBadges, bigEmotes = bigEmotes, hideBots = hideBots,
            onHideBots = prefs::setHideBots,
            onTextSize = prefs::setTextSize, onCompact = prefs::setCompact,
            onHideCommands = prefs::setHideCommands, onShowBadges = prefs::setShowBadges,
            onBigEmotes = prefs::setBigEmotes,
            highlightWords = highlightWords, onHighlightWords = settingsViewModel.advancedPrefs::setHighlightWords,
            chatWidth = chatWidth, onChatWidth = settingsViewModel.advancedPrefs::setChatWidth,
            chatSide = chatSide, onChatSide = settingsViewModel.advancedPrefs::setChatSide,
            mutedUsers = mutedUsers, onToggleMute = settingsViewModel.advancedPrefs::toggleMute,
            onDismiss = { showSettings = false }
        )
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
                                bigEmotes = bigEmotes,
                                highlightWords = highlightWords,
                                onLongPressUser = { settingsViewModel.advancedPrefs.toggleMute(it) },
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
private fun ChatSettingsDialog(
    textSize: Float,
    compact: Boolean,
    hideCommands: Boolean,
    showBadges: Boolean,
    bigEmotes: Boolean,
    hideBots: Boolean,
    highlightWords: String,
    onHighlightWords: (String) -> Unit,
    chatWidth: Int,
    onChatWidth: (Int) -> Unit,
    chatSide: String,
    onChatSide: (String) -> Unit,
    mutedUsers: Set<String>,
    onToggleMute: (String) -> Unit,
    onHideBots: (Boolean) -> Unit,
    onTextSize: (Float) -> Unit,
    onCompact: (Boolean) -> Unit,
    onHideCommands: (Boolean) -> Unit,
    onShowBadges: (Boolean) -> Unit,
    onBigEmotes: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chat settings") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        text = {
            Column {
                Text("Text size: ${textSize.toInt()}", style = MaterialTheme.typography.labelLarge)
                Slider(value = textSize, onValueChange = { onTextSize(it.toInt().toFloat()) }, valueRange = 11f..24f)
                SettingSwitch("Compact messages", compact, onCompact)
                SettingSwitch("Show badges", showBadges, onShowBadges)
                SettingSwitch("Large emotes", bigEmotes, onBigEmotes)
                SettingSwitch("Hide ! commands", hideCommands, onHideCommands)
                SettingSwitch("Hide bot messages", hideBots, onHideBots)
                Spacer(Modifier.height(8.dp))
                Text("Highlight words (comma separated)", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(value = highlightWords, onValueChange = onHighlightWords, singleLine = true, placeholder = { Text("username, giveaway") })
                Spacer(Modifier.height(8.dp))
                Text("Fullscreen chat width: ${chatWidth}dp", style = MaterialTheme.typography.labelLarge)
                Slider(value = chatWidth.toFloat(), onValueChange = { onChatWidth(it.toInt()) }, valueRange = 240f..520f)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Chat side", modifier = Modifier.weight(1f))
                    FilterChip(selected = chatSide == "left", onClick = { onChatSide("left") }, label = { Text("Left") })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = chatSide == "right", onClick = { onChatSide("right") }, label = { Text("Right") })
                }
                if (mutedUsers.isNotEmpty()) {
                    Text("Muted: ${mutedUsers.joinToString()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("Long-press a message to mute or unmute that user.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

private fun badgeSymbol(type: String): String? = when (type) {
    "broadcaster" -> "\uD83C\uDFA5"
    "moderator" -> "\uD83D\uDEE1\uFE0F"
    "vip" -> "\uD83D\uDC8E"
    "og" -> "\uD83D\uDC51"
    "founder" -> "\uD83C\uDFC6"
    "verified" -> "\u2714\uFE0F"
    "subscriber", "sub_gifter" -> "\u2B50"
    else -> null
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    modifier: Modifier = Modifier,
    textSize: Float = 15f,
    showBadges: Boolean = true,
    bigEmotes: Boolean = false,
    highlightWords: String = "",
    onLongPressUser: (String) -> Unit = {}
) {
    val primary = MaterialTheme.colorScheme.primary
    val nameColor = remember(message.sender.identity.color, primary) {
        runCatching { Color(android.graphics.Color.parseColor(message.sender.identity.color)) }
            .getOrDefault(primary)
    }

    val (text, inline) = remember(message.id, textSize, showBadges, bigEmotes, nameColor, primary, highlightWords) {
        buildChatText(message, nameColor, primary, showBadges, if (bigEmotes) (textSize * 2.4f).sp else (textSize * 1.7f).sp, highlightWords, primary)
    }
    val reply = remember(message.id) { parseReply(message.metadata) }
    val pinned = remember(message.id, message.metadata) { isPinned(message.metadata) }

    if (reply != null) {
        Column(modifier = modifier) {
            Text(
                text = "\u21A9 ${reply.first}: ${reply.second}",
                fontSize = (textSize * 0.8f).sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
            )
            if (pinned) {
                Text("📌 Pinned message", fontSize = (textSize * 0.72f).sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Text(
                text = text,
                inlineContent = inline,
                fontSize = textSize.sp,
                lineHeight = (textSize * 1.45f).sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.combinedClickable(onClick = {}, onLongClick = { onLongPressUser(message.sender.username) }).padding(bottom = 2.dp)
            )
        }
        return
    }

    if (pinned) {
        Column(modifier = modifier) {
            Text("📌 Pinned message", fontSize = (textSize * 0.72f).sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(
                text = text,
                inlineContent = inline,
                fontSize = textSize.sp,
                lineHeight = (textSize * 1.45f).sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.combinedClickable(onClick = {}, onLongClick = { onLongPressUser(message.sender.username) }).padding(vertical = 2.dp)
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
        modifier = modifier.combinedClickable(onClick = {}, onLongClick = { onLongPressUser(message.sender.username) }).padding(vertical = 2.dp)
    )
}

/** Turns "[emote:123:Name]" tokens into inline images; everything else (unicode emoji included) stays text. */
private fun buildChatText(
    message: ChatMessage,
    nameColor: Color,
    mentionColor: Color,
    showBadges: Boolean,
    emoteSize: TextUnit,
    highlightWords: String,
    highlightColor: Color
): Pair<AnnotatedString, Map<String, InlineTextContent>> {
    val inline = HashMap<String, InlineTextContent>()
    val content = message.content
    val text = buildAnnotatedString {
        if (showBadges) {
            for (b in message.sender.identity.badges) {
                badgeSymbol(b.type)?.let { append(it); append(" ") }
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
                    AsyncImage(
                        model = emoteUrl(id),
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            last = m.range.last + 1
        }
        if (last < content.length) appendWithMentions(content.substring(last), mentionColor)
    }
    val words = highlightWords.split(",").map { it.trim() }.filter { it.length > 1 }
    if (words.isNotEmpty()) {
        val raw = text.text
        val rebuilt = buildAnnotatedString {
            var cursor = 0
            val regex = Regex(words.joinToString("|") { Regex.escape(it) }, RegexOption.IGNORE_CASE)
            for (m in regex.findAll(raw)) {
                if (m.range.first > cursor) append(raw.substring(cursor, m.range.first))
                withStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.ExtraBold)) { append(m.value) }
                cursor = m.range.last + 1
            }
            if (cursor < raw.length) append(raw.substring(cursor))
        }
        return rebuilt to inline
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
