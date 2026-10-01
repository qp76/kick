package com.kickstra.domain.repository

import com.kickstra.domain.common.Resource
import com.kickstra.domain.model.ChatResponse
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun getChatHistory(channelId: Long): Flow<Resource<ChatResponse>>
}
