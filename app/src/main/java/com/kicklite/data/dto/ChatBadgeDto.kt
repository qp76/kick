package com.kicklite.data.dto

import com.google.gson.annotations.SerializedName
import com.kicklite.domain.model.ChatBadge

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
