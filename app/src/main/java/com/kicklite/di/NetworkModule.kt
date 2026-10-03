package com.kicklite.di

import com.kicklite.data.remote.KickApi
import com.kicklite.data.remote.KickChannelApi
import com.kicklite.data.remote.KickSearchApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Mobile Safari/537.36"

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class WebApi

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class ChannelApi

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class SearchApi

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        }
    }

    @Provides
    @Singleton
    fun provideCookieJar(): CookieJar {
        return com.kicklite.data.remote.InMemoryCookieJar()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        cookieJar: CookieJar
    ): OkHttpClient {
        val defaultHeadersInterceptor = okhttp3.Interceptor { chain ->
            val request = chain.request()
            val builder = request.newBuilder()

            if (request.header("User-Agent") == null) {
                builder.header("User-Agent", DEFAULT_USER_AGENT)
            }

            val host = request.url.host
            if (host.endsWith("kick.com")) {
                if (request.header("Origin") == null) {
                    builder.header("Origin", "https://kick.com")
                }
                if (request.header("Referer") == null) {
                    builder.header("Referer", "https://kick.com/")
                }
            }

            chain.proceed(builder.build())
        }

        return OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(defaultHeadersInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @WebApi
    fun provideWebRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(KickApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @ChannelApi
    fun provideChannelRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(KickChannelApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @SearchApi
    fun provideSearchRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(KickSearchApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideKickApi(@WebApi retrofit: Retrofit): KickApi {
        return retrofit.create(KickApi::class.java)
    }

    @Provides
    @Singleton
    fun provideKickChannelApi(@ChannelApi retrofit: Retrofit): KickChannelApi {
        return retrofit.create(KickChannelApi::class.java)
    }

    @Provides
    @Singleton
    fun provideKickSearchApi(@SearchApi retrofit: Retrofit): KickSearchApi {
        return retrofit.create(KickSearchApi::class.java)
    }
}
