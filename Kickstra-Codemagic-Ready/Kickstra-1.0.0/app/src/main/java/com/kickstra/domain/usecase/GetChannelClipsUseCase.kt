package com.kickstra.domain.usecase

import com.kickstra.domain.model.ChannelClip
import com.kickstra.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetChannelClipsUseCase @Inject constructor(
    private val channelRepository: ChannelRepository
){
    operator fun invoke(
        username: String,
        sort: String = "view",
        time: String = "week",
        cursor: String? = null
    ): Flow<Result<Pair<List<ChannelClip>, String?>>> = flow {
        try {
            val result = channelRepository.getChannelClips(username, sort, time, cursor)
            emit(Result.success(result))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
