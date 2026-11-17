package gio.frag.csvparsely.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Created by Giorgos Fragkis on 16-11-2026
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15.seconds)
        .readTimeout(30.seconds)
        // Limits the whole download, so a server that sends data very slowly can't keep it loading forever.
        .callTimeout(2.minutes)
        .build()
}
