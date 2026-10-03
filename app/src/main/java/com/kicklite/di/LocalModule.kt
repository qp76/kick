package com.kicklite.di

import android.content.Context
import com.kicklite.data.local.FollowPreferences
import com.kicklite.data.local.LanguagePreferences
import com.kicklite.data.local.ThemePreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocalModule {

    @Provides
    @Singleton
    fun provideFollowPreferences(@ApplicationContext context: Context): FollowPreferences {
        return FollowPreferences(context)
    }

    @Provides
    @Singleton
    fun provideLanguagePreferences(@ApplicationContext context: Context): LanguagePreferences {
        return LanguagePreferences(context)
    }

    @Provides
    @Singleton
    fun provideThemePreferences(@ApplicationContext context: Context): ThemePreferences {
        return ThemePreferences(context)
    }
}
