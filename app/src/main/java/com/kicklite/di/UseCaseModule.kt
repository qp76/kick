package com.kicklite.di

import com.kicklite.domain.repository.ChannelRepository
import com.kicklite.domain.repository.ChatRepository
import com.kicklite.domain.repository.SearchRepository
import com.kicklite.domain.repository.StreamRepository
import com.kicklite.domain.usecase.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideGetPopularStreamsUseCase(
        repository: StreamRepository
    ): GetPopularStreamsUseCase = GetPopularStreamsUseCase(repository)

    @Provides
    @Singleton
    fun provideGetChannelDetailsUseCase(
        repository: ChannelRepository
    ): GetChannelDetailsUseCase = GetChannelDetailsUseCase(repository)

    @Provides
    @Singleton
    fun provideGetChatHistoryUseCase(
        repository: ChatRepository
    ): GetChatHistoryUseCase = GetChatHistoryUseCase(repository)

    @Provides
    @Singleton
    fun provideFollowChannelUseCase(
        repository: ChannelRepository
    ): FollowChannelUseCase = FollowChannelUseCase(repository)

    @Provides
    @Singleton
    fun provideUnfollowChannelUseCase(
        repository: ChannelRepository
    ): UnfollowChannelUseCase = UnfollowChannelUseCase(repository)

    @Provides
    @Singleton
    fun provideSearchUseCase(
        repository: SearchRepository
    ): SearchUseCase = SearchUseCase(repository)

    @Provides
    @Singleton
    fun provideGetChannelClipsUseCase(
        repository: ChannelRepository
    ): GetChannelClipsUseCase = GetChannelClipsUseCase(repository)
}
