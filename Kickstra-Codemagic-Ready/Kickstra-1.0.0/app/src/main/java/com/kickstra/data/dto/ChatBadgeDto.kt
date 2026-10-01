package com.kickstra.data.dto

import com.google.gson.annotations.SerializedName
import com.kickstra.domain.model.ChatBadge

data class ChatBadgeDto(
    @SerializedName("type")
    val type: String,
    @SerializedName("text")
    val text: String,
    @SerializedName("count")
    val count: Int? = null
) {
    fun toDomainModel(): ChatBadge {
        return ChatBadge(
            type = type,
            text = text,
            count = count
        )
    }
}
