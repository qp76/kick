package com.kickstra.domain.usecase

import com.kickstra.domain.common.Resource
import com.kickstra.domain.model.ChatResponse
import com.kickstra.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetChatHistoryUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(channelId: Long): Flow<Resource<ChatResponse>> {
        return repository.getChatHistory(channelId)
    }
}
