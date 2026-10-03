package com.kicklite.presentation.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kicklite.domain.model.ChatMessage
import com.kicklite.R
import kotlinx.coroutines.delay

@Composable
fun ChatOverlay(
    chatMessages: List<ChatMessage>,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null
) {
    if (isVisible) {
        val listState = rememberLazyListState()

        LaunchedEffect(chatMessages.size) {
            // Auto-scroll to top (newest messages) in reverse layout
            delay(100)
            if (chatMessages.isNotEmpty()) {
                listState.animateScrollToItem(0)
            }
        }

        Card(
            modifier = modifier
                .width(300.dp)
                .height(400.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Chat header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.live_chat),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (onClose != null) {
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.close),
                                tint = Color.White
                            )
                        }
                    }
                }

                Divider(
                    color = Color.White.copy(alpha = 0.3f),
                    thickness = 1.dp
                )

                // Chat messages
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(chatMessages.reversed()) { message ->
                        ChatMessageItem(message = message)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatToggleButton(
    isVisible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        containerColor = Color.Black.copy(alpha = 0.7f),
        contentColor = Color.White
    ) {
        Text(
            text = if (isVisible) "×" else "💬",
            style = MaterialTheme.typography.titleMedium
        )
    }
}
