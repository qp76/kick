package com.kicklite.di

import com.kicklite.data.repository.ChannelRepositoryImpl
import com.kicklite.data.repository.ChatRepositoryImpl
import com.kicklite.data.repository.SearchRepositoryImpl
import com.kicklite.data.repository.StreamRepositoryImpl
import com.kicklite.domain.repository.ChannelRepository
import com.kicklite.domain.repository.ChatRepository
import com.kicklite.domain.repository.SearchRepository
import com.kicklite.domain.repository.StreamRepository
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
