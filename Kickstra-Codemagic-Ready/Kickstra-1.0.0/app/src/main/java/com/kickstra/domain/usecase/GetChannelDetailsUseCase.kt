package com.kickstra.domain.usecase

import com.kickstra.domain.common.Resource
import com.kickstra.domain.model.ChannelDetails
import com.kickstra.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetChannelDetailsUseCase @Inject constructor(
    private val repository: ChannelRepository
) {
    suspend operator fun invoke(username: String): Flow<Resource<ChannelDetails>> {
        return repository.getChannelDetails(username)
    }
}
