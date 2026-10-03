package com.kicklite.presentation.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kicklite.R
import com.kicklite.domain.model.ChatMessage

private val EMOTE_REGEX = Regex("""\[emote:(\d+):([^\]]*)]""")
private fun emoteUrl(id: String) = "https://files.kick.com/emotes/$id/fullsize"

@Composable
fun ChatSection(
    chatMessages: List<ChatMessage>,
    channelName: String,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null
) {
    val listState = rememberLazyListState()
    val reversed = remember(chatMessages) { chatMessages.asReversed().distinctBy { it.id } }

    // Newest message sits at index 0 (reverse layout). Only follow it if the user is already at the bottom.
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty() && listState.firstVisibleItemIndex <= 2) {
            listState.scrollToItem(0)
        }
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

            // Messages
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (chatMessages.isEmpty()) {
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
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(reversed, key = { it.id }) { message ->
                            ChatMessageItem(message = message, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val nameColor = remember(message.sender.identity.color, primary) {
        runCatching { Color(android.graphics.Color.parseColor(message.sender.identity.color)) }
            .getOrDefault(primary)
    }

    val (text, inline) = remember(message.id) {
        buildChatText(message.sender.username, nameColor, message.content)
    }

    Text(
        text = text,
        inlineContent = inline,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = 22.sp,
        modifier = modifier.padding(vertical = 2.dp)
    )
}

/** Turns "[emote:123:Name]" tokens into inline images; everything else (unicode emoji included) stays text. */
private fun buildChatText(
    username: String,
    nameColor: Color,
    content: String
): Pair<AnnotatedString, Map<String, InlineTextContent>> {
    val inline = HashMap<String, InlineTextContent>()
    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = nameColor, fontWeight = FontWeight.Bold)) { append(username) }
        append(": ")
        var last = 0
        for (m in EMOTE_REGEX.findAll(content)) {
            if (m.range.first > last) append(content.substring(last, m.range.first))
            val id = m.groupValues[1]
            val name = m.groupValues[2]
            val key = "e$id"
            appendInlineContent(key, name)
            if (!inline.containsKey(key)) {
                inline[key] = InlineTextContent(
                    Placeholder(26.sp, 26.sp, PlaceholderVerticalAlign.Center)
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
        if (last < content.length) append(content.substring(last))
    }
    return text to inline
}
