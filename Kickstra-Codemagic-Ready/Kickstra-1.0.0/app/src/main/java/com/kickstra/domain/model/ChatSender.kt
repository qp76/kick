package com.kickstra.domain.model

data class ChatSender(
    val id: Long,
    val slug: String,
    val username: String,
    val identity: ChatIdentity
)
