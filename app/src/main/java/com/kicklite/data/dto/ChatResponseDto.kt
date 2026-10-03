package com.kicklite.data.dto

import com.google.gson.annotations.SerializedName
import com.kicklite.domain.model.ChatResponse

data class ChatResponseDto(
    @SerializedName("messages")
    val messages: List<ChatMessageDto>
) {
    fun toDomainModel(): ChatResponse {
        return ChatResponse(
            messages = messages.map { it.toDomainModel() }
        )
    }
}

data class ChatHistoryApiResponse(
    @SerializedName("data")
    val data: ChatResponseDto
)
