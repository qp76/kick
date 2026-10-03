package com.kicklite.domain.repository

import com.kicklite.domain.common.Resource
import com.kicklite.domain.model.ChatResponse
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun getChatHistory(channelId: Long): Flow<Resource<ChatResponse>>
}
