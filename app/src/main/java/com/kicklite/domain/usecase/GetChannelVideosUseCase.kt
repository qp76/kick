package com.kicklite.domain.usecase

import com.kicklite.domain.model.ChannelVideo
import com.kicklite.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetChannelVideosUseCase @Inject constructor(
    private val channelRepository: ChannelRepository
) {
    operator fun invoke(username: String): Flow<Result<List<ChannelVideo>>> = flow {
        try {
            emit(Result.success(channelRepository.getChannelVideos(username)))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
