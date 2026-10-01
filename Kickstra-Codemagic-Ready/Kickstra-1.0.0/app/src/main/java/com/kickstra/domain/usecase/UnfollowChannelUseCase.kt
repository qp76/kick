package com.kickstra.domain.usecase

import com.kickstra.domain.common.Resource
import com.kickstra.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UnfollowChannelUseCase @Inject constructor(
    private val repository: ChannelRepository
) {
    suspend operator fun invoke(channelId: Long): Flow<Resource<Boolean>> {
        return repository.unfollowChannel(channelId)
    }
}
