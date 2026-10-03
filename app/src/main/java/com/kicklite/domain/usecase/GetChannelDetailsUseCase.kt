package com.kicklite.domain.usecase

import com.kicklite.domain.common.Resource
import com.kicklite.domain.model.ChannelDetails
import com.kicklite.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetChannelDetailsUseCase @Inject constructor(
    private val repository: ChannelRepository
) {
    suspend operator fun invoke(username: String): Flow<Resource<ChannelDetails>> {
        return repository.getChannelDetails(username)
    }
}
