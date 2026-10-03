package com.kicklite.domain.usecase

import com.kicklite.domain.common.Resource
import com.kicklite.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UnfollowChannelUseCase @Inject constructor(
    private val repository: ChannelRepository
) {
    suspend operator fun invoke(channelId: Long): Flow<Resource<Boolean>> {
        return repository.unfollowChannel(channelId)
    }
}
