package gio.frag.csvparsely.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import gio.frag.csvparsely.core.domain.di.DefaultDispatcher
import gio.frag.csvparsely.core.domain.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Created by Giorgos Fragkis on 16-11-2026
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

}
