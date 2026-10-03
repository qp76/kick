package com.kicklite.domain.model

data class ChatMessage(
    val id: String,
    val chatId: Long,
    val userId: Long,
    val content: String,
    val type: String,
    val metadata: String?,
    val sender: ChatSender,
    val createdAt: String
)
