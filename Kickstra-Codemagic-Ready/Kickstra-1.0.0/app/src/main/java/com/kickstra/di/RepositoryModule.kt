package com.kickstra.di

import com.kickstra.data.repository.ChannelRepositoryImpl
import com.kickstra.data.repository.ChatRepositoryImpl
import com.kickstra.data.repository.SearchRepositoryImpl
import com.kickstra.data.repository.StreamRepositoryImpl
import com.kickstra.domain.repository.ChannelRepository
import com.kickstra.domain.repository.ChatRepository
import com.kickstra.domain.repository.SearchRepository
import com.kickstra.domain.repository.StreamRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindStreamRepository(
        streamRepositoryImpl: StreamRepositoryImpl
    ): StreamRepository

    @Binds
    @Singleton
    abstract fun bindChannelRepository(
        channelRepositoryImpl: ChannelRepositoryImpl
    ): ChannelRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(
        searchRepositoryImpl: SearchRepositoryImpl
    ): SearchRepository
}
