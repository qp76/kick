package com.kicklite.domain.usecase

import com.kicklite.domain.common.Resource
import com.kicklite.domain.model.ChatResponse
import com.kicklite.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetChatHistoryUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(channelId: Long): Flow<Resource<ChatResponse>> {
        return repository.getChatHistory(channelId)
    }
}
