package com.kicklite.data.dto

import com.google.gson.annotations.SerializedName
import com.kicklite.domain.model.ChatMessage

data class ChatMessageDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("chat_id")
    val chatId: Long,
    @SerializedName("user_id")
    val userId: Long,
    @SerializedName("content")
    val content: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("metadata")
    val metadata: String?,
    @SerializedName("sender")
    val sender: ChatSenderDto,
    @SerializedName("created_at")
    val createdAt: String
) {
    fun toDomainModel(): ChatMessage {
        return ChatMessage(
            id = id,
            chatId = chatId,
            userId = userId,
            content = content,
            type = type,
            metadata = metadata,
            sender = sender.toDomainModel(),
            createdAt = createdAt
        )
    }
}
